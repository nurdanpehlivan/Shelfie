package com.shelfie.app.data.remote.gemini

import android.net.Uri
import com.shelfie.app.data.remote.gemini.dto.GenerateContentRequest
import com.shelfie.app.data.remote.gemini.dto.GenerateContentResponse
import com.shelfie.app.domain.model.MealType
import com.shelfie.app.domain.model.PantryItem
import com.shelfie.app.domain.model.PantryScanException
import kotlinx.coroutines.delay
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException
import java.time.LocalDate
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.io.encoding.Base64
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * Sends requests to Gemini and returns the raw JSON text of the model's answer.
 * Every failure is translated into a [PantryScanException].
 */
@Singleton
class GeminiRemoteDataSource @Inject constructor(
    private val api: GeminiApiService,
    private val imageCompressor: ImageCompressor,
    private val config: GeminiConfig,
) {

    suspend fun analyzePantryImage(
        imageUri: String,
        locale: Locale = Locale.getDefault(),
    ): String {
        requireConfigured()

        val image = try {
            imageCompressor.compress(Uri.parse(imageUri))
        } catch (e: IOException) {
            throw PantryScanException.ImageUnreadable(e)
        } catch (e: SecurityException) {
            // URI permission was revoked or never granted.
            throw PantryScanException.ImageUnreadable(e)
        }

        val request = GeminiPrompts.buildRequest(
            imageBase64 = Base64.encode(image.bytes),
            imageMimeType = image.mimeType,
            locale = locale,
        )
        return generate(request, GeminiApiService.FEATURE_SCAN)
    }

    /** Returns the JSON of a quick recipe using up [ingredients] (see [RecipePrompts]). */
    suspend fun generateRescueRecipe(
        ingredients: List<PantryItem>,
        mealType: MealType,
        today: LocalDate,
        locale: Locale = Locale.getDefault(),
    ): String {
        requireConfigured()
        return generate(RecipePrompts.buildRequest(ingredients, mealType, today, locale), GeminiApiService.FEATURE_RECIPE)
    }

    private fun requireConfigured() {
        if (config.proxyToken.isBlank()) {
            throw PantryScanException.NotConfigured("SHELFIE_PROXY_TOKEN is missing in local.properties")
        }
    }

    private suspend fun generate(request: GenerateContentRequest, feature: String): String {
        val response = try {
            callWithRetry(request, feature)
        } catch (e: HttpException) {
            throw PantryScanException.Api(
                httpCode = e.code(),
                cause = e,
                isDailyQuotaExhausted = e.code() == HTTP_TOO_MANY_REQUESTS && e.isDailyQuota(),
            )
        } catch (e: SerializationException) {
            throw PantryScanException.MalformedResponse("Unexpected response envelope", e)
        } catch (e: IOException) {
            throw PantryScanException.Network(e)
        }

        return response.extractJsonText()
    }

    /** Models that recently ran out of quota or stayed overloaded, skipped until the mark. */
    private val cooldowns = ConcurrentHashMap<String, TimeMark>()

    /**
     * Tries [GeminiConfig.models] in order. Quotas are per model, so when one is used up (429)
     * or stays overloaded (503 even after retries) the next one usually still answers.
     * The last model's error is the one reported.
     */
    private suspend fun callWithRetry(request: GenerateContentRequest, feature: String): GenerateContentResponse {
        val available = config.models.filter { model -> cooldowns[model]?.hasPassedNow() ?: true }
        // If every model is cooling down, try them all anyway rather than fail without asking.
        val candidates = available.ifEmpty { config.models }
        var lastError: HttpException? = null
        for (model in candidates) {
            try {
                return callModelWithRetry(model, request, feature)
            } catch (e: HttpException) {
                if (e.code() != HTTP_TOO_MANY_REQUESTS && e.code() !in TRANSIENT_HTTP_CODES) throw e
                cooldowns[model] = TimeSource.Monotonic.markNow() + MODEL_COOLDOWN
                lastError = e
            }
        }
        throw checkNotNull(lastError)
    }

    /**
     * Gemini answers 503 ("model is experiencing high demand") in short spikes, especially on
     * new models; a retry a moment later usually succeeds, so the user never sees those.
     */
    private suspend fun callModelWithRetry(
        model: String,
        request: GenerateContentRequest,
        feature: String,
    ): GenerateContentResponse {
        RETRY_DELAYS.forEach { retryDelay ->
            try {
                return api.generateContent(model = model, request = request, feature = feature)
            } catch (e: HttpException) {
                if (e.code() !in TRANSIENT_HTTP_CODES) throw e
            }
            delay(retryDelay)
        }
        return api.generateContent(model = model, request = request, feature = feature)
    }

    /** Gemini names the exhausted quota in the error body, e.g. "...RequestsPerDayPerProjectPerModel...". */
    private fun HttpException.isDailyQuota(): Boolean =
        runCatching { response()?.errorBody()?.string().orEmpty() }.getOrDefault("").contains("PerDay")

    private fun GenerateContentResponse.extractJsonText(): String {
        promptFeedback?.blockReason?.let { throw PantryScanException.Blocked(it) }

        val candidate = candidates.firstOrNull()
            ?: throw PantryScanException.MalformedResponse("Response contained no candidates")

        when (candidate.finishReason) {
            in BLOCKING_FINISH_REASONS -> throw PantryScanException.Blocked(candidate.finishReason!!)
            // Truncated JSON is unparseable; report it instead of failing later in the parser.
            "MAX_TOKENS" -> throw PantryScanException.MalformedResponse("Response was truncated")
        }

        val text = candidate.content?.parts.orEmpty()
            .filter { it.thought != true }
            .mapNotNull { it.text }
            .joinToString(separator = "")
        if (text.isBlank()) throw PantryScanException.MalformedResponse("Response had no text")
        return text
    }

    private companion object {
        /** Server-side hiccups worth retrying; 429 (quota) is not, retrying only burns quota. */
        val TRANSIENT_HTTP_CODES = setOf(500, 502, 503, 504)

        /** One entry per retry, so 2 retries after the first attempt. */
        val RETRY_DELAYS = listOf(1.seconds, 3.seconds)

        const val HTTP_TOO_MANY_REQUESTS = 429

        /** Long enough to stop hammering an exhausted model, short enough to notice it's back. */
        val MODEL_COOLDOWN = 10.minutes

        val BLOCKING_FINISH_REASONS = setOf(
            "SAFETY",
            "RECITATION",
            "BLOCKLIST",
            "PROHIBITED_CONTENT",
            "SPII",
            "IMAGE_SAFETY",
        )
    }
}
