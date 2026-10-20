package com.shelfie.app.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.shelfie.app.data.local.entity.MealPlanEntity
import com.shelfie.app.data.local.entity.PantryItemEntity
import com.shelfie.app.data.local.entity.SavedRecipeEntity
import com.shelfie.app.data.local.entity.ShoppingItemEntity
import com.shelfie.app.data.local.entity.SyncApplyingEntity
import com.shelfie.app.data.local.entity.SyncCollections
import com.shelfie.app.data.local.entity.SyncOutboxEntity

/**
 * SQLite triggers that queue every insert, update and delete on a synced table into
 * [SyncOutboxEntity], unless the sync itself is writing (a row in [SyncApplyingEntity]).
 *
 * Room doesn't manage triggers, so they are created by [MIGRATION_3_4] for existing installs
 * and by [create] from the database's onCreate callback for new ones.
 */
object SyncTriggers {

    /** (table, collection) of every synced table. */
    val SYNCED_TABLES = listOf(
        PantryItemEntity.TABLE_NAME to SyncCollections.PANTRY,
        ShoppingItemEntity.TABLE_NAME to SyncCollections.SHOPPING,
        SavedRecipeEntity.TABLE_NAME to SyncCollections.RECIPES,
        MealPlanEntity.TABLE_NAME to SyncCollections.MEAL_PLAN,
    )

    private const val NOW_MILLIS = "CAST((julianday('now') - 2440587.5) * 86400000 AS INTEGER)"

    fun create(db: SupportSQLiteDatabase, tables: List<Pair<String, String>> = SYNCED_TABLES) {
        for ((table, collection) in tables) {
            for ((event, row) in listOf("INSERT" to "NEW", "UPDATE" to "NEW", "DELETE" to "OLD")) {
                db.execSQL(
                    """
                    CREATE TRIGGER IF NOT EXISTS sync_${table}_${event.lowercase()}
                    AFTER $event ON $table
                    WHEN NOT EXISTS (SELECT 1 FROM ${SyncApplyingEntity.TABLE_NAME})
                    BEGIN
                        INSERT OR REPLACE INTO ${SyncOutboxEntity.TABLE_NAME} (collection, sync_id, changed_at)
                        VALUES ('$collection', $row.sync_id, $NOW_MILLIS);
                    END
                    """.trimIndent(),
                )
            }
        }
    }
}

/** 3 -> 4: sync ids on the synced tables, the outbox and its triggers. */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Only the tables that existed in schema 4; later ones come with their own migration.
        for ((table, _) in SyncTriggers.SYNCED_TABLES.filter { it.first in SCHEMA_4_SYNCED_TABLES }) {
            db.execSQL("ALTER TABLE `$table` ADD COLUMN `sync_id` TEXT NOT NULL DEFAULT ''")
            // A random UUID-shaped id per existing row.
            db.execSQL(
                """
                UPDATE `$table` SET sync_id = lower(
                    hex(randomblob(4)) || '-' || hex(randomblob(2)) || '-' || hex(randomblob(2)) || '-' ||
                    hex(randomblob(2)) || '-' || hex(randomblob(6)))
                """.trimIndent(),
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_${table}_sync_id` ON `$table` (`sync_id`)")
        }
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `sync_outbox` (
                `collection` TEXT NOT NULL, `sync_id` TEXT NOT NULL, `changed_at` INTEGER NOT NULL,
                PRIMARY KEY(`collection`, `sync_id`))
            """.trimIndent(),
        )
        db.execSQL("CREATE TABLE IF NOT EXISTS `sync_applying` (`id` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        SyncTriggers.create(db, SyncTriggers.SYNCED_TABLES.filter { it.first in SCHEMA_4_SYNCED_TABLES })
    }
}

private val SCHEMA_4_SYNCED_TABLES = setOf(
    PantryItemEntity.TABLE_NAME,
    ShoppingItemEntity.TABLE_NAME,
    SavedRecipeEntity.TABLE_NAME,
)
