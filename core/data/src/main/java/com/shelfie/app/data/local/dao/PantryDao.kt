package com.shelfie.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.shelfie.app.data.local.entity.PantryItemEntity
import com.shelfie.app.domain.model.Category
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate

/**
 * Unless stated otherwise, list queries return only active (not consumed) items, ordered by
 * soonest expiry first and then by name for a stable order among items expiring the same day.
 */
@Dao
interface PantryDao {

    // region Observe

    @Query(
        """
        SELECT * FROM pantry_items
        WHERE is_consumed = 0
        ORDER BY expiry_date ASC, name COLLATE NOCASE ASC
        """
    )
    fun observeActiveItems(): Flow<List<PantryItemEntity>>

    /** Includes consumed items; useful for history and statistics. */
    @Query("SELECT * FROM pantry_items ORDER BY expiry_date ASC, name COLLATE NOCASE ASC")
    fun observeAllItems(): Flow<List<PantryItemEntity>>

    @Query("SELECT * FROM pantry_items WHERE id = :id")
    fun observeItemById(id: Long): Flow<PantryItemEntity?>

    @Query(
        """
        SELECT * FROM pantry_items
        WHERE is_consumed = 0 AND category = :category
        ORDER BY expiry_date ASC, name COLLATE NOCASE ASC
        """
    )
    fun observeItemsByCategory(category: Category): Flow<List<PantryItemEntity>>

    /** Items expiring on or before [date], including already expired ones. */
    @Query(
        """
        SELECT * FROM pantry_items
        WHERE is_consumed = 0 AND expiry_date <= :date
        ORDER BY expiry_date ASC, name COLLATE NOCASE ASC
        """
    )
    fun observeItemsExpiringOnOrBefore(date: LocalDate): Flow<List<PantryItemEntity>>

    /** Items expiring within the inclusive range [[start], [end]]. */
    @Query(
        """
        SELECT * FROM pantry_items
        WHERE is_consumed = 0 AND expiry_date BETWEEN :start AND :end
        ORDER BY expiry_date ASC, name COLLATE NOCASE ASC
        """
    )
    fun observeItemsExpiringBetween(start: LocalDate, end: LocalDate): Flow<List<PantryItemEntity>>

    /** Items expiring on or after [date]. */
    @Query(
        """
        SELECT * FROM pantry_items
        WHERE is_consumed = 0 AND expiry_date >= :date
        ORDER BY expiry_date ASC, name COLLATE NOCASE ASC
        """
    )
    fun observeItemsExpiringOnOrAfter(date: LocalDate): Flow<List<PantryItemEntity>>

    /** Case-insensitive substring match on the item name. */
    @Query(
        """
        SELECT * FROM pantry_items
        WHERE is_consumed = 0 AND name LIKE '%' || :query || '%' ESCAPE '\'
        ORDER BY expiry_date ASC, name COLLATE NOCASE ASC
        """
    )
    fun searchActiveItems(query: String): Flow<List<PantryItemEntity>>

    /** Consumed or wasted items whose last update (the removal) is at or after [since]. */
    @Query(
        """
        SELECT * FROM pantry_items
        WHERE is_consumed = 1 AND updated_at >= :since
        ORDER BY updated_at DESC
        """
    )
    fun observeRemovedSince(since: Instant): Flow<List<PantryItemEntity>>

    @Query("SELECT COUNT(*) FROM pantry_items WHERE is_consumed = 0")
    fun observeActiveItemCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM pantry_items WHERE is_consumed = 0 AND expiry_date <= :date")
    fun observeCountExpiringOnOrBefore(date: LocalDate): Flow<Int>

    // endregion

    // region One-shot reads

    @Query("SELECT * FROM pantry_items WHERE id = :id")
    suspend fun getItemById(id: Long): PantryItemEntity?

    @Query(
        """
        SELECT * FROM pantry_items
        WHERE is_consumed = 0 AND expiry_date <= :date
        ORDER BY expiry_date ASC, name COLLATE NOCASE ASC
        """
    )
    suspend fun getItemsExpiringOnOrBefore(date: LocalDate): List<PantryItemEntity>

    // endregion

    // region Write

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(item: PantryItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(items: List<PantryItemEntity>): List<Long>

    @Upsert
    suspend fun upsert(item: PantryItemEntity): Long

    /** Returns the number of rows updated (0 if the item no longer exists). */
    @Update
    suspend fun update(item: PantryItemEntity): Int

    @Query("UPDATE pantry_items SET is_consumed = 1, updated_at = :updatedAt WHERE id = :id")
    suspend fun markAsConsumed(id: Long, updatedAt: Instant): Int

    /** Only active items change, so an item already thrown away isn't re-labelled as eaten. */
    @Query(
        "UPDATE pantry_items SET is_consumed = 1, updated_at = :updatedAt WHERE id IN (:ids) AND is_consumed = 0"
    )
    suspend fun markAllAsConsumed(ids: List<Long>, updatedAt: Instant): Int

    @Query(
        "UPDATE pantry_items SET is_consumed = 1, is_wasted = 1, updated_at = :updatedAt WHERE id = :id"
    )
    suspend fun markAsWasted(id: Long, updatedAt: Instant): Int

    /** Puts a consumed or wasted item back into the pantry (e.g. to undo a swipe). */
    @Query(
        "UPDATE pantry_items SET is_consumed = 0, is_wasted = 0, updated_at = :updatedAt WHERE id = :id"
    )
    suspend fun restoreToPantry(id: Long, updatedAt: Instant): Int

    @Query("UPDATE pantry_items SET quantity = :quantity, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateQuantity(id: Long, quantity: Double, updatedAt: Instant): Int

    @Delete
    suspend fun delete(item: PantryItemEntity): Int

    @Query("DELETE FROM pantry_items WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    /** Deletes items whose expiry date is strictly before [date]. */
    @Query("DELETE FROM pantry_items WHERE expiry_date < :date")
    suspend fun deleteExpiredBefore(date: LocalDate): Int

    @Query("DELETE FROM pantry_items WHERE is_consumed = 1")
    suspend fun deleteConsumedItems(): Int

    @Query("DELETE FROM pantry_items")
    suspend fun deleteAll()

    // endregion
}
