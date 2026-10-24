package com.shelfie.app.data.repository

import com.shelfie.app.data.remote.openfoodfacts.OpenFoodFactsApi
import com.shelfie.app.data.translation.AppLanguage
import com.shelfie.app.domain.model.Category
import com.shelfie.app.domain.model.ProductInfo
import com.shelfie.app.domain.model.ProductLookupException
import com.shelfie.app.domain.repository.ProductLookupRepository
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException
import java.io.IOException
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductLookupRepositoryImpl @Inject constructor(
    private val api: OpenFoodFactsApi,
    private val appLanguage: AppLanguage,
) : ProductLookupRepository {

    override suspend fun lookup(barcode: String): Result<ProductInfo?> {
        val code = barcode.filter { it.isDigit() }
        if (code.length !in MIN_BARCODE_LENGTH..MAX_BARCODE_LENGTH) return Result.success(null)
        return try {
            val language = appLanguage.language.value
            val response = api.getProduct(code, OpenFoodFactsApi.fieldsFor(language))
            val product = response.product.takeIf { response.status == 1 }
            Result.success(product?.toProductInfo(code, language))
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            if (e.code() == HTTP_NOT_FOUND) Result.success(null) else Result.failure(ProductLookupException.Service(e))
        } catch (e: IOException) {
            Result.failure(ProductLookupException.Network(e))
        } catch (e: Exception) {
            Result.failure(ProductLookupException.Service(e))
        }
    }

    /** Prefers the name in the user's language, then the product's main name. */
    private fun JsonObject.toProductInfo(code: String, language: String): ProductInfo? {
        val name = sequenceOf("product_name_$language", "product_name", "generic_name_$language", "generic_name")
            .map { key -> (get(key) as? JsonPrimitive)?.contentOrNull }
            .mapNotNull { it?.replace(WHITESPACE, " ")?.trim()?.takeIf(String::isNotEmpty) }
            .firstOrNull()
            ?.take(MAX_NAME_LENGTH)
            ?: return null
        val tags = runCatching { getValue("categories_tags").jsonArray.mapNotNull { it.jsonPrimitive.contentOrNull } }
            .getOrDefault(emptyList())
        return ProductInfo(barcode = code, name = name, category = categoryFromTags(tags))
    }

    internal companion object {
        const val HTTP_NOT_FOUND = 404
        const val MIN_BARCODE_LENGTH = 8
        const val MAX_BARCODE_LENGTH = 14
        const val MAX_NAME_LENGTH = 60
        private val WHITESPACE = Regex("\\s+")

        /** Within one tag, earlier rules win (frozen fish is FROZEN for its shelf life). */
        private val CATEGORY_RULES: List<Pair<Category, List<String>>> = listOf(
            Category.FROZEN to listOf("frozen-foods", "frozen"),
            Category.CANNED to listOf("canned-foods", "canned"),
            Category.SEAFOOD to listOf("fishes", "seafood"),
            Category.MEAT to listOf("meats", "poultry", "sausages", "hams", "charcuterie"),
            Category.DAIRY to listOf("dairies", "milks", "cheeses", "yogurts", "butters", "eggs", "creams"),
            Category.BAKERY to listOf("breads", "pastries", "cakes", "viennoiseries"),
            Category.BEVERAGES to listOf("beverages", "drinks", "waters", "juices", "sodas", "coffees", "teas"),
            Category.CONDIMENTS to listOf("condiments", "sauces", "spices", "oils", "vinegars", "spreads"),
            Category.SNACKS to listOf("snacks", "chocolates", "candies", "confectioneries", "chips", "crisps", "biscuits"),
            Category.FRUITS to listOf("fruits"),
            Category.VEGETABLES to listOf("vegetables", "legumes"),
            Category.GRAINS to listOf("cereals", "pastas", "rices", "flours", "grains"),
        )

        /**
         * Open Food Facts tags go from broad to specific ("en:plant-based-foods-and-beverages",
         * …, "en:fresh-tomatoes"), and broad tags mention unrelated words, so the most specific
         * tag is tried first and only matches a rule by its last word(s): "semi-skimmed-milks"
         * matches "milks", "plant-based-foods-and-beverages" would match "beverages" only if no
         * more specific tag matched before it.
         */
        fun categoryFromTags(tags: List<String>): Category {
            for (tag in tags.asReversed()) {
                val name = tag.substringAfter(':').lowercase(Locale.ROOT)
                val match = CATEGORY_RULES.firstOrNull { (_, keys) ->
                    keys.any { key -> name == key || name.endsWith("-$key") }
                }
                if (match != null) return match.first
            }
            return Category.OTHER
        }
    }
}
