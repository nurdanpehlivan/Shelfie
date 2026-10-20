package com.shelfie.app.data.sync

import com.shelfie.app.data.local.entity.MealPlanEntity
import com.shelfie.app.data.local.entity.PantryItemEntity
import com.shelfie.app.data.local.entity.SavedRecipeEntity
import com.shelfie.app.data.local.entity.ShoppingItemEntity
import com.shelfie.app.domain.model.Category
import com.shelfie.app.domain.model.MealType
import com.shelfie.app.domain.model.MeasurementUnit
import com.shelfie.app.domain.model.StorageLocation
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import java.time.Instant
import java.time.LocalDate

/**
 * Converts synced rows to the JSON records stored on the server and back. The same shapes are
 * read and written by the web panel (backend/src/pages/panel.ts), so field names are part of
 * the API. Local-only fields (row ids, photo URIs) are never sent; decoding keeps the local
 * values from [existing] instead.
 */
object SyncRecordMapper {

    private const val MAX_NAME_LENGTH = 60
    private const val MAX_AMOUNT_LENGTH = 30

    /** One emoji, including skin-tone or ZWJ sequences. */
    private const val MAX_EMOJI_LENGTH = 16

    fun PantryItemEntity.toRecord(): JsonObject = buildJsonObject {
        put("name", name)
        put("category", category.name)
        put("quantity", quantity)
        put("unit", unit.name)
        put("location", location.name)
        emoji?.let { put("emoji", it) }
        put("expiryEstimated", isExpiryEstimated)
        put("purchaseDate", purchaseDate.toString())
        put("expiryDate", expiryDate.toString())
        put("isConsumed", isConsumed)
        put("isWasted", isWasted)
        put("createdAt", createdAt.toEpochMilli())
        put("updatedAt", updatedAt.toEpochMilli())
    }

    /** `null` if the record is missing a field the app can't do without. */
    fun pantryFromRecord(syncId: String, data: JsonObject, existing: PantryItemEntity?): PantryItemEntity? {
        val name = data.string("name")?.trim()?.take(MAX_NAME_LENGTH)?.takeIf { it.isNotEmpty() } ?: return null
        val expiry = data.date("expiryDate") ?: return null
        val createdAt = data.instant("createdAt") ?: existing?.createdAt ?: Instant.EPOCH
        val isConsumed = data.boolean("isConsumed") ?: false
        return PantryItemEntity(
            id = existing?.id ?: 0L,
            name = name,
            category = Category.fromName(data.string("category")),
            quantity = data.double("quantity")?.takeIf { it > 0 } ?: 1.0,
            unit = MeasurementUnit.fromName(data.string("unit")),
            // Records written before locations existed (or by the web panel) keep what we have.
            location = StorageLocation.fromNameOrNull(data.string("location"))
                ?: existing?.location
                ?: Category.fromName(data.string("category")).defaultLocation,
            emoji = data.string("emoji")?.take(MAX_EMOJI_LENGTH)?.takeIf { it.isNotBlank() } ?: existing?.emoji,
            isExpiryEstimated = data.boolean("expiryEstimated") ?: existing?.isExpiryEstimated ?: false,
            imageUri = existing?.imageUri,
            purchaseDate = data.date("purchaseDate") ?: existing?.purchaseDate ?: expiry,
            expiryDate = expiry,
            isConsumed = isConsumed,
            isWasted = isConsumed && (data.boolean("isWasted") ?: false),
            createdAt = createdAt,
            updatedAt = data.instant("updatedAt") ?: createdAt,
            syncId = syncId,
        )
    }

    fun MealPlanEntity.toRecord(): JsonObject = buildJsonObject {
        put("date", date.toString())
        put("mealType", mealType.name)
        recipeSyncId?.let { put("recipeSyncId", it) }
        put("note", note)
        put("updatedAt", updatedAt.toEpochMilli())
    }

    fun mealPlanFromRecord(syncId: String, data: JsonObject): MealPlanEntity? {
        val date = data.date("date") ?: return null
        val mealType = MealType.entries.firstOrNull { it.name == data.string("mealType") } ?: return null
        return MealPlanEntity(
            syncId = syncId,
            date = date,
            mealType = mealType,
            recipeSyncId = data.string("recipeSyncId")?.takeIf { it.isNotBlank() },
            note = data.string("note")?.trim()?.take(MAX_NAME_LENGTH).orEmpty(),
            updatedAt = data.instant("updatedAt") ?: Instant.EPOCH,
        )
    }

    fun ShoppingItemEntity.toRecord(): JsonObject = buildJsonObject {
        put("name", name)
        put("amount", amount)
        put("isChecked", isChecked)
        put("createdAt", createdAt.toEpochMilli())
    }

    fun shoppingFromRecord(syncId: String, data: JsonObject, existing: ShoppingItemEntity?): ShoppingItemEntity? {
        val name = data.string("name")?.trim()?.take(MAX_NAME_LENGTH)?.takeIf { it.isNotEmpty() } ?: return null
        return ShoppingItemEntity(
            id = existing?.id ?: 0L,
            name = name,
            amount = data.string("amount")?.trim()?.take(MAX_AMOUNT_LENGTH).orEmpty(),
            isChecked = data.boolean("isChecked") ?: false,
            createdAt = data.instant("createdAt") ?: existing?.createdAt ?: Instant.EPOCH,
            syncId = syncId,
        )
    }

    /** [pantrySyncIds] are the sync ids of the recipe's pantry items (see [SavedRecipeEntity.pantryItemIds]). */
    fun SavedRecipeEntity.toRecord(pantrySyncIds: List<String>): JsonObject = buildJsonObject {
        put("title", title)
        put("recipeJson", recipeJson)
        put("pantryItemIds", JsonArray(pantrySyncIds.map(::JsonPrimitive)))
        put("isFavorite", isFavorite)
        put("createdAt", createdAt.toEpochMilli())
        put("cookedAt", cookedAt?.toEpochMilli()?.let(::JsonPrimitive) ?: JsonNull)
    }

    fun recipePantrySyncIds(data: JsonObject): List<String> =
        (data["pantryItemIds"] as? JsonArray).orEmpty().mapNotNull { (it as? JsonPrimitive)?.contentOrNull }

    /** [pantryLocalIds] are the local ids of the pantry items named by [recipePantrySyncIds]. */
    fun recipeFromRecord(
        syncId: String,
        data: JsonObject,
        existing: SavedRecipeEntity?,
        pantryLocalIds: List<Long>,
    ): SavedRecipeEntity? {
        val recipeJson = data.string("recipeJson")?.takeIf { it.isNotBlank() } ?: return null
        return SavedRecipeEntity(
            id = existing?.id ?: 0L,
            title = data.string("title")?.trim().orEmpty(),
            recipeJson = recipeJson,
            pantryItemIds = SavedRecipeEntity.encodeIds(pantryLocalIds),
            isFavorite = data.boolean("isFavorite") ?: false,
            createdAt = data.instant("createdAt") ?: existing?.createdAt ?: Instant.EPOCH,
            cookedAt = data.instant("cookedAt"),
            syncId = syncId,
        )
    }

    private fun JsonObject.primitive(key: String): JsonPrimitive? = (get(key) as? JsonPrimitive)?.takeUnless { it is JsonNull }

    private fun JsonObject.string(key: String): String? = primitive(key)?.contentOrNull

    private fun JsonObject.boolean(key: String): Boolean? = primitive(key)?.booleanOrNull

    private fun JsonObject.double(key: String): Double? = primitive(key)?.doubleOrNull

    private fun JsonObject.instant(key: String): Instant? = primitive(key)?.longOrNull?.let(Instant::ofEpochMilli)

    private fun JsonObject.date(key: String): LocalDate? =
        string(key)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
}
