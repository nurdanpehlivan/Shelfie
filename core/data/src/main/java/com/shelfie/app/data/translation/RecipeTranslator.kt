package com.shelfie.app.data.translation

import com.shelfie.app.data.local.dao.RecipeTranslationDao
import com.shelfie.app.data.local.entity.RecipeTranslationEntity
import com.shelfie.app.data.mapper.GeminiRecipeParser
import com.shelfie.app.data.remote.gemini.GeminiRemoteDataSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Saved recipes in the app's current language. A recipe is translated whole the first time it
 * is opened in another language and cached per language in [RecipeTranslationDao]; lists only
 * need titles, which [ItemNameTranslator] does in cheap batches.
 */
@Singleton
class RecipeTranslator @Inject constructor(
    private val dao: RecipeTranslationDao,
    private val gemini: GeminiRemoteDataSource,
    private val parser: GeminiRecipeParser,
    private val appLanguage: AppLanguage,
) {

    /** Cached recipe JSON in the current language, by recipe id. */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeCurrent(): Flow<Map<Long, String>> =
        appLanguage.language.flatMapLatest { language ->
            dao.observeLanguage(language).map { rows -> rows.associate { it.recipeId to it.recipeJson } }
        }

    /** A recipe just generated is already in the current language. */
    suspend fun remember(recipeId: Long, recipeJson: String) {
        dao.upsert(RecipeTranslationEntity(recipeId, appLanguage.language.value, recipeJson))
    }

    /**
     * [recipeJson] in the current language: cached, or translated now. `null` when that fails
     * (offline, out of allowance, an answer that isn't a valid recipe); show the original then.
     */
    suspend fun translated(recipeId: Long, recipeJson: String): String? {
        val language = appLanguage.language.value
        dao.get(recipeId, language)?.let { return it }
        return try {
            val translatedJson = gemini.translateRecipe(recipeJson, appLanguage.locale)
            // The same validation as a generated recipe; a mangled answer is never stored.
            parser.parse(translatedJson)
            dao.upsert(RecipeTranslationEntity(recipeId, language, translatedJson))
            translatedJson
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    suspend fun forget(recipeId: Long) = dao.deleteRecipe(recipeId)
}
