package com.shelfie.app.di

import com.shelfie.app.data.billing.PlaySubscriptionRepository
import com.shelfie.app.data.ocr.MlKitExpiryDateReader
import com.shelfie.app.data.local.image.LocalPhotoStorage
import com.shelfie.app.data.repository.AccountRepositoryImpl
import com.shelfie.app.data.repository.HouseholdRepositoryImpl
import com.shelfie.app.data.repository.MealPlanRepositoryImpl
import com.shelfie.app.data.repository.PantryRepositoryImpl
import com.shelfie.app.data.repository.ProductLookupRepositoryImpl
import com.shelfie.app.data.repository.RecipeRepositoryImpl
import com.shelfie.app.data.repository.SettingsRepositoryImpl
import com.shelfie.app.data.repository.ShoppingListRepositoryImpl
import com.shelfie.app.domain.repository.AccountRepository
import com.shelfie.app.domain.repository.ExpiryDateReader
import com.shelfie.app.domain.repository.HouseholdRepository
import com.shelfie.app.domain.repository.MealPlanRepository
import com.shelfie.app.domain.repository.PantryRepository
import com.shelfie.app.domain.repository.PhotoStorage
import com.shelfie.app.domain.repository.ProductLookupRepository
import com.shelfie.app.domain.repository.RecipeRepository
import com.shelfie.app.domain.repository.SettingsRepository
import com.shelfie.app.domain.repository.ShoppingListRepository
import com.shelfie.app.domain.repository.SubscriptionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPantryRepository(impl: PantryRepositoryImpl): PantryRepository

    @Binds
    @Singleton
    abstract fun bindPhotoStorage(impl: LocalPhotoStorage): PhotoStorage

    @Binds
    @Singleton
    abstract fun bindRecipeRepository(impl: RecipeRepositoryImpl): RecipeRepository

    @Binds
    @Singleton
    abstract fun bindShoppingListRepository(impl: ShoppingListRepositoryImpl): ShoppingListRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindProductLookupRepository(impl: ProductLookupRepositoryImpl): ProductLookupRepository

    @Binds
    @Singleton
    abstract fun bindAccountRepository(impl: AccountRepositoryImpl): AccountRepository

    @Binds
    @Singleton
    abstract fun bindMealPlanRepository(impl: MealPlanRepositoryImpl): MealPlanRepository

    @Binds
    @Singleton
    abstract fun bindHouseholdRepository(impl: HouseholdRepositoryImpl): HouseholdRepository

    @Binds
    @Singleton
    abstract fun bindSubscriptionRepository(impl: PlaySubscriptionRepository): SubscriptionRepository

    @Binds
    @Singleton
    abstract fun bindExpiryDateReader(impl: MlKitExpiryDateReader): ExpiryDateReader
}
