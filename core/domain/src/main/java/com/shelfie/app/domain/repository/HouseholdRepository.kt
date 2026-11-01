package com.shelfie.app.domain.repository

import com.shelfie.app.domain.model.Household
import com.shelfie.app.domain.model.HouseholdActivity
import com.shelfie.app.domain.model.HouseholdInvite
import kotlinx.coroutines.flow.Flow

/**
 * Family sharing, served by the Shelfie Worker. Household actions need an
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

    /** The newest events of the household, newest first; also refreshes [unseenActivityCount]. */
    suspend fun activity(): Result<List<HouseholdActivity>>

    /** Events by other members since the user last opened the feed (as of the last fetch). */
    val unseenActivityCount: Flow<Int>

    /** The user has seen the feed up to its newest event. */
    suspend fun markActivitySeen()

    /**
     * Other members' events not notified about yet, oldest first, and marks them notified. The
     * first check after signing in only sets the mark, so old history never pops up. When there
     * is news, this phone's data is synced too, so what the notification says is on screen.
     */
    suspend fun takeActivityToNotify(): List<HouseholdActivity>
}
