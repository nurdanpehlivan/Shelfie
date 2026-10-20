package com.shelfie.app.data.sync

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.shelfie.app.di.AccountPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * This install's id and the signed-in session. Kept in its own file, which is excluded from
 * backups (res/xml/data_extraction_rules.xml): a restored phone must neither reuse another
 * device's install id nor arrive signed in with a token it never received.
 */
@Singleton
class AccountStore @Inject constructor(
    @AccountPreferences private val dataStore: DataStore<Preferences>,
) {

    data class Session(val token: String, val email: String, val cursor: Long, val lastSyncAt: Long?)

    private val prefs: Flow<Preferences> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }

    val session: Flow<Session?> = prefs
        .map { p ->
            val token = p[TOKEN] ?: return@map null
            Session(token = token, email = p[EMAIL].orEmpty(), cursor = p[CURSOR] ?: 0L, lastSyncAt = p[LAST_SYNC_AT])
        }
        .distinctUntilChanged()

    suspend fun currentSession(): Session? = session.first()

    @Volatile
    private var cachedInstallId: String? = null

    /** Random, created on first use; identifies this install in the server's usage statistics. */
    suspend fun installId(): String {
        cachedInstallId?.let { return it }
        var id: String? = null
        dataStore.edit { p -> id = p[INSTALL_ID] ?: UUID.randomUUID().toString().also { p[INSTALL_ID] = it } }
        return checkNotNull(id).also { cachedInstallId = it }
    }

    suspend fun signIn(token: String, email: String) {
        dataStore.edit { p ->
            p[TOKEN] = token
            p[EMAIL] = email
            p[CURSOR] = 0L
            p.remove(LAST_SYNC_AT)
        }
    }

    suspend fun signOut() {
        dataStore.edit { p ->
            p.remove(TOKEN)
            p.remove(EMAIL)
            p.remove(CURSOR)
            p.remove(LAST_SYNC_AT)
        }
    }

    /** Ignored if the session ended (or changed) while the sync was running. */
    suspend fun saveProgress(token: String, cursor: Long, syncedAt: Long?) {
        dataStore.edit { p ->
            if (p[TOKEN] != token) return@edit
            p[CURSOR] = cursor
            if (syncedAt != null) p[LAST_SYNC_AT] = syncedAt
        }
    }

    private companion object {
        val INSTALL_ID = stringPreferencesKey("install_id")
        val TOKEN = stringPreferencesKey("session_token")
        val EMAIL = stringPreferencesKey("email")
        val CURSOR = longPreferencesKey("sync_cursor")
        val LAST_SYNC_AT = longPreferencesKey("last_sync_at")
    }
}
