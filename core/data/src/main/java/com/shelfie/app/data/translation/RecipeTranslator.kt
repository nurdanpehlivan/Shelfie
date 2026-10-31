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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
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

    /**
     * Cached recipe JSON in the current language, by recipe id. Pass each through
     * [withOriginalDetails] before use.
     */
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
        dao.get(recipeId, language)?.let { return withOriginalDetails(recipeJson, it) }
        return try {
            val translatedJson = withOriginalDetails(recipeJson, gemini.translateRecipe(recipeJson, appLanguage.locale))
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

    companion object {
        private val json = Json { ignoreUnknownKeys = true; isLenient = true }

        /** What a translation may not change: everything but the words. */
        private val KEPT_FIELDS = listOf("cooking_time_minutes", "servings")
        private val KEPT_INGREDIENT_FIELDS = listOf("emoji", "from_pantry")
        private val KEPT_STEP_FIELDS = listOf("duration_minutes")

        /**
         * [translated] with the emojis, times, servings and pantry flags of [original]: the model
         * is only trusted with text, and has been seen writing "|" in place of an emoji. Lists
         * that changed length are left as they are, since items can't be matched up then.
         * Applied on every read too, so translations cached before this rule are fixed as well.
         */
        fun withOriginalDetails(original: String, translated: String): String = runCatching {
            val source = json.parseToJsonElement(original.jsonObjectPart()).jsonObject
            val target = json.parseToJsonElement(translated.jsonObjectPart()).jsonObject
            buildJsonObject {
                target.forEach { (key, value) -> put(key, value) }
                KEPT_FIELDS.forEach { key -> source[key]?.let { put(key, it) } }
                put("ingredients", mergeList(source["ingredients"], target["ingredients"], KEPT_INGREDIENT_FIELDS))
                put("steps", mergeList(source["steps"], target["steps"], KEPT_STEP_FIELDS))
            }.toString()
        }.getOrDefault(translated)

        private fun mergeList(source: JsonElement?, target: JsonElement?, kept: List<String>): JsonElement {
            val sourceItems = (source as? JsonArray) ?: return target ?: JsonArray(emptyList())
            val targetItems = (target as? JsonArray) ?: return sourceItems
            if (sourceItems.size != targetItems.size) return targetItems
            return JsonArray(
                targetItems.mapIndexed { index, item ->
                    val from = sourceItems[index] as? JsonObject
                    val into = item as? JsonObject
                    if (from == null || into == null) {
                        item
                    } else {
                        JsonObject(into + kept.mapNotNull { key -> from[key]?.let { key to it } })
                    }
                },
            )
        }

        private fun String.jsonObjectPart(): String = substring(indexOf('{'), lastIndexOf('}') + 1)
    }
}
