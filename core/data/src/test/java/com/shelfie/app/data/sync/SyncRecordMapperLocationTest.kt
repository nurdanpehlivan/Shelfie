package com.shelfie.app.data.sync

import com.shelfie.app.data.local.entity.PantryItemEntity
import com.shelfie.app.data.sync.SyncRecordMapper.toRecord
import com.shelfie.app.domain.model.Category
import com.shelfie.app.domain.model.MeasurementUnit
import com.shelfie.app.domain.model.StorageLocation
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class SyncRecordMapperLocationTest {

    private val entity = PantryItemEntity(
        name = "Chicken",
        category = Category.MEAT,
        quantity = 1.0,
        unit = MeasurementUnit.PACKAGE,
        location = StorageLocation.FREEZER,
        imageUri = null,
        purchaseDate = LocalDate.of(2026, 10, 1),
        expiryDate = LocalDate.of(2027, 1, 1),
        isConsumed = false,
        createdAt = Instant.ofEpochMilli(1),
        updatedAt = Instant.ofEpochMilli(2),
        syncId = "abc",
    )

    @Test
    fun locationRoundTrips() {
        val decoded = SyncRecordMapper.pantryFromRecord("abc", entity.toRecord(), existing = null)

        assertEquals(StorageLocation.FREEZER, decoded?.location)
    }

    @Test
    fun recordWithoutLocation_keepsTheLocalPlace() {
        val record = JsonObject(entity.toRecord() - "location")

        val decoded = SyncRecordMapper.pantryFromRecord("abc", record, existing = entity)

        assertEquals(StorageLocation.FREEZER, decoded?.location)
    }

    @Test
    fun newRecordWithoutLocation_usesTheCategoryDefault() {
        val record = JsonObject(entity.toRecord() - "location" + ("category" to JsonPrimitive("GRAINS")))

        val decoded = SyncRecordMapper.pantryFromRecord("abc", record, existing = null)

        assertEquals(StorageLocation.PANTRY, decoded?.location)
    }
}
