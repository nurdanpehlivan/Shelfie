package com.shelfie.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.shelfie.app.data.local.MIGRATION_3_4
import com.shelfie.app.data.local.MIGRATION_7_8
import com.shelfie.app.data.local.ShelfieDatabase
import com.shelfie.app.data.local.SyncTriggers
import com.shelfie.app.data.local.dao.MealPlanDao
import com.shelfie.app.data.local.dao.PantryDao
import com.shelfie.app.data.local.dao.SavedRecipeDao
import com.shelfie.app.data.local.dao.ShoppingItemDao
import com.shelfie.app.data.local.dao.SyncDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideShelfieDatabase(@ApplicationContext context: Context): ShelfieDatabase =
        Room.databaseBuilder(context, ShelfieDatabase::class.java, ShelfieDatabase.NAME)
            .addMigrations(MIGRATION_3_4, MIGRATION_7_8)
            .addCallback(
                object : RoomDatabase.Callback() {
                    // Upgraded databases get their triggers from MIGRATION_3_4.
                    override fun onCreate(db: SupportSQLiteDatabase) = SyncTriggers.create(db)
                },
            )
            .build()

    @Provides
    fun providePantryDao(database: ShelfieDatabase): PantryDao = database.pantryDao()

    @Provides
    fun provideSavedRecipeDao(database: ShelfieDatabase): SavedRecipeDao = database.savedRecipeDao()

    @Provides
    fun provideShoppingItemDao(database: ShelfieDatabase): ShoppingItemDao = database.shoppingItemDao()

    @Provides
    fun provideSyncDao(database: ShelfieDatabase): SyncDao = database.syncDao()

    @Provides
    fun provideMealPlanDao(database: ShelfieDatabase): MealPlanDao = database.mealPlanDao()
}
