package com.shelfie.app.data.repository

import com.shelfie.app.data.remote.shelfie.CredentialsRequest
import com.shelfie.app.data.remote.shelfie.ForgotPasswordRequest
import com.shelfie.app.data.remote.shelfie.SessionResponse
import com.shelfie.app.data.remote.shelfie.ShelfieApiService
import com.shelfie.app.data.sync.AccountStore
import com.shelfie.app.data.sync.SyncManager
import com.shelfie.app.data.sync.SyncScheduler
import com.shelfie.app.domain.model.Account
import com.shelfie.app.domain.model.AccountException
import com.shelfie.app.domain.repository.AccountRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import java.io.IOException
import java.time.Instant
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepositoryImpl @Inject constructor(
    private val api: ShelfieApiService,
    private val accountStore: AccountStore,
    private val syncManager: SyncManager,
    private val syncScheduler: SyncScheduler,
) : AccountRepository {

    override val account: Flow<Account?> = accountStore.session.map { session ->
        session?.let { Account(email = it.email, lastSyncAt = it.lastSyncAt?.let(Instant::ofEpochMilli)) }
    }

    override suspend fun register(email: String, password: String): Result<Unit> =
        authenticate(email, password) { api.register(it) }

    override suspend fun signIn(email: String, password: String): Result<Unit> =
        authenticate(email, password) { api.login(it) }

    private suspend fun authenticate(
        email: String,
        password: String,
        call: suspend (CredentialsRequest) -> SessionResponse,
    ): Result<Unit> = try {
        val response = call(
            CredentialsRequest(email = normalizeEmail(email), password = password, installId = accountStore.installId()),
        )
        accountStore.signIn(token = response.token, email = response.user.email)
        syncManager.onSignedIn()
        syncScheduler.onSignedIn()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e.toAccountException())
    }

    override suspend fun requestPasswordReset(email: String): Result<Unit> = try {
        api.forgotPassword(ForgotPasswordRequest(email = normalizeEmail(email), lang = Locale.getDefault().language))
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e.toAccountException())
    }

    override suspend fun signOut() {
        val session = accountStore.currentSession()
        syncManager.signOutLocally()
        syncScheduler.onSignedOut()
        // Best effort: an unreachable server only means the token expires on its own.
        if (session != null) {
            try {
                api.logout(ShelfieApiService.bearer(session.token))
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            }
        }
    }

    override suspend fun deleteAccount(): Result<Unit> {
        val session = accountStore.currentSession() ?: return Result.success(Unit)
        return try {
            api.deleteAccount(ShelfieApiService.bearer(session.token))
            syncManager.signOutLocally()
            syncScheduler.onSignedOut()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e.toAccountException())
        }
    }

    override suspend fun syncNow(): Result<Unit> = syncManager.sync()

    private fun Exception.toAccountException(): AccountException = when (this) {
        is HttpException -> when (code()) {
            400 -> AccountException.InvalidInput(this)
            401 -> AccountException.InvalidCredentials(this)
            409 -> AccountException.EmailTaken(this)
            429 -> AccountException.TooManyAttempts(this)
            503 -> AccountException.Unavailable(this)
            else -> AccountException.Server(this)
        }
        is IOException -> AccountException.Network(this)
        else -> AccountException.Server(this)
    }

    companion object {
        fun normalizeEmail(email: String): String = email.trim().lowercase(Locale.ROOT)
    }
}
