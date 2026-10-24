package com.shelfie.app.data.translation

import com.shelfie.app.data.local.dao.NameTranslationDao
import com.shelfie.app.data.local.entity.NameTranslationEntity
import com.shelfie.app.data.remote.gemini.GeminiRemoteDataSource
import com.shelfie.app.di.ApplicationScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

/**
 * Shows item names in the app's current language. Names are stored as they were written (or
 * scanned), so a pantry filled in Turkish still reads "Mint candy" after switching to English.
 *
 * Translations come from Gemini in batches and are cached per language in [NameTranslationDao];
 * until one arrives the stored name is shown. Names the user writes are assumed to be in the
 * current language and cost no call.
 */
@Singleton
class ItemNameTranslator @Inject constructor(
    private val appLanguage: AppLanguage,
    private val dao: NameTranslationDao,
    private val gemini: GeminiRemoteDataSource,
    @ApplicationScope private val scope: CoroutineScope,
) {

    private val language = appLanguage.language

    /** "language|name" keys already asked for in this process, so a failure isn't retried in a loop. */
    private val requested = ConcurrentHashMap.newKeySet<String>()

    /** Names waiting for the next call, by language; guarded by itself. */
    private val pending = mutableMapOf<String, MutableSet<String>>()


    /** [items] with each name replaced by its translation, updating as translations arrive. */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun <T> translate(items: Flow<List<T>>, nameOf: (T) -> String, rename: (T, String) -> T): Flow<List<T>> {
        val translations = language.flatMapLatest { lang ->
            dao.observeLanguage(lang).map { rows -> lang to rows.associate { it.source to it.translated } }
        }
        return combine(items, translations) { list, (lang, known) ->
            val missing = list.map(nameOf).filter { it.isNotBlank() && it !in known }
            if (missing.isNotEmpty()) request(lang, missing)
            list.map { item -> known[nameOf(item)]?.let { rename(item, it) } ?: item }
        }.distinctUntilChanged()
    }

    suspend fun translate(name: String): String =
        dao.getTranslation(name, language.value) ?: name

    /** Notes that [names] were just written in the current language, so they need no translating. */
    suspend fun rememberAsCurrentLanguage(names: Collection<String>) {
        val lang = language.value
        dao.insertIfAbsent(names.filter { it.isNotBlank() }.distinct().map { NameTranslationEntity(it, lang, it) })
    }

    private fun request(lang: String, names: List<String>) {
        val fresh = names.filter { requested.add("$lang|$it") }
        if (fresh.isEmpty()) return
        val startBatch = synchronized(pending) {
            val wasEmpty = pending.isEmpty()
            pending.getOrPut(lang) { mutableSetOf() } += fresh
            wasEmpty
        }
        if (!startBatch) return
        scope.launch {
            // Lets the other screens' lists join the same call.
            delay(BATCH_DELAY)
            val batch = synchronized(pending) { pending.toMap().also { pending.clear() } }
            batch.forEach { (batchLang, batchNames) ->
                batchNames.chunked(MAX_NAMES_PER_CALL).forEach { fetch(batchLang, it) }
            }
        }
    }

    private suspend fun fetch(lang: String, names: List<String>) {
        try {
            val json = Json.parseToJsonElement(gemini.translateNames(names, Locale.forLanguageTag(lang)))
            val translated = json.jsonObject["translations"]?.jsonArray?.map { it.jsonPrimitive.content.trim() }
            // Matched by position; a short or long answer can't be trusted to line up.
            if (translated == null || translated.size != names.size) return
            dao.upsertAll(
                names.zip(translated).map { (source, target) ->
                    NameTranslationEntity(source, lang, target.ifBlank { source }.take(MAX_NAME_LENGTH))
                },
            )
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Offline, out of quota or a garbled answer: keep showing the stored names.
        }
    }


    private companion object {
        val BATCH_DELAY = 300.milliseconds
        const val MAX_NAMES_PER_CALL = 60
        const val MAX_NAME_LENGTH = 60
    }
}
