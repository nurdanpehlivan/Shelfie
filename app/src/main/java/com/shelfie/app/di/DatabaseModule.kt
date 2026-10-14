package com.shelfie.app.di

import android.content.Context
import androidx.room.Room
import com.shelfie.app.data.local.ShelfieDatabase
import com.shelfie.app.data.local.dao.PantryDao
import com.shelfie.app.data.local.dao.SavedRecipeDao
import com.shelfie.app.data.local.dao.ShoppingItemDao
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
            .build()

    @Provides
    fun providePantryDao(database: ShelfieDatabase): PantryDao = database.pantryDao()

    @Provides
    fun provideSavedRecipeDao(database: ShelfieDatabase): SavedRecipeDao = database.savedRecipeDao()

    @Provides
    fun provideShoppingItemDao(database: ShelfieDatabase): ShoppingItemDao = database.shoppingItemDao()
}
