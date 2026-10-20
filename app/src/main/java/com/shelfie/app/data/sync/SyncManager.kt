package com.shelfie.app.data.sync

import androidx.room.withTransaction
import com.shelfie.app.data.local.ShelfieDatabase
import com.shelfie.app.data.local.dao.SyncDao
import com.shelfie.app.data.local.entity.SavedRecipeEntity
import com.shelfie.app.data.local.entity.SyncCollections
import com.shelfie.app.data.local.entity.SyncOutboxEntity
import com.shelfie.app.data.remote.shelfie.ShelfieApiService
import com.shelfie.app.data.remote.shelfie.SyncChangeDto
import com.shelfie.app.data.remote.shelfie.SyncRequest
import com.shelfie.app.data.sync.SyncRecordMapper.toRecord
import com.shelfie.app.domain.model.SyncException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException
import java.io.IOException
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Two-way sync of the pantry, shopping list and recipe history with the signed-in account.
 *
 * Each round pushes up to [PUSH_BATCH] queued local changes (the outbox) and gets back what
 * changed on the server since the saved cursor, then repeats until both sides are drained.
 * Conflicts are resolved on the server by last write wins. A record changed locally while a
 * round was in flight stays queued, and the server's copy of it is not applied, so the newer
 * local change is pushed next round instead of being overwritten.
 */
@Singleton
class SyncManager @Inject constructor(
    private val database: ShelfieDatabase,
    private val syncDao: SyncDao,
    private val api: ShelfieApiService,
    private val accountStore: AccountStore,
    private val clock: Clock,
) {

    private val mutex = Mutex()

    /**
     * Runs a full sync. Does nothing when signed out. A rejected session (401) signs the user
     * out locally; the failure carries a [SyncException].
     */
    suspend fun sync(): Result<Unit> = mutex.withLock {
        val session = accountStore.currentSession() ?: return Result.success(Unit)
        try {
            var cursor = session.cursor
            repeat(MAX_ROUNDS) {
                val outbox = syncDao.getOutbox(PUSH_BATCH)
                val response = api.sync(
                    bearer = ShelfieApiService.bearer(session.token),
                    request = SyncRequest(cursor = cursor, changes = outbox.mapNotNull { it.toChange() }),
                )
                database.withTransaction {
                    syncDao.setApplying()
                    try {
                        // Done before applying, so the server's answer for these records applies.
                        outbox.forEach { syncDao.deleteOutboxEntry(it.collection, it.syncId, it.changedAt) }
                        applyRemote(response.records)
                    } finally {
                        syncDao.clearApplying()
                    }
                }
                cursor = response.cursor
                val done = !response.hasMore && outbox.size < PUSH_BATCH
                accountStore.saveProgress(session.token, cursor, syncedAt = if (done) clock.millis() else null)
                if (done) return Result.success(Unit)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            if (e.code() == HTTP_UNAUTHORIZED) {
                signOutLocally()
                Result.failure(SyncException.SessionExpired(e))
            } else {
                Result.failure(SyncException.Server(e.code(), e))
            }
        } catch (e: IOException) {
            Result.failure(SyncException.Network(e))
        } catch (e: Exception) {
            Result.failure(SyncException.Server(null, e))
        }
    }

    /** Queues everything on the phone for the account that just signed in. */
    suspend fun onSignedIn() {
        syncDao.enqueueAll()
    }

    /** Local data stays; only what links it to the account is dropped. */
    suspend fun signOutLocally() {
        accountStore.signOut()
        syncDao.clearOutbox()
    }

    /** The outbox entry as a change to push; a record that no longer exists was deleted. */
    private suspend fun SyncOutboxEntity.toChange(): SyncChangeDto? {
        val data = when (collection) {
            SyncCollections.PANTRY -> syncDao.getPantryItem(syncId)?.toRecord()
            SyncCollections.SHOPPING -> syncDao.getShoppingItem(syncId)?.toRecord()
            SyncCollections.RECIPES -> syncDao.getRecipe(syncId)?.let { recipe ->
                val ids = SavedRecipeEntity.decodeIds(recipe.pantryItemIds)
                recipe.toRecord(if (ids.isEmpty()) emptyList() else syncDao.getPantrySyncIds(ids).map { it.syncId })
            }
            else -> return null
        }
        return SyncChangeDto(collection = collection, syncId = syncId, data = data, deleted = data == null, changedAt = changedAt)
    }

    private suspend fun applyRemote(records: List<SyncChangeDto>) {
        if (records.isEmpty()) return
        val pending = mutableMapOf<String, Set<String>>()
        suspend fun isPending(change: SyncChangeDto) =
            change.syncId in pending.getOrPut(change.collection) { syncDao.getPendingSyncIds(change.collection).toSet() }

        // Pantry first, so recipes in the same page can resolve their pantry items.
        for (change in records.sortedBy { COLLECTION_ORDER.indexOf(it.collection) }) {
            if (isPending(change)) continue
            val data = change.data
            when (change.collection) {
                SyncCollections.PANTRY ->
                    if (change.deleted || data == null) {
                        syncDao.deletePantryItem(change.syncId)
                    } else {
                        SyncRecordMapper.pantryFromRecord(change.syncId, data, syncDao.getPantryItem(change.syncId))
                            ?.let { syncDao.upsertPantryItem(it) }
                    }
                SyncCollections.SHOPPING ->
                    if (change.deleted || data == null) {
                        syncDao.deleteShoppingItem(change.syncId)
                    } else {
                        SyncRecordMapper.shoppingFromRecord(change.syncId, data, syncDao.getShoppingItem(change.syncId))
                            ?.let { syncDao.upsertShoppingItem(it) }
                    }
                SyncCollections.RECIPES ->
                    if (change.deleted || data == null) {
                        syncDao.deleteRecipe(change.syncId)
                    } else {
                        val pantrySyncIds = SyncRecordMapper.recipePantrySyncIds(data)
                        val localIds = if (pantrySyncIds.isEmpty()) emptyList() else syncDao.getPantryLocalIds(pantrySyncIds).map { it.id }
                        SyncRecordMapper.recipeFromRecord(change.syncId, data, syncDao.getRecipe(change.syncId), localIds)
                            ?.let { syncDao.upsertRecipe(it) }
                    }
            }
        }
    }

    private companion object {
        const val PUSH_BATCH = 200
        /** Bounds one sync run; the rest follows on the next one. */
        const val MAX_ROUNDS = 50
        const val HTTP_UNAUTHORIZED = 401
        val COLLECTION_ORDER = listOf(SyncCollections.PANTRY, SyncCollections.SHOPPING, SyncCollections.RECIPES)
    }
}
