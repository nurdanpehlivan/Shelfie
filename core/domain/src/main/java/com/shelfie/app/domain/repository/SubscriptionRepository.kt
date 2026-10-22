package com.shelfie.app.domain.repository

import com.shelfie.app.domain.model.PremiumOffer
import com.shelfie.app.domain.model.PurchaseEvent
import com.shelfie.app.domain.model.PurchaseHost
import com.shelfie.app.domain.model.QuotaStatus
import kotlinx.coroutines.flow.Flow

/**
 * Shelfie Premium: the store's offers, buying and restoring, and the daily AI allowance the
 * plan gives. Purchases are confirmed by the server before they count.
 */
interface SubscriptionRepository {

    /** Outcomes of purchases started with [purchase] (or finished later by the store). */
    val events: Flow<PurchaseEvent>

    /** Available offers, shortest period first; empty if the store or the product isn't available. */
    suspend fun offers(): List<PremiumOffer>

    fun purchase(host: PurchaseHost, offer: PremiumOffer)

    /** Re-sends active subscriptions to the server; `true` when one grants Premium. */
    suspend fun restore(): Boolean

    /** The plan and today's usage; works signed out too (counted per install). */
    suspend fun quota(): Result<QuotaStatus>

    /** Where the user manages or cancels the subscription. */
    fun manageUrl(): String
}
