package com.shelfie.app.data.install

import android.content.Context
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.shelfie.app.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.net.URLDecoder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * The family invite code an install came with. A shared invite links to the Play Store with
 * `&referrer=joinCode%3D<code>`; Play hands that referrer to the app after installing it, so
 * the code is filled in on first launch without any typing.
 */
@Singleton
class InstallReferrerSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
) {

    /** The invite code, the first time it is asked after an install; `null` otherwise. */
    suspend fun consumeInviteCode(): String? {
        if (settingsRepository.isMaintenanceDone(DONE_KEY)) return null
        val result = readReferrer() ?: return null // Play busy or unreachable: try next launch.
        settingsRepository.markMaintenanceDone(DONE_KEY)
        return result.referrer?.let(::inviteCodeFrom)
    }

    private class Result(val referrer: String?)

    private suspend fun readReferrer(): Result? = suspendCancellableCoroutine { continuation ->
        val client = InstallReferrerClient.newBuilder(context).build()
        continuation.invokeOnCancellation { runCatching { client.endConnection() } }
        client.startConnection(
            object : InstallReferrerStateListener {
                override fun onInstallReferrerSetupFinished(responseCode: Int) {
                    val result = when (responseCode) {
                        InstallReferrerClient.InstallReferrerResponse.OK ->
                            Result(runCatching { client.installReferrer.installReferrer }.getOrNull())
                        // No Play Store on this device (or too old): there will never be one.
                        InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED -> Result(null)
                        else -> null
                    }
                    runCatching { client.endConnection() }
                    if (continuation.isActive) continuation.resume(result)
                }

                override fun onInstallReferrerServiceDisconnected() {
                    if (continuation.isActive) continuation.resume(null)
                }
            },
        )
    }

    companion object {
        private const val DONE_KEY = "install_referrer_read"
        private const val JOIN_CODE_PARAM = "joinCode"

        /** Invite codes as the backend makes them: 8 characters, no 0/O or 1/I/L. */
        private val INVITE_CODE = Regex("^[A-HJ-KM-NP-Z2-9]{8}$")

        /** "joinCode=ABCD2345" (and anything else Play adds) to "ABCD2345". */
        internal fun inviteCodeFrom(referrer: String): String? =
            referrer.split('&')
                .map { it.split('=', limit = 2) }
                .firstOrNull { it.size == 2 && URLDecoder.decode(it[0], Charsets.UTF_8.name()) == JOIN_CODE_PARAM }
                ?.let { URLDecoder.decode(it[1], Charsets.UTF_8.name()).trim().uppercase() }
                ?.takeIf { INVITE_CODE.matches(it) }
    }
}
