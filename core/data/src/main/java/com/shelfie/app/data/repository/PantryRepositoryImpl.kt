package com.shelfie.app.data.repository

import com.shelfie.app.data.local.dao.PantryDao
import com.shelfie.app.data.local.entity.PantryItemEntity
import com.shelfie.app.data.mapper.GeminiScanResultParser
import com.shelfie.app.data.mapper.toDomain
import com.shelfie.app.data.mapper.toEntity
import com.shelfie.app.data.remote.gemini.GeminiRemoteDataSource
import com.shelfie.app.domain.model.Category
import com.shelfie.app.domain.model.FreshnessStatus
import com.shelfie.app.domain.model.FreshnessThresholds
import com.shelfie.app.domain.model.PantryItem
import com.shelfie.app.domain.model.PantryScanException
import com.shelfie.app.domain.repository.PantryRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PantryRepositoryImpl @Inject constructor(
    private val pantryDao: PantryDao,
    private val geminiRemoteDataSource: GeminiRemoteDataSource,
    private val scanResultParser: GeminiScanResultParser,
    private val clock: Clock,
) : PantryRepository {

    override fun observePantryItems(): Flow<List<PantryItem>> =
        pantryDao.observeActiveItems().mapToDomain()

    override fun observePantryItem(id: Long): Flow<PantryItem?> =
        pantryDao.observeItemById(id)
            .map { it?.toDomain() }
            .distinctUntilChanged()

    override fun observeItemsByCategory(category: Category): Flow<List<PantryItem>> =
        pantryDao.observeItemsByCategory(category).mapToDomain()

    override fun observeItemsByFreshness(
        status: FreshnessStatus,
        today: LocalDate,
        thresholds: FreshnessThresholds,
    ): Flow<List<PantryItem>> {
        val window = FreshnessStatus.expiryWindow(status, today, thresholds)
        val start = window.start
        val end = window.end
        val source = when {
            start != null && end != null -> pantryDao.observeItemsExpiringBetween(start, end)
            end != null -> pantryDao.observeItemsExpiringOnOrBefore(end)
            start != null -> pantryDao.observeItemsExpiringOnOrAfter(start)
            else -> pantryDao.observeActiveItems()
        }
        return source.mapToDomain()
    }

    override fun observeRemovedItemsSince(since: Instant): Flow<List<PantryItem>> =
        pantryDao.observeRemovedSince(since).mapToDomain()

    override fun searchItems(query: String): Flow<List<PantryItem>> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return observePantryItems()
        return pantryDao.searchActiveItems(trimmed.escapeLikeWildcards()).mapToDomain()
    }

    override suspend fun getPantryItem(id: Long): PantryItem? =
        pantryDao.getItemById(id)?.toDomain()

    override suspend fun addPantryItem(item: PantryItem): Long =
        pantryDao.insert(item.prepareForInsert(now()).toEntity())

    override suspend fun addPantryItems(items: List<PantryItem>): List<Long> {
        if (items.isEmpty()) return emptyList()
        val now = now()
        return pantryDao.insertAll(items.map { it.prepareForInsert(now).toEntity() })
    }

    override suspend fun updatePantryItem(item: PantryItem) {
        val existing = checkNotNull(pantryDao.getItemById(item.id)) { "PantryItem with id=${item.id} does not exist" }
        // The domain model has no sync id; keep the stored one so the server sees an edit, not a
        // new record (a fresh id would duplicate the item on every other device).
        val updatedRows = pantryDao.update(item.copy(updatedAt = now()).toEntity().copy(syncId = existing.syncId))
        check(updatedRows == 1) { "PantryItem with id=${item.id} does not exist" }
    }

    override suspend fun markAsConsumed(id: Long) {
        pantryDao.markAsConsumed(id = id, updatedAt = now())
    }

    override suspend fun markAllAsConsumed(ids: Collection<Long>): Int {
        if (ids.isEmpty()) return 0
        return pantryDao.markAllAsConsumed(ids = ids.distinct(), updatedAt = now())
    }

    override suspend fun markAsWasted(id: Long) {
        pantryDao.markAsWasted(id = id, updatedAt = now())
    }

    override suspend fun restoreToPantry(id: Long) {
        pantryDao.restoreToPantry(id = id, updatedAt = now())
    }

    override suspend fun deletePantryItem(id: Long) {
        pantryDao.deleteById(id)
    }

    override suspend fun deleteExpiredItems(today: LocalDate): Int =
        pantryDao.deleteExpiredBefore(today)

    override suspend fun scanPantryImage(imageUri: String): Result<List<PantryItem>> =
        try {
            val rawJson = geminiRemoteDataSource.analyzePantryImage(imageUri)
            Result.success(scanResultParser.parse(rawJson, imageUri))
        } catch (e: CancellationException) {
            // Never swallow cancellation, or the calling scope keeps running after it's cancelled.
            throw e
        } catch (e: PantryScanException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(PantryScanException.Unknown(e))
        }

    private fun now(): Instant = clock.instant()

    private fun PantryItem.prepareForInsert(now: Instant): PantryItem =
        copy(id = PantryItem.NEW_ITEM_ID, createdAt = now, updatedAt = now)

    private fun Flow<List<PantryItemEntity>>.mapToDomain() =
        map { it.toDomain() }.distinctUntilChanged()

    /** Escapes LIKE wildcards so user input is matched literally (paired with ESCAPE '\'). */
    private fun String.escapeLikeWildcards(): String =
        replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
}
