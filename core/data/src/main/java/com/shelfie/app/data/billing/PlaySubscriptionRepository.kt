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
import com.shelfie.app.data.remote.gemini.GeminiConfig
import com.shelfie.app.data.remote.shelfie.ShelfieApiService
import com.shelfie.app.data.remote.shelfie.VerifyPurchaseRequest
import com.shelfie.app.data.sync.AccountStore
import com.shelfie.app.domain.model.PremiumOffer
import com.shelfie.app.domain.model.PurchaseEvent
import com.shelfie.app.domain.model.PurchaseHost
import com.shelfie.app.domain.model.QuotaStatus
import com.shelfie.app.domain.repository.SubscriptionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Premium on Google Play. Play takes the payment; the backend checks the purchase with Google
 * (backend/src/billing.ts) and only then is it acknowledged here, so an unconfirmed purchase is
 * refunded by Play after three days instead of granting nothing.
 */
@Singleton
class PlaySubscriptionRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val api: ShelfieApiService,
    private val accountStore: AccountStore,
    private val config: GeminiConfig,
) : SubscriptionRepository, PurchasesUpdatedListener {

    private val packageName: String = context.packageName

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val client: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    /** Play's product for each offer token, needed to launch the purchase. */
    private val productsByOfferToken = ConcurrentHashMap<String, ProductDetails>()

    private val _events = MutableSharedFlow<PurchaseEvent>(extraBufferCapacity = 4)
    override val events: Flow<PurchaseEvent> = _events.asSharedFlow()

    override suspend fun offers(): List<PremiumOffer> {
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
                productsByOfferToken[offer.offerToken] = product
                PremiumOffer(
                    basePlanId = offer.basePlanId,
                    formattedPrice = recurring.formattedPrice,
                    billingPeriod = recurring.billingPeriod,
                    hasIntroOffer = phases.size > 1,
                    offerToken = offer.offerToken,
                )
            }
        }
            // One entry per base plan: the intro offer when there is one, it's the better deal.
            .groupBy { it.basePlanId }
            .map { (_, offers) -> offers.firstOrNull { it.hasIntroOffer } ?: offers.first() }
            .sortedBy { it.billingPeriod.periodDays() }
    }

    override fun purchase(host: PurchaseHost, offer: PremiumOffer) {
        val activity = host.platformHost as? Activity
        val product = productsByOfferToken[offer.offerToken]
        if (activity == null || product == null) {
            _events.tryEmit(PurchaseEvent.Failed)
            return
        }
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(product)
                        .setOfferToken(offer.offerToken)
                        .build(),
                ),
            )
            .build()
        if (!client.launchBillingFlow(activity, params).isOk) _events.tryEmit(PurchaseEvent.Failed)
    }

    override suspend fun restore(): Boolean {
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

    override suspend fun quota(): Result<QuotaStatus> = try {
        val session = accountStore.currentSession()
        val dto = api.quota(
            proxyToken = config.proxyToken,
            installId = accountStore.installId(),
            bearer = session?.let { ShelfieApiService.bearer(it.token) },
        )
        Result.success(
            QuotaStatus(
                isPremium = dto.plan == "premium",
                premiumUntil = dto.premiumUntil?.let(Instant::ofEpochMilli),
                resetsAt = Instant.ofEpochMilli(dto.resetsAt),
                scansUsed = dto.scan.used,
                scanLimit = dto.scan.limit,
                recipesUsed = dto.recipe.used,
                recipeLimit = dto.recipe.limit,
            ),
        )
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    override fun manageUrl(): String =
        "https://play.google.com/store/account/subscriptions?sku=$PREMIUM_PRODUCT_ID&package=$packageName"

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
        if (!verify(purchase.products.firstOrNull() ?: PREMIUM_PRODUCT_ID, purchase.purchaseToken)) return PurchaseEvent.Failed
        if (!purchase.isAcknowledged) acknowledge(purchase.purchaseToken)
        return PurchaseEvent.Granted
    }

    /** Premium needs an account; the server checks the token with Google. */
    private suspend fun verify(productId: String, purchaseToken: String): Boolean {
        val session = accountStore.currentSession() ?: return false
        return try {
            api.verifyPurchase(ShelfieApiService.bearer(session.token), VerifyPurchaseRequest(productId, purchaseToken)).entitled
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            false
        }
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

    private companion object {
        /** Play Console subscription id; keep in sync with PREMIUM_PRODUCT_IDS on the server. */
        const val PREMIUM_PRODUCT_ID = "shelfie_premium"
    }
}
