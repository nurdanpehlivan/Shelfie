package com.shelfie.app.di

import com.shelfie.app.BuildConfig
import com.shelfie.app.data.remote.gemini.GeminiApiService
import com.shelfie.app.data.remote.gemini.GeminiConfig
import com.shelfie.app.data.remote.openfoodfacts.OpenFoodFactsApi
import com.shelfie.app.data.remote.shelfie.ShelfieApiService
import com.shelfie.app.data.sync.AccountStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val CONNECT_TIMEOUT_SECONDS = 15L

    /** Vision requests with thinking models can take a while to respond. */
    private const val GEMINI_READ_TIMEOUT_SECONDS = 90L

    private const val SYNC_TIMEOUT_SECONDS = 30L

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideGeminiConfig(): GeminiConfig = GeminiConfig(
        baseUrl = BuildConfig.SHELFIE_PROXY_URL.let { if (it.endsWith('/')) it else "$it/" },
        proxyToken = BuildConfig.SHELFIE_PROXY_TOKEN,
        models = (listOf(BuildConfig.GEMINI_MODEL) + BuildConfig.GEMINI_FALLBACK_MODELS.split(','))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct(),
    )

    /** Shared base client; API-specific clients derive from it via [OkHttpClient.newBuilder]. */
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .apply {
            if (BuildConfig.DEBUG) {
                // HEADERS, not BODY: bodies carry base64 photos (huge) and pantry content.
                addInterceptor(
                    HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.HEADERS
                        redactHeader(GeminiApiService.PROXY_TOKEN_HEADER)
                        redactHeader(ShelfieApiService.AUTHORIZATION)
                    },
                )
            }
        }
        .build()

    @Provides
    @Singleton
    fun provideGeminiApiService(
        okHttpClient: OkHttpClient,
        json: Json,
        config: GeminiConfig,
        accountStore: AccountStore,
    ): GeminiApiService {
        val geminiClient = okHttpClient.newBuilder()
            .readTimeout(GEMINI_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(GEMINI_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header(GeminiApiService.PROXY_TOKEN_HEADER, config.proxyToken)
                        // For the usage dashboard; read from DataStore once, then cached.
                        // OkHttp runs interceptors off the main thread.
                        .header(GeminiApiService.INSTALL_HEADER, runBlocking { accountStore.installId() })
                        .header(GeminiApiService.VERSION_HEADER, BuildConfig.VERSION_NAME)
                        .build(),
                )
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(config.baseUrl)
            .client(geminiClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GeminiApiService::class.java)
    }

    /** Accounts and sync, served by the same Worker as the Gemini proxy. */
    @Provides
    @Singleton
    fun provideShelfieApiService(okHttpClient: OkHttpClient, json: Json, config: GeminiConfig): ShelfieApiService =
        Retrofit.Builder()
            .baseUrl(config.baseUrl)
            .client(
                okHttpClient.newBuilder()
                    .readTimeout(SYNC_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .writeTimeout(SYNC_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .build(),
            )
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ShelfieApiService::class.java)

    @Provides
    @Singleton
    fun provideOpenFoodFactsApi(okHttpClient: OkHttpClient, json: Json): OpenFoodFactsApi {
        val client = okHttpClient.newBuilder()
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("User-Agent", OpenFoodFactsApi.USER_AGENT)
                        .build(),
                )
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(OpenFoodFactsApi.BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(OpenFoodFactsApi::class.java)
    }
}
