package com.shelfie.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

/**
 * A saved recipe as it reads in one app language: the whole recipe JSON, translated. Local
 * cache only, never synced; a recipe generated on this phone is stored here in its own language
 * too, so switching back costs nothing.
 */
@Entity(
    tableName = RecipeTranslationEntity.TABLE_NAME,
    primaryKeys = [RecipeTranslationEntity.COLUMN_RECIPE_ID, RecipeTranslationEntity.COLUMN_LANGUAGE],
)
data class RecipeTranslationEntity(
    /** [SavedRecipeEntity.id] of the recipe. */
    @ColumnInfo(name = COLUMN_RECIPE_ID)
    val recipeId: Long,

    /** ISO 639 language code, e.g. "en". */
    @ColumnInfo(name = COLUMN_LANGUAGE)
    val language: String,

    /** Same shape as [SavedRecipeEntity.recipeJson]. */
    @ColumnInfo(name = COLUMN_RECIPE_JSON)
    val recipeJson: String,
) {
    companion object {
        const val TABLE_NAME = "recipe_translations"

        const val COLUMN_RECIPE_ID = "recipe_id"
        const val COLUMN_LANGUAGE = "language"
        const val COLUMN_RECIPE_JSON = "recipe_json"
    }
}
