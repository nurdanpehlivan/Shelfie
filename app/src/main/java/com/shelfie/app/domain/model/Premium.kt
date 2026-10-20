package com.shelfie.app.domain.model

import java.time.Instant

/** The plan and today's AI usage, as counted by the server. */
data class QuotaStatus(
    val isPremium: Boolean,
    val premiumUntil: Instant?,
    val resetsAt: Instant,
    val scansUsed: Int,
    val scanLimit: Int,
    val recipesUsed: Int,
    val recipeLimit: Int,
)

/** One way to pay for Premium, e.g. the monthly or the yearly base plan. */
data class PremiumOffer(
    val basePlanId: String,
    /** The store's localized price of the recurring phase, e.g. "₺49,99". */
    val formattedPrice: String,
    /** ISO 8601 period of that phase: "P1M", "P1Y". */
    val billingPeriod: String,
    /** A free trial or intro price comes first. */
    val hasIntroOffer: Boolean,
    /** Opaque store token that selects this offer when buying. */
    val offerToken: String,
) {
    val isYearly: Boolean get() = billingPeriod.endsWith("Y")
}

sealed interface PurchaseEvent {
    data object Granted : PurchaseEvent

    /** Paid by a slow method (cash, bank); Premium starts once the store confirms it. */
    data object Pending : PurchaseEvent

    data object Cancelled : PurchaseEvent

    /** The store said no, or the server couldn't confirm the purchase. */
    data object Failed : PurchaseEvent
}

/**
 * The screen a store's purchase dialog is shown over. The domain doesn't know Android; on
 * Android [platformHost] is the current Activity, which the billing implementation unwraps.
 */
class PurchaseHost(val platformHost: Any)
