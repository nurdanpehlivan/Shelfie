package com.shelfie.app.data.repository

import com.shelfie.app.data.local.dao.SavedRecipeDao
import com.shelfie.app.data.local.entity.SavedRecipeEntity
import com.shelfie.app.data.mapper.GeminiRecipeParser
import com.shelfie.app.data.remote.gemini.GeminiRemoteDataSource
import com.shelfie.app.data.translation.ItemNameTranslator
import com.shelfie.app.data.translation.RecipeTranslator
import com.shelfie.app.domain.model.MealType
import com.shelfie.app.domain.model.PantryItem
import com.shelfie.app.domain.model.PantryScanException
import com.shelfie.app.domain.model.SavedRecipe
import com.shelfie.app.domain.repository.RecipeRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecipeRepositoryImpl @Inject constructor(
    private val geminiRemoteDataSource: GeminiRemoteDataSource,
    private val recipeParser: GeminiRecipeParser,
    private val savedRecipeDao: SavedRecipeDao,
    private val recipeTranslator: RecipeTranslator,
    private val nameTranslator: ItemNameTranslator,
    private val clock: Clock,
) : RecipeRepository {

    override suspend fun generateRescueRecipe(
        ingredients: List<PantryItem>,
        mealType: MealType,
        isBackground: Boolean,
    ): Result<SavedRecipe> {
        require(ingredients.isNotEmpty()) { "A rescue recipe needs at least one ingredient" }
        return try {
            val rawJson = geminiRemoteDataSource.generateRescueRecipe(
                ingredients = ingredients,
                mealType = mealType,
                today = LocalDate.now(clock),
                isBackground = isBackground,
            )
            val recipe = recipeParser.parse(rawJson)
            val entity = SavedRecipeEntity(
                title = recipe.title,
                recipeJson = rawJson,
                pantryItemIds = SavedRecipeEntity.encodeIds(ingredients.map { it.id }),
                createdAt = clock.instant(),
            )
            val id = savedRecipeDao.insert(entity)
            // Written in the current language: no translation needed to show it in it.
            recipeTranslator.remember(id, rawJson)
            nameTranslator.rememberAsCurrentLanguage(listOf(recipe.title))
            Result.success(entity.copy(id = id).toDomain() ?: error("Parsed recipe failed to re-parse"))
        } catch (e: CancellationException) {
            throw e
        } catch (e: PantryScanException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(PantryScanException.Unknown(e))
        }
    }

    override suspend fun getTodaysRecipeFor(ingredients: List<PantryItem>): SavedRecipe? {
        val startOfToday = LocalDate.now(clock).atStartOfDay(clock.zone).toInstant()
        return savedRecipeDao
            .getLatestFor(SavedRecipeEntity.encodeIds(ingredients.map { it.id }), startOfToday)
            ?.inCurrentLanguage()
    }

    /**
     * Lists use a recipe's full translation once it has one; otherwise only the title is
     * translated (in cheap batches), and the rest waits until the recipe is opened.
     */
    override fun observeSavedRecipes(): Flow<List<SavedRecipe>> {
        val recipes = combine(savedRecipeDao.observeAll(), recipeTranslator.observeCurrent()) { rows, translated ->
            rows.mapNotNull { it.toDomain(translated[it.id] ?: it.recipeJson) }
        }
        return nameTranslator.translate(recipes, { it.recipe.title }) { saved, title ->
            saved.copy(recipe = saved.recipe.copy(title = title))
        }.distinctUntilChanged()
    }

    override suspend fun getSavedRecipe(id: Long): SavedRecipe? = savedRecipeDao.getById(id)?.inCurrentLanguage()

    override suspend fun setFavorite(id: Long, isFavorite: Boolean) {
        savedRecipeDao.setFavorite(id, isFavorite)
    }

    override suspend fun markCooked(id: Long) {
        savedRecipeDao.markCooked(id, clock.instant())
    }

    override suspend fun deleteSavedRecipe(id: Long) {
        savedRecipeDao.deleteById(id)
        recipeTranslator.forget(id)
    }

    override fun observeCookedCountSince(since: Instant): Flow<Int> =
        savedRecipeDao.observeCookedCountSince(since)

    /** The whole recipe in the app's language, translated now if needed; the original if that fails. */
    private suspend fun SavedRecipeEntity.inCurrentLanguage(): SavedRecipe? =
        toDomain(recipeTranslator.translated(id, recipeJson) ?: recipeJson) ?: toDomain()

    /** The parser validates the stored JSON again; a row it rejects is left out. */
    private fun SavedRecipeEntity.toDomain(json: String = recipeJson): SavedRecipe? {
        val recipe = runCatching { recipeParser.parse(json) }.getOrNull() ?: return null
        return SavedRecipe(
            id = id,
            recipe = recipe,
            pantryItemIds = SavedRecipeEntity.decodeIds(pantryItemIds),
            isFavorite = isFavorite,
            createdAt = createdAt,
            cookedAt = cookedAt,
        )
    }
}
