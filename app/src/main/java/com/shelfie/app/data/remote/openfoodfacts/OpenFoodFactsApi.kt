package com.shelfie.app.data.remote.openfoodfacts

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Open Food Facts, a free and open product database: no API key, but its usage policy asks for
 * a descriptive User-Agent (set in NetworkModule).
 */
interface OpenFoodFactsApi {

    /** [fields] limits the payload; see [fieldsFor]. */
    @GET("api/v2/product/{barcode}.json")
    suspend fun getProduct(
        @Path("barcode") barcode: String,
        @Query("fields") fields: String,
    ): OffProductResponse

    companion object {
        const val BASE_URL = "https://world.openfoodfacts.org/"
        const val USER_AGENT = "Shelfie/1.0 (Android pantry app)"

        /** Names come per language ("product_name_de"), so the request asks for the user's. */
        fun fieldsFor(language: String): String =
            "product_name,product_name_$language,generic_name,generic_name_$language,categories_tags"
    }
}

/**
 * `status` is 1 when the product exists, 0 otherwise. [product] stays a raw object because its
 * name keys depend on the language that was asked for.
 */
@Serializable
data class OffProductResponse(
    val status: Int = 0,
    val product: JsonObject? = null,
)
