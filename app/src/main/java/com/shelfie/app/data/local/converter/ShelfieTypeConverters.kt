package com.shelfie.app.data.local.converter

import androidx.room.TypeConverter
import com.shelfie.app.domain.model.Category
import com.shelfie.app.domain.model.MeasurementUnit
import java.time.Instant
import java.time.LocalDate

/**
 * Enums are persisted by name (stable across reordering) and decoded leniently so a value
 * removed in a future release does not crash reads of old rows.
 */
class ShelfieTypeConverters {

    @TypeConverter
    fun localDateToEpochDay(date: LocalDate?): Long? = date?.toEpochDay()

    @TypeConverter
    fun epochDayToLocalDate(epochDay: Long?): LocalDate? = epochDay?.let(LocalDate::ofEpochDay)

    @TypeConverter
    fun instantToEpochMilli(instant: Instant?): Long? = instant?.toEpochMilli()

    @TypeConverter
    fun epochMilliToInstant(epochMilli: Long?): Instant? = epochMilli?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun categoryToName(category: Category?): String? = category?.name

    @TypeConverter
    fun nameToCategory(name: String?): Category? = name?.let(Category::fromName)

    @TypeConverter
    fun measurementUnitToName(unit: MeasurementUnit?): String? = unit?.name

    @TypeConverter
    fun nameToMeasurementUnit(name: String?): MeasurementUnit? = name?.let(MeasurementUnit::fromName)
}
