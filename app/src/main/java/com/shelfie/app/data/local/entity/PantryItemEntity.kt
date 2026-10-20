package com.shelfie.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.shelfie.app.domain.model.Category
import com.shelfie.app.domain.model.MeasurementUnit
import com.shelfie.app.domain.model.StorageLocation
import java.time.Instant
import java.time.LocalDate

@Entity(
    tableName = PantryItemEntity.TABLE_NAME,
    indices = [
        Index(value = [PantryItemEntity.COLUMN_EXPIRY_DATE]),
        Index(value = [PantryItemEntity.COLUMN_CATEGORY]),
        Index(value = [PantryItemEntity.COLUMN_IS_CONSUMED, PantryItemEntity.COLUMN_EXPIRY_DATE]),
        Index(value = [PantryItemEntity.COLUMN_SYNC_ID], unique = true),
    ],
)
data class PantryItemEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = COLUMN_ID)
    val id: Long = 0L,

    @ColumnInfo(name = COLUMN_NAME)
    val name: String,

    @ColumnInfo(name = COLUMN_CATEGORY)
    val category: Category,

    @ColumnInfo(name = COLUMN_QUANTITY, defaultValue = "1.0")
    val quantity: Double,

    @ColumnInfo(name = COLUMN_UNIT, defaultValue = "PIECE")
    val unit: MeasurementUnit,

    /** Added in schema 5; older rows default to the fridge. */
    @ColumnInfo(name = COLUMN_LOCATION, defaultValue = "FRIDGE")
    val location: StorageLocation = StorageLocation.FRIDGE,

    /** Added in schema 7. */
    @ColumnInfo(name = COLUMN_EMOJI)
    val emoji: String? = null,

    /** Added in schema 7; see [com.shelfie.app.domain.model.PantryItem.isExpiryEstimated]. */
    @ColumnInfo(name = COLUMN_EXPIRY_ESTIMATED, defaultValue = "0")
    val isExpiryEstimated: Boolean = false,

    @ColumnInfo(name = COLUMN_IMAGE_URI)
    val imageUri: String?,

    /** Stored as epoch day. */
    @ColumnInfo(name = COLUMN_PURCHASE_DATE)
    val purchaseDate: LocalDate,

    /** Stored as epoch day so range queries and ORDER BY are plain integer comparisons. */
    @ColumnInfo(name = COLUMN_EXPIRY_DATE)
    val expiryDate: LocalDate,

    /** The item left the pantry, eaten or thrown away; every "active" query filters on this. */
    @ColumnInfo(name = COLUMN_IS_CONSUMED, defaultValue = "0")
    val isConsumed: Boolean,

    /** Thrown away rather than eaten. Always paired with [isConsumed] = true. */
    @ColumnInfo(name = COLUMN_IS_WASTED, defaultValue = "0")
    val isWasted: Boolean = false,

    /** Stored as epoch milliseconds. */
    @ColumnInfo(name = COLUMN_CREATED_AT)
    val createdAt: Instant,

    /** Stored as epoch milliseconds. */
    @ColumnInfo(name = COLUMN_UPDATED_AT)
    val updatedAt: Instant,

    /** Stable id across devices for sync; the auto-generated [id] is only local. */
    @ColumnInfo(name = COLUMN_SYNC_ID, defaultValue = "''")
    val syncId: String = newSyncId(),
) {
    companion object {
        const val TABLE_NAME = "pantry_items"

        const val COLUMN_ID = "id"
        const val COLUMN_NAME = "name"
        const val COLUMN_CATEGORY = "category"
        const val COLUMN_QUANTITY = "quantity"
        const val COLUMN_UNIT = "unit"
        const val COLUMN_LOCATION = "location"
        const val COLUMN_EMOJI = "emoji"
        const val COLUMN_EXPIRY_ESTIMATED = "expiry_estimated"
        const val COLUMN_IMAGE_URI = "image_uri"
        const val COLUMN_PURCHASE_DATE = "purchase_date"
        const val COLUMN_EXPIRY_DATE = "expiry_date"
        const val COLUMN_IS_CONSUMED = "is_consumed"
        const val COLUMN_IS_WASTED = "is_wasted"
        const val COLUMN_CREATED_AT = "created_at"
        const val COLUMN_UPDATED_AT = "updated_at"
        const val COLUMN_SYNC_ID = "sync_id"
    }
}
