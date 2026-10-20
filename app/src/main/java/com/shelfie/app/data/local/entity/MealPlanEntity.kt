package com.shelfie.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.shelfie.app.domain.model.MealType
import java.time.Instant
import java.time.LocalDate

/**
 * One slot of the weekly meal plan: a saved recipe and/or a free note for a day's meal.
 *
 * The sync id is derived from the slot ([syncIdFor]), so two devices planning the same meal
 * write the same record and the server's last-write-wins settles it, instead of two rows
 * fighting over one slot. It is the primary key for the same reason.
 */
@Entity(
    tableName = MealPlanEntity.TABLE_NAME,
    indices = [Index(value = [MealPlanEntity.COLUMN_DATE])],
)
data class MealPlanEntity(
    @PrimaryKey
    @ColumnInfo(name = COLUMN_SYNC_ID)
    val syncId: String,

    /** Stored as epoch day. */
    @ColumnInfo(name = COLUMN_DATE)
    val date: LocalDate,

    @ColumnInfo(name = COLUMN_MEAL_TYPE)
    val mealType: MealType,

    /** Sync id of the planned [SavedRecipeEntity], stable across devices; `null` for a note only. */
    @ColumnInfo(name = COLUMN_RECIPE_SYNC_ID)
    val recipeSyncId: String?,

    @ColumnInfo(name = COLUMN_NOTE, defaultValue = "''")
    val note: String,

    /** Stored as epoch milliseconds. */
    @ColumnInfo(name = COLUMN_UPDATED_AT)
    val updatedAt: Instant,
) {
    companion object {
        const val TABLE_NAME = "meal_plan"

        const val COLUMN_SYNC_ID = "sync_id"
        const val COLUMN_DATE = "date"
        const val COLUMN_MEAL_TYPE = "meal_type"
        const val COLUMN_RECIPE_SYNC_ID = "recipe_sync_id"
        const val COLUMN_NOTE = "note"
        const val COLUMN_UPDATED_AT = "updated_at"

        /** e.g. "plan-2026-10-20-dinner"; matches the server's sync id pattern. */
        fun syncIdFor(date: LocalDate, mealType: MealType): String = "plan-$date-${mealType.name.lowercase()}"
    }
}
