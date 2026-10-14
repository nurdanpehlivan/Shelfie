package com.shelfie.app.data.remote.gemini

import com.shelfie.app.data.remote.gemini.dto.GenerateContentRequest
import com.shelfie.app.data.remote.gemini.dto.GenerateContentResponse
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

/** The API key is attached by an OkHttp interceptor (see NetworkModule), never in the URL. */
interface GeminiApiService {

    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Body request: GenerateContentRequest,
    ): GenerateContentResponse

    companion object {
        const val BASE_URL = "https://generativelanguage.googleapis.com/"
        const val API_KEY_HEADER = "x-goog-api-key"
    }
}

data class GeminiConfig(
    val apiKey: String,
    /** Preferred model first, then fallbacks tried when it is out of quota or overloaded. */
    val models: List<String>,
)
