package com.shelfie.app.data.local.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.shelfie.app.data.local.entity.MealPlanEntity
import com.shelfie.app.data.local.entity.PantryItemEntity
import com.shelfie.app.data.local.entity.SavedRecipeEntity
import com.shelfie.app.data.local.entity.ShoppingItemEntity
import com.shelfie.app.data.local.entity.SyncApplyingEntity
import com.shelfie.app.data.local.entity.SyncOutboxEntity
import kotlinx.coroutines.flow.Flow

/** Everything the sync engine reads and writes; see `SyncManager`. */
@Dao
interface SyncDao {

    // region Outbox

    @Query("SELECT * FROM sync_outbox ORDER BY changed_at LIMIT :limit")
    suspend fun getOutbox(limit: Int): List<SyncOutboxEntity>

    @Query("SELECT COUNT(*) FROM sync_outbox")
    fun observeOutboxCount(): Flow<Int>

    @Query("SELECT sync_id FROM sync_outbox WHERE collection = :collection")
    suspend fun getPendingSyncIds(collection: String): List<String>

    /** Only removes the entry if it wasn't changed again since it was read. */
    @Query("DELETE FROM sync_outbox WHERE collection = :collection AND sync_id = :syncId AND changed_at = :changedAt")
    suspend fun deleteOutboxEntry(collection: String, syncId: String, changedAt: Long)

    @Query("DELETE FROM sync_outbox")
    suspend fun clearOutbox()

    /**
     * Queues every local record, e.g. right after signing in, so the account receives what was
     * on the phone before. Uses the record's own time where it has one, so a newer copy already
     * on the server wins the conflict.
     */
    @Query(
        """
        INSERT OR IGNORE INTO sync_outbox (collection, sync_id, changed_at)
        SELECT 'pantry', sync_id, updated_at FROM pantry_items
        UNION ALL SELECT 'shopping', sync_id, created_at FROM shopping_items
        UNION ALL SELECT 'recipes', sync_id, COALESCE(cooked_at, created_at) FROM saved_recipes
        UNION ALL SELECT 'mealplan', sync_id, updated_at FROM meal_plan
        """
    )
    suspend fun enqueueAll()

    // endregion

    // region Applying flag

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setApplying(flag: SyncApplyingEntity = SyncApplyingEntity())

    @Query("DELETE FROM sync_applying")
    suspend fun clearApplying()

    // endregion

    // region Records by sync id

    @Query("SELECT * FROM pantry_items WHERE sync_id = :syncId")
    suspend fun getPantryItem(syncId: String): PantryItemEntity?

    @Query("SELECT * FROM shopping_items WHERE sync_id = :syncId")
    suspend fun getShoppingItem(syncId: String): ShoppingItemEntity?

    @Query("SELECT * FROM saved_recipes WHERE sync_id = :syncId")
    suspend fun getRecipe(syncId: String): SavedRecipeEntity?

    @Query("SELECT * FROM meal_plan WHERE sync_id = :syncId")
    suspend fun getMealPlan(syncId: String): MealPlanEntity?

    @Upsert
    suspend fun upsertMealPlan(entry: MealPlanEntity)

    @Query("DELETE FROM meal_plan WHERE sync_id = :syncId")
    suspend fun deleteMealPlan(syncId: String)

    @Upsert
    suspend fun upsertPantryItem(item: PantryItemEntity)

    @Upsert
    suspend fun upsertShoppingItem(item: ShoppingItemEntity)

    @Upsert
    suspend fun upsertRecipe(recipe: SavedRecipeEntity)

    @Query("DELETE FROM pantry_items WHERE sync_id = :syncId")
    suspend fun deletePantryItem(syncId: String)

    @Query("DELETE FROM shopping_items WHERE sync_id = :syncId")
    suspend fun deleteShoppingItem(syncId: String)

    @Query("DELETE FROM saved_recipes WHERE sync_id = :syncId")
    suspend fun deleteRecipe(syncId: String)

    /** Recipes refer to pantry items by local id on the phone and by sync id on the server. */
    @Query("SELECT id, sync_id FROM pantry_items WHERE id IN (:ids)")
    suspend fun getPantrySyncIds(ids: List<Long>): List<PantryIdPair>

    @Query("SELECT id, sync_id FROM pantry_items WHERE sync_id IN (:syncIds)")
    suspend fun getPantryLocalIds(syncIds: List<String>): List<PantryIdPair>

    // endregion
}

data class PantryIdPair(
    @ColumnInfo(name = "id") val id: Long,
    @ColumnInfo(name = "sync_id") val syncId: String,
)
