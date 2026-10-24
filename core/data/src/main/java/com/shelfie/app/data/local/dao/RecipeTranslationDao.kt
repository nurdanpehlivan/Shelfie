package com.shelfie.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.shelfie.app.data.local.entity.RecipeTranslationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeTranslationDao {

    @Query("SELECT * FROM recipe_translations WHERE language = :language")
    fun observeLanguage(language: String): Flow<List<RecipeTranslationEntity>>

    @Query("SELECT recipe_json FROM recipe_translations WHERE recipe_id = :recipeId AND language = :language")
    suspend fun get(recipeId: Long, language: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(translation: RecipeTranslationEntity)

    @Query("DELETE FROM recipe_translations WHERE recipe_id = :recipeId")
    suspend fun deleteRecipe(recipeId: Long)
}
