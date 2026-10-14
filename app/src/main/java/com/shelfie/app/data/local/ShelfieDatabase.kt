package com.shelfie.app.data.local

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.shelfie.app.data.local.converter.ShelfieTypeConverters
import com.shelfie.app.data.local.dao.PantryDao
import com.shelfie.app.data.local.dao.SavedRecipeDao
import com.shelfie.app.data.local.dao.ShoppingItemDao
import com.shelfie.app.data.local.entity.PantryItemEntity
import com.shelfie.app.data.local.entity.SavedRecipeEntity
import com.shelfie.app.data.local.entity.ShoppingItemEntity

@Database(
    entities = [PantryItemEntity::class, SavedRecipeEntity::class, ShoppingItemEntity::class],
    version = ShelfieDatabase.VERSION,
    exportSchema = true,
    autoMigrations = [
        // 1 -> 2: adds pantry_items.is_wasted (defaults to 0).
        AutoMigration(from = 1, to = 2),
        // 2 -> 3: adds the saved_recipes and shopping_items tables.
        AutoMigration(from = 2, to = 3),
    ],
)
@TypeConverters(ShelfieTypeConverters::class)
abstract class ShelfieDatabase : RoomDatabase() {

    abstract fun pantryDao(): PantryDao

    abstract fun savedRecipeDao(): SavedRecipeDao

    abstract fun shoppingItemDao(): ShoppingItemDao

    companion object {
        const val NAME = "shelfie.db"
        const val VERSION = 3
    }
}
