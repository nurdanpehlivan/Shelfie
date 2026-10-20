package com.shelfie.app.data.local.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.shelfie.app.data.local.entity.SavedRecipeEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface SavedRecipeDao {

    @Query("SELECT * FROM saved_recipes ORDER BY created_at DESC")
    fun observeAll(): Flow<List<SavedRecipeEntity>>

    @Query("SELECT * FROM saved_recipes WHERE id = :id")
    suspend fun getById(id: Long): SavedRecipeEntity?

    /** Local id ↔ sync id of every recipe, for meal plan slots that refer to recipes by sync id. */
    @Query("SELECT id, sync_id FROM saved_recipes")
    fun observeIdPairs(): Flow<List<RecipeIdPair>>

    @Query("SELECT sync_id FROM saved_recipes WHERE id = :id")
    suspend fun getSyncId(id: Long): String?

    /** Latest recipe for exactly these (encoded) pantry item ids, created at or after [since]. */
    @Query(
        """
        SELECT * FROM saved_recipes
        WHERE pantry_item_ids = :pantryItemIds AND created_at >= :since
        ORDER BY created_at DESC
        LIMIT 1
        """
    )
    suspend fun getLatestFor(pantryItemIds: String, since: Instant): SavedRecipeEntity?

    @Query("SELECT COUNT(*) FROM saved_recipes WHERE cooked_at IS NOT NULL AND cooked_at >= :since")
    fun observeCookedCountSince(since: Instant): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(recipe: SavedRecipeEntity): Long

    @Query("UPDATE saved_recipes SET is_favorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean): Int

    /** Keeps the first cooking time if the recipe is cooked again. */
    @Query("UPDATE saved_recipes SET cooked_at = COALESCE(cooked_at, :cookedAt) WHERE id = :id")
    suspend fun markCooked(id: Long, cookedAt: Instant): Int

    @Query("DELETE FROM saved_recipes WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("DELETE FROM saved_recipes")
    suspend fun deleteAll()
}

data class RecipeIdPair(
    @ColumnInfo(name = "id") val id: Long,
    @ColumnInfo(name = "sync_id") val syncId: String,
)
