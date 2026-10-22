package com.shelfie.app.domain.repository

import com.shelfie.app.domain.model.Category
import com.shelfie.app.domain.model.FreshnessStatus
import com.shelfie.app.domain.model.FreshnessThresholds
import com.shelfie.app.domain.model.PantryItem
import com.shelfie.app.domain.model.ScannedItem
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate

/** All list streams emit active (not consumed) items ordered by soonest expiry first. */
interface PantryRepository {

    fun observePantryItems(): Flow<List<PantryItem>>

    fun observePantryItem(id: Long): Flow<PantryItem?>

    fun observeItemsByCategory(category: Category): Flow<List<PantryItem>>

    fun observeItemsByFreshness(
        status: FreshnessStatus,
        today: LocalDate,
        thresholds: FreshnessThresholds = FreshnessThresholds.DEFAULT,
    ): Flow<List<PantryItem>>

    /**
     * Items that left the pantry (eaten or thrown away) on or after [since], newest first.
     * The time an item left is its last update.
     */
    fun observeRemovedItemsSince(since: Instant): Flow<List<PantryItem>>

    /** Marks every item in [ids] as eaten in one transaction; returns how many changed. */
    suspend fun markAllAsConsumed(ids: Collection<Long>): Int

    fun searchItems(query: String): Flow<List<PantryItem>>

    suspend fun getPantryItem(id: Long): PantryItem?

    /** Returns the id of the inserted item. */
    suspend fun addPantryItem(item: PantryItem): Long

    suspend fun addPantryItems(items: List<PantryItem>): List<Long>

    suspend fun updatePantryItem(item: PantryItem)

    suspend fun markAsConsumed(id: Long)

    /** Marks the item as thrown away; it leaves the active lists like a consumed item. */
    suspend fun markAsWasted(id: Long)

    /** Undoes [markAsConsumed] / [markAsWasted]. */
    suspend fun restoreToPantry(id: Long)

    suspend fun deletePantryItem(id: Long)

    /** Removes items that expired strictly before [today]; returns the number deleted. */
    suspend fun deleteExpiredItems(today: LocalDate): Int

    /**
     * Detects food items in the fridge/pantry/receipt photo at [imageUri] (a local content URI).
     *
     * The returned items are **not** persisted, so the user can review them first; pass them to
     * [addPantryItems] to save. A failure always carries a
     * [com.shelfie.app.domain.model.PantryScanException]. An empty list means nothing was found.
     */
    suspend fun scanPantryImage(imageUri: String): Result<List<ScannedItem>>
}
