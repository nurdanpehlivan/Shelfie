package com.shelfie.app.data.sync

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shelfie.app.data.local.ShelfieDatabase
import com.shelfie.app.data.local.SyncTriggers
import com.shelfie.app.data.local.entity.PantryItemEntity
import com.shelfie.app.data.local.entity.SavedRecipeEntity
import com.shelfie.app.data.local.entity.ShoppingItemEntity
import com.shelfie.app.data.remote.shelfie.CredentialsRequest
import com.shelfie.app.data.remote.shelfie.SessionResponse
import com.shelfie.app.data.remote.shelfie.ShelfieApiService
import com.shelfie.app.data.remote.shelfie.SyncChangeDto
import com.shelfie.app.data.remote.shelfie.SyncRequest
import com.shelfie.app.data.remote.shelfie.SyncResponse
import com.shelfie.app.domain.model.Category
import com.shelfie.app.domain.model.MeasurementUnit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.HttpException
import retrofit2.Response
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@RunWith(AndroidJUnit4::class)
class SyncManagerTest {

    private lateinit var database: ShelfieDatabase
    private lateinit var accountStore: AccountStore
    private lateinit var api: FakeApi
    private lateinit var manager: SyncManager
    private lateinit var storeFile: File

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ShelfieDatabase::class.java)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) = SyncTriggers.create(db)
            })
            .allowMainThreadQueries()
            .build()
        storeFile = File(context.cacheDir, "account-test-${System.nanoTime()}.preferences_pb")
        accountStore = AccountStore(PreferenceDataStoreFactory.create(produceFile = { storeFile }))
        api = FakeApi()
        manager = SyncManager(database, database.syncDao(), api, accountStore, Clock.fixed(NOW, ZoneOffset.UTC))
        accountStore.signIn(token = "token", email = "me@example.com")
    }

    @After
    fun tearDown() {
        database.close()
        storeFile.delete()
    }

    @Test
    fun pushesLocalChangesAndClearsTheOutbox() = runTest {
        val id = database.pantryDao().insert(pantryItem("Milk"))
        database.shoppingItemDao().insertAll(listOf(ShoppingItemEntity(name = "Bread", amount = "1", isChecked = false, createdAt = NOW)))

        assertTrue(manager.sync().isSuccess)

        val pushed = api.requests.single().changes
        assertEquals(setOf("pantry", "shopping"), pushed.map { it.collection }.toSet())
        val milk = pushed.first { it.collection == "pantry" }
        assertEquals(database.pantryDao().getItemById(id)!!.syncId, milk.syncId)
        assertEquals("Milk", milk.data!!["name"]!!.jsonPrimitive.content)
        assertEquals(0, database.syncDao().observeOutboxCount().first())
        assertEquals(NOW.toEpochMilli(), accountStore.currentSession()!!.lastSyncAt)
    }

    @Test
    fun pushesDeletionsAsTombstones() = runTest {
        val id = database.pantryDao().insert(pantryItem("Milk"))
        val syncId = database.pantryDao().getItemById(id)!!.syncId
        database.pantryDao().deleteById(id)

        manager.sync()

        val change = api.requests.single().changes.single()
        assertEquals(syncId, change.syncId)
        assertTrue(change.deleted)
        assertNull(change.data)
    }

    @Test
    fun appliesRemoteRecordsWithoutQueueingThemBack() = runTest {
        api.responses += SyncResponse(
            cursor = 7,
            hasMore = false,
            records = listOf(
                remotePantry("remote-pantry-1", "Yogurt"),
                SyncChangeDto(
                    collection = "recipes",
                    syncId = "remote-recipe-1",
                    data = buildJsonObject {
                        put("title", "Yogurt bowl")
                        put("recipeJson", "{}")
                        put("pantryItemIds", JsonArray(listOf(JsonPrimitive("remote-pantry-1"))))
                        put("isFavorite", true)
                        put("createdAt", 5L)
                    },
                    changedAt = 10,
                ),
            ),
        )

        manager.sync()

        val yogurt = database.syncDao().getPantryItem("remote-pantry-1")!!
        assertEquals("Yogurt", yogurt.name)
        assertEquals(Category.DAIRY, yogurt.category)
        val recipe = database.syncDao().getRecipe("remote-recipe-1")!!
        assertEquals(listOf(yogurt.id), SavedRecipeEntity.decodeIds(recipe.pantryItemIds))
        assertTrue(recipe.isFavorite)
        assertEquals(0, database.syncDao().observeOutboxCount().first())
        assertEquals(7L, accountStore.currentSession()!!.cursor)
    }

    @Test
    fun remoteCopyDoesNotOverwriteAPendingLocalChange() = runTest {
        val id = database.pantryDao().insert(pantryItem("Milk"))
        val syncId = database.pantryDao().getItemById(id)!!.syncId
        // Pins the queued change's time, so the edit below can't land in the same millisecond.
        database.openHelper.writableDatabase.execSQL("UPDATE sync_outbox SET changed_at = 1")
        // The server answers with an older copy while this device has a newer, unsent edit.
        api.onSync = {
            database.pantryDao().updateQuantity(id, 5.0, NOW)
            SyncResponse(cursor = 1, hasMore = false, records = listOf(remotePantry(syncId, "Old milk")))
        }

        manager.sync()

        val local = database.pantryDao().getItemById(id)!!
        assertEquals("Milk", local.name)
        assertEquals(5.0, local.quantity, 0.0)
        assertEquals(1, database.syncDao().observeOutboxCount().first())
    }

    @Test
    fun remoteDeletionRemovesTheRow_andKeepsLocalPhoto() = runTest {
        api.responses += SyncResponse(1, false, listOf(remotePantry("p-000001", "Cheese")))
        manager.sync()
        val cheese = database.syncDao().getPantryItem("p-000001")!!
        database.syncDao().setApplying()
        database.pantryDao().update(cheese.copy(imageUri = "content://photo"))
        database.syncDao().clearApplying()

        api.responses += SyncResponse(2, false, listOf(remotePantry("p-000001", "Cheddar")))
        manager.sync()
        assertEquals("content://photo", database.syncDao().getPantryItem("p-000001")!!.imageUri)
        assertEquals("Cheddar", database.syncDao().getPantryItem("p-000001")!!.name)

        api.responses += SyncResponse(3, false, listOf(SyncChangeDto("pantry", "p-000001", null, true, 99)))
        manager.sync()
        assertNull(database.syncDao().getPantryItem("p-000001"))
    }

    @Test
    fun rejectedSessionSignsOutLocally() = runTest {
        api.onSync = { throw HttpException(Response.error<Any>(401, "".toResponseBody())) }

        assertFalse(manager.sync().isSuccess)
        assertNull(accountStore.currentSession())
    }

    private fun pantryItem(name: String) = PantryItemEntity(
        name = name,
        category = Category.DAIRY,
        quantity = 1.0,
        unit = MeasurementUnit.PIECE,
        imageUri = null,
        purchaseDate = LocalDate.of(2026, 10, 18),
        expiryDate = LocalDate.of(2026, 10, 22),
        isConsumed = false,
        createdAt = NOW,
        updatedAt = NOW,
    )

    private fun remotePantry(syncId: String, name: String) = SyncChangeDto(
        collection = "pantry",
        syncId = syncId,
        data = buildJsonObject {
            put("name", name)
            put("category", "DAIRY")
            put("quantity", 1)
            put("unit", "PIECE")
            put("purchaseDate", "2026-10-18")
            put("expiryDate", "2026-10-25")
            put("isConsumed", false)
            put("isWasted", false)
            put("createdAt", 1L)
            put("updatedAt", 2L)
        },
        changedAt = 2,
    )

    private class FakeApi : ShelfieApiService {
        val requests = mutableListOf<SyncRequest>()
        val responses = ArrayDeque<SyncResponse>()
        var onSync: (suspend () -> SyncResponse)? = null

        override suspend fun sync(bearer: String, request: SyncRequest): SyncResponse {
            requests += request
            onSync?.let { return it() }
            return responses.removeFirstOrNull() ?: SyncResponse(request.cursor, false, emptyList())
        }

        override suspend fun register(request: CredentialsRequest): SessionResponse = error("unused")
        override suspend fun login(request: CredentialsRequest): SessionResponse = error("unused")
        override suspend fun logout(bearer: String) = Unit
        override suspend fun deleteAccount(bearer: String) = Unit
    }

    private companion object {
        val NOW: Instant = Instant.parse("2026-10-20T09:00:00Z")
    }
}
