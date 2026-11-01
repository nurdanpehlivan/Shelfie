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

    /** [space] is the server's data space last synced with (own account or household), if known. */
    data class Session(val token: String, val email: String, val cursor: Long, val lastSyncAt: Long?, val space: String?)

    private val prefs: Flow<Preferences> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }

    val session: Flow<Session?> = prefs
        .map { p ->
            val token = p[TOKEN] ?: return@map null
            Session(
                token = token,
                email = p[EMAIL].orEmpty(),
                cursor = p[CURSOR] ?: 0L,
                lastSyncAt = p[LAST_SYNC_AT],
                space = p[SPACE],
            )
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
            p.remove(SPACE)
        }
    }

    /**
     * Records the data space the server synced with. A new space (joined or left a household)
     * restarts the pull from the beginning; ignored if the session ended meanwhile.
     */
    suspend fun saveSpace(token: String, space: String, restart: Boolean) {
        dataStore.edit { p ->
            if (p[TOKEN] != token) return@edit
            p[SPACE] = space
            if (restart) p[CURSOR] = 0L
        }
    }

    /** Id of the newest household event the user has looked at; drives the unread badge. */
    val activitySeenId: Flow<Long> = prefs.map { it[ACTIVITY_SEEN] ?: 0L }.distinctUntilChanged()

    suspend fun markActivitySeen(id: Long) {
        dataStore.edit { p -> if (id > (p[ACTIVITY_SEEN] ?: 0L)) p[ACTIVITY_SEEN] = id }
    }

    /** Id of the newest household event already notified about; `null` before the first check. */
    suspend fun activityNotifiedId(): Long? = prefs.first()[ACTIVITY_NOTIFIED]

    suspend fun markActivityNotified(id: Long) {
        dataStore.edit { p -> if (id > (p[ACTIVITY_NOTIFIED] ?: 0L)) p[ACTIVITY_NOTIFIED] = id }
    }

    suspend fun signOut() {
        dataStore.edit { p ->
            p.remove(ACTIVITY_SEEN)
            p.remove(ACTIVITY_NOTIFIED)
            p.remove(TOKEN)
            p.remove(EMAIL)
            p.remove(CURSOR)
            p.remove(LAST_SYNC_AT)
            p.remove(SPACE)
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
        val SPACE = stringPreferencesKey("sync_space")
        val ACTIVITY_SEEN = longPreferencesKey("activity_seen_id")
        val ACTIVITY_NOTIFIED = longPreferencesKey("activity_notified_id")
    }
}
