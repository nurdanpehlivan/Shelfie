package com.shelfie.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = SavedRecipeEntity.TABLE_NAME,
    indices = [
        Index(value = [SavedRecipeEntity.COLUMN_CREATED_AT]),
        Index(value = [SavedRecipeEntity.COLUMN_PANTRY_ITEM_IDS]),
        Index(value = [SavedRecipeEntity.COLUMN_SYNC_ID], unique = true),
    ],
)
data class SavedRecipeEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = COLUMN_ID)
    val id: Long = 0L,

    /** Duplicated out of [recipeJson] for readability of the table; the JSON is the source. */
    @ColumnInfo(name = COLUMN_TITLE)
    val title: String,

    /** The model's raw JSON answer; re-validated by `GeminiRecipeParser` when read. */
    @ColumnInfo(name = COLUMN_RECIPE_JSON)
    val recipeJson: String,

    /** Sorted, comma-separated ids, so "same ingredients" is a plain string comparison. */
    @ColumnInfo(name = COLUMN_PANTRY_ITEM_IDS)
    val pantryItemIds: String,

    @ColumnInfo(name = COLUMN_IS_FAVORITE, defaultValue = "0")
    val isFavorite: Boolean = false,

    /** Stored as epoch milliseconds. */
    @ColumnInfo(name = COLUMN_CREATED_AT)
    val createdAt: Instant,

    @ColumnInfo(name = COLUMN_COOKED_AT)
    val cookedAt: Instant? = null,

    /** Stable id across devices for sync; the auto-generated [id] is only local. */
    @ColumnInfo(name = COLUMN_SYNC_ID, defaultValue = "''")
    val syncId: String = newSyncId(),
) {
    companion object {
        const val TABLE_NAME = "saved_recipes"

        const val COLUMN_ID = "id"
        const val COLUMN_TITLE = "title"
        const val COLUMN_RECIPE_JSON = "recipe_json"
        const val COLUMN_PANTRY_ITEM_IDS = "pantry_item_ids"
        const val COLUMN_IS_FAVORITE = "is_favorite"
        const val COLUMN_CREATED_AT = "created_at"
        const val COLUMN_COOKED_AT = "cooked_at"
        const val COLUMN_SYNC_ID = "sync_id"

        fun encodeIds(ids: Collection<Long>): String = ids.distinct().sorted().joinToString(",")

        fun decodeIds(value: String): List<Long> = value.split(',').mapNotNull { it.trim().toLongOrNull() }
    }
}
