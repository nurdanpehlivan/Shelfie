package com.shelfie.app.data.local

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase
import com.shelfie.app.data.local.converter.ShelfieTypeConverters
import com.shelfie.app.data.local.dao.MealPlanDao
import com.shelfie.app.data.local.dao.PantryDao
import com.shelfie.app.data.local.dao.SavedRecipeDao
import com.shelfie.app.data.local.dao.ShoppingItemDao
import com.shelfie.app.data.local.dao.SyncDao
import com.shelfie.app.data.local.entity.MealPlanEntity
import com.shelfie.app.data.local.entity.PantryItemEntity
import com.shelfie.app.data.local.entity.SavedRecipeEntity
import com.shelfie.app.data.local.entity.ShoppingItemEntity
import com.shelfie.app.data.local.entity.SyncApplyingEntity
import com.shelfie.app.data.local.entity.SyncOutboxEntity

@Database(
    entities = [
        PantryItemEntity::class,
        MealPlanEntity::class,
        SavedRecipeEntity::class,
        ShoppingItemEntity::class,
        SyncOutboxEntity::class,
        SyncApplyingEntity::class,
    ],
    version = ShelfieDatabase.VERSION,
    exportSchema = true,
    autoMigrations = [
        // 1 -> 2: adds pantry_items.is_wasted (defaults to 0).
        AutoMigration(from = 1, to = 2),
        // 2 -> 3: adds the saved_recipes and shopping_items tables.
        AutoMigration(from = 2, to = 3),
        // 3 -> 4 is manual (MIGRATION_3_4): it fills sync ids and creates triggers.
        // 4 -> 5: adds pantry_items.location (defaults to FRIDGE); the sync triggers survive
        // because SQLite's ADD COLUMN keeps the table.
        AutoMigration(from = 4, to = 5),
        // 5 -> 6: adds the meal_plan table; the spec creates its sync triggers.
        AutoMigration(from = 5, to = 6, spec = ShelfieDatabase.MealPlanMigration::class),
        // 6 -> 7: adds pantry_items.emoji and .expiry_estimated (ADD COLUMN keeps the triggers).
        AutoMigration(from = 6, to = 7),
    ],
)
@TypeConverters(ShelfieTypeConverters::class)
abstract class ShelfieDatabase : RoomDatabase() {

    abstract fun pantryDao(): PantryDao

    abstract fun savedRecipeDao(): SavedRecipeDao

    abstract fun shoppingItemDao(): ShoppingItemDao

    abstract fun syncDao(): SyncDao

    abstract fun mealPlanDao(): MealPlanDao

    /** Room makes the table; triggers aren't Room's, so they're added here (idempotent). */
    class MealPlanMigration : AutoMigrationSpec {
        override fun onPostMigrate(db: SupportSQLiteDatabase) = SyncTriggers.create(db)
    }

    companion object {
        const val NAME = "shelfie.db"
        const val VERSION = 7
    }
}
