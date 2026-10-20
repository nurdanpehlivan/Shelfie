package com.shelfie.app.domain.repository

import com.shelfie.app.domain.model.Household
import com.shelfie.app.domain.model.HouseholdInvite
import com.shelfie.app.domain.model.QuotaStatus

/**
 * Family sharing and the plan, both served by the Shelfie Worker. Household actions need an
 * account; failures carry a [com.shelfie.app.domain.model.HouseholdException]. Joining or leaving
 * syncs right away, which merges this phone's data into the new household (or back out of it).
 */
interface HouseholdRepository {

    suspend fun household(): Result<Household>

    /** A fresh invite code for the owner's household; it replaces the previous one. */
    suspend fun createInvite(): Result<HouseholdInvite>

    suspend fun join(code: String): Result<Household>

    /** The member keeps a copy of the shared data. */
    suspend fun leave(): Result<Household>

    /** Owner only. */
    suspend fun removeMember(email: String): Result<Household>

    /** Works signed out too (counted per install). */
    suspend fun quota(): Result<QuotaStatus>

    /** Sends a Play purchase to the server for checking; `true` when it grants Premium. */
    suspend fun verifyPurchase(productId: String, purchaseToken: String): Result<Boolean>
}
