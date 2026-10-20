package com.shelfie.app.data.remote.gemini

import com.shelfie.app.data.remote.gemini.dto.GenerateContentRequest
import com.shelfie.app.data.remote.gemini.dto.GenerateContentResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Talks to the Shelfie proxy Worker (backend/), which mirrors Gemini's REST paths and adds the
 * Gemini API key server-side. The app token is attached by an OkHttp interceptor (see NetworkModule).
 */
interface GeminiApiService {

    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Body request: GenerateContentRequest,
        /** What the call is for, for the proxy's usage statistics: [FEATURE_SCAN] or [FEATURE_RECIPE]. */
        @Header(FEATURE_HEADER) feature: String,
    ): GenerateContentResponse

    companion object {
        const val PROXY_TOKEN_HEADER = "x-shelfie-token"
        const val INSTALL_HEADER = "x-shelfie-install"
        const val VERSION_HEADER = "x-shelfie-version"
        const val FEATURE_HEADER = "x-shelfie-feature"
        const val FEATURE_SCAN = "scan"
        const val FEATURE_RECIPE = "recipe"
    }
}

data class GeminiConfig(
    /** Base URL of the proxy Worker, ending in a slash. */
    val baseUrl: String,
    /** Identifies the app to the proxy; not a Gemini key, and can be rotated on the Worker. */
    val proxyToken: String,
    /** Preferred model first, then fallbacks tried when it is out of quota or overloaded. */
    val models: List<String>,
)
