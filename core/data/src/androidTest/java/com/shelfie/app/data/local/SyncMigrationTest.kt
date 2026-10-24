package com.shelfie.app.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SyncMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), ShelfieDatabase::class.java)

    @Test
    fun migrate3To4_givesExistingRowsUniqueSyncIds_andQueuesLaterChanges() {
        helper.createDatabase(DB, 3).use { db ->
            db.execSQL(
                "INSERT INTO pantry_items (name, category, quantity, unit, purchase_date, expiry_date, is_consumed, is_wasted, created_at, updated_at) " +
                    "VALUES ('Milk', 'DAIRY', 1, 'PIECE', 20000, 20005, 0, 0, 1, 1), ('Eggs', 'DAIRY', 6, 'PIECE', 20000, 20010, 0, 0, 1, 1)",
            )
            db.execSQL("INSERT INTO shopping_items (name, amount, is_checked, created_at) VALUES ('Bread', '', 0, 1)")
        }

        // Validates the migrated schema against the exported 4.json.
        val db = helper.runMigrationsAndValidate(DB, 4, true, MIGRATION_3_4)

        val ids = db.query("SELECT sync_id FROM pantry_items").use { c ->
            buildList { while (c.moveToNext()) add(c.getString(0)) }
        }
        assertEquals(2, ids.toSet().size)
        assertTrue(ids.all { it.matches(Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) })
        // Existing rows are queued on sign-in, not by the migration.
        assertEquals(0, db.count("SELECT COUNT(*) FROM sync_outbox"))

        db.execSQL("UPDATE pantry_items SET quantity = 2 WHERE name = 'Milk'")
        db.execSQL("DELETE FROM shopping_items")
        assertEquals(2, db.count("SELECT COUNT(*) FROM sync_outbox"))
        assertEquals(1, db.count("SELECT COUNT(*) FROM sync_outbox WHERE collection = 'shopping'"))

        // Writes made by the sync itself are not queued.
        db.execSQL("DELETE FROM sync_outbox")
        db.execSQL("INSERT INTO sync_applying (id) VALUES (0)")
        db.execSQL("UPDATE pantry_items SET quantity = 3")
        db.execSQL("DELETE FROM sync_applying")
        assertEquals(0, db.count("SELECT COUNT(*) FROM sync_outbox"))
        db.close()
    }

    @Test
    fun migrate3To5_defaultsLocationToFridge_andKeepsSyncTriggers() {
        helper.createDatabase(DB, 3).use { db ->
            db.execSQL(
                "INSERT INTO pantry_items (name, category, quantity, unit, purchase_date, expiry_date, is_consumed, is_wasted, created_at, updated_at) " +
                    "VALUES ('Milk', 'DAIRY', 1, 'PIECE', 20000, 20005, 0, 0, 1, 1)",
            )
        }

        // 4 -> 5 is an auto-migration; validates against the exported 5.json.
        val db = helper.runMigrationsAndValidate(DB, 5, true, MIGRATION_3_4)

        assertEquals(1, db.count("SELECT COUNT(*) FROM pantry_items WHERE location = 'FRIDGE'"))
        // ADD COLUMN must not drop the triggers MIGRATION_3_4 created.
        db.execSQL("UPDATE pantry_items SET location = 'FREEZER'")
        assertEquals(1, db.count("SELECT COUNT(*) FROM sync_outbox"))
        db.close()
    }

    @Test
    fun migrate3To6_addsMealPlanWithSyncTriggers() {
        helper.createDatabase(DB, 3).close()

        val db = helper.runMigrationsAndValidate(DB, 6, true, MIGRATION_3_4)

        db.execSQL(
            "INSERT INTO meal_plan (sync_id, date, meal_type, recipe_sync_id, note, updated_at) " +
                "VALUES ('plan-2026-10-20-dinner', 20381, 'DINNER', NULL, 'Pizza', 1)",
        )
        assertEquals(1, db.count("SELECT COUNT(*) FROM sync_outbox WHERE collection = 'mealplan'"))
        db.close()
    }

    @Test
    fun migrate3To7_addsEmojiAndEstimatedExpiry() {
        helper.createDatabase(DB, 3).use { db ->
            db.execSQL(
                "INSERT INTO pantry_items (name, category, quantity, unit, purchase_date, expiry_date, is_consumed, is_wasted, created_at, updated_at) " +
                    "VALUES ('Milk', 'DAIRY', 1, 'PIECE', 20000, 20005, 0, 0, 1, 1)",
            )
        }

        val db = helper.runMigrationsAndValidate(DB, 7, true, MIGRATION_3_4)

        assertEquals(1, db.count("SELECT COUNT(*) FROM pantry_items WHERE emoji IS NULL AND expiry_estimated = 0"))
        db.close()
    }

    @Test
    fun migrate7To8_letsAQueuedRowBeUpdatedAgain() {
        helper.createDatabase(DB, 3).use { db ->
            db.execSQL(
                "INSERT INTO pantry_items (name, category, quantity, unit, purchase_date, expiry_date, is_consumed, is_wasted, created_at, updated_at) " +
                    "VALUES ('Milk', 'DAIRY', 1, 'PIECE', 20000, 20005, 0, 0, 1, 1)",
            )
        }

        val db = helper.runMigrationsAndValidate(DB, 8, true, MIGRATION_3_4, MIGRATION_7_8)

        // Room's @Update is UPDATE OR ABORT; the second one used to hit the outbox key.
        db.execSQL("UPDATE OR ABORT pantry_items SET quantity = 2")
        db.execSQL("UPDATE OR ABORT pantry_items SET location = 'FREEZER'")
        assertEquals(1, db.count("SELECT COUNT(*) FROM sync_outbox WHERE collection = 'pantry'"))
        db.close()
    }

    @Test
    fun migrate8To10_addsTheTranslationCachesWithoutSyncingThem() {
        helper.createDatabase(DB, 3).close()

        val db = helper.runMigrationsAndValidate(DB, 10, true, MIGRATION_3_4, MIGRATION_7_8)

        db.execSQL("INSERT INTO name_translations (source, language, translated) VALUES ('Süt', 'en', 'Milk')")
        db.execSQL("INSERT INTO recipe_translations (recipe_id, language, recipe_json) VALUES (1, 'en', '{}')")
        assertEquals(1, db.count("SELECT COUNT(*) FROM name_translations"))
        assertEquals(1, db.count("SELECT COUNT(*) FROM recipe_translations"))
        // A local cache: nothing for the server.
        assertEquals(0, db.count("SELECT COUNT(*) FROM sync_outbox"))
        db.close()
    }

    private fun androidx.sqlite.db.SupportSQLiteDatabase.count(sql: String): Int =
        query(sql).use { it.moveToFirst(); it.getInt(0) }

    private companion object {
        const val DB = "migration-test"
    }
}
