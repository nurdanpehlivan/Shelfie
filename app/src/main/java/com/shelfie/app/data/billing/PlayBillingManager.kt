package com.shelfie.app.data.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.shelfie.app.domain.repository.HouseholdRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/** One way to pay for Premium, e.g. the monthly or the yearly base plan. */
data class PremiumOffer(
    val basePlanId: String,
    /** Play's localized price of the recurring phase, e.g. "₺49,99". */
    val formattedPrice: String,
    /** ISO 8601 period of that phase: "P1M", "P1Y". */
    val billingPeriod: String,
    /** A free trial or intro price comes first. */
    val hasIntroOffer: Boolean,
    internal val offerToken: String,
    internal val details: ProductDetails,
)

sealed interface PurchaseEvent {
    data object Granted : PurchaseEvent

    /** Paid by a slow method (cash, bank); Premium starts once Play confirms it. */
    data object Pending : PurchaseEvent

    data object Cancelled : PurchaseEvent

    /** Play said no, or the server couldn't confirm the purchase. */
    data object Failed : PurchaseEvent
}

/**
 * Google Play Billing for the Premium subscription. Play takes the payment; the backend checks
 * the purchase with Google (backend/src/billing.ts) and only then is it acknowledged here, so an
 * unconfirmed purchase is refunded by Play after three days instead of granting nothing.
 */
@Singleton
class PlayBillingManager @Inject constructor(
    @ApplicationContext context: Context,
    private val householdRepository: HouseholdRepository,
) : PurchasesUpdatedListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val client: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    private val _events = MutableSharedFlow<PurchaseEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<PurchaseEvent> = _events.asSharedFlow()

    /** Available offers, cheapest period first; empty if Play or the product isn't available. */
    suspend fun loadOffers(): List<PremiumOffer> {
        if (!connect()) return emptyList()
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PREMIUM_PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build(),
                ),
            )
            .build()
        val details = suspendCancellableCoroutine { continuation ->
            client.queryProductDetailsAsync(params) { result, queryResult ->
                continuation.resume(if (result.isOk) queryResult.productDetailsList else emptyList())
            }
        }
        return details.flatMap { product ->
            product.subscriptionOfferDetails.orEmpty().mapNotNull { offer ->
                val phases = offer.pricingPhases.pricingPhaseList
                val recurring = phases.lastOrNull() ?: return@mapNotNull null
                PremiumOffer(
                    basePlanId = offer.basePlanId,
                    formattedPrice = recurring.formattedPrice,
                    billingPeriod = recurring.billingPeriod,
                    hasIntroOffer = phases.size > 1,
                    offerToken = offer.offerToken,
                    details = product,
                )
            }
        }
            // One entry per base plan: the intro offer when there is one, it's the better deal.
            .groupBy { it.basePlanId }
            .map { (_, offers) -> offers.firstOrNull { it.hasIntroOffer } ?: offers.first() }
            .sortedBy { it.billingPeriod.periodDays() }
    }

    fun launch(activity: Activity, offer: PremiumOffer) {
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(offer.details)
                        .setOfferToken(offer.offerToken)
                        .build(),
                ),
            )
            .build()
        val result = client.launchBillingFlow(activity, params)
        if (!result.isOk) _events.tryEmit(PurchaseEvent.Failed)
    }

    /** Re-sends active subscriptions to the server, e.g. after reinstalling or signing in again. */
    suspend fun restore(): Boolean {
        if (!connect()) return false
        val purchases = suspendCancellableCoroutine { continuation ->
            client.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build(),
            ) { result, list -> continuation.resume(if (result.isOk) list else emptyList()) }
        }
        var granted = false
        for (purchase in purchases) if (handle(purchase) == PurchaseEvent.Granted) granted = true
        return granted
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> scope.launch {
                purchases.orEmpty().forEach { _events.emit(handle(it)) }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> _events.tryEmit(PurchaseEvent.Cancelled)
            else -> _events.tryEmit(PurchaseEvent.Failed)
        }
    }

    private suspend fun handle(purchase: Purchase): PurchaseEvent {
        if (purchase.purchaseState == Purchase.PurchaseState.PENDING) return PurchaseEvent.Pending
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return PurchaseEvent.Failed
        val productId = purchase.products.firstOrNull() ?: PREMIUM_PRODUCT_ID
        val entitled = householdRepository.verifyPurchase(productId, purchase.purchaseToken).getOrDefault(false)
        if (!entitled) return PurchaseEvent.Failed
        if (!purchase.isAcknowledged) acknowledge(purchase.purchaseToken)
        return PurchaseEvent.Granted
    }

    private suspend fun acknowledge(token: String) = suspendCancellableCoroutine { continuation ->
        client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(token).build()) {
            continuation.resume(Unit)
        }
    }

    private suspend fun connect(): Boolean {
        if (client.isReady) return true
        return suspendCancellableCoroutine { continuation ->
            client.startConnection(
                object : BillingClientStateListener {
                    override fun onBillingSetupFinished(result: BillingResult) {
                        if (continuation.isActive) continuation.resume(result.isOk)
                    }

                    override fun onBillingServiceDisconnected() {
                        if (continuation.isActive) continuation.resume(false)
                    }
                },
            )
        }
    }

    private val BillingResult.isOk: Boolean get() = responseCode == BillingClient.BillingResponseCode.OK

    /** Rough length of an ISO period, only for sorting monthly before yearly. */
    private fun String.periodDays(): Int {
        val match = Regex("""P(\d+)([DWMY])""").find(this) ?: return Int.MAX_VALUE
        val n = match.groupValues[1].toInt()
        return n * when (match.groupValues[2]) {
            "D" -> 1
            "W" -> 7
            "M" -> 30
            else -> 365
        }
    }

    companion object {
        /** Play Console subscription id; keep in sync with PREMIUM_PRODUCT_IDS on the server. */
        const val PREMIUM_PRODUCT_ID = "shelfie_premium"
    }
}
