package com.shelfie.app.domain.repository

import com.shelfie.app.domain.model.Account
import kotlinx.coroutines.flow.Flow

/**
 * Optional account that syncs the pantry, shopping list and recipe history across devices and
 * with the web panel. The app works fully without one.
 */
interface AccountRepository {

    /** The signed-in account, or `null`. */
    val account: Flow<Account?>

    /** Creates an account and signs in; on-device data is uploaded to it. */
    suspend fun register(email: String, password: String): Result<Unit>

    /** Signs in; on-device data is merged with what the account already has. */
    suspend fun signIn(email: String, password: String): Result<Unit>

    /**
     * Emails a link to choose a new password. Succeeds whether or not the account exists, so the
     * answer doesn't reveal who has one; fails with [com.shelfie.app.domain.model.AccountException.Unavailable]
     * while the server can't send mail.
     */
    suspend fun requestPasswordReset(email: String): Result<Unit>

    /** Tells the server where to push household news; does nothing while signed out. */
    suspend fun registerPushToken(token: String)

    /** Signs out; on-device data stays. */
    suspend fun signOut()

    /** Deletes the account and its server copy; on-device data stays. */
    suspend fun deleteAccount(): Result<Unit>

    /** Syncs right now; a failure carries a [com.shelfie.app.domain.model.SyncException]. */
    suspend fun syncNow(): Result<Unit>
}
