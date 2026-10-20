package com.shelfie.app.data.repository

import com.shelfie.app.data.local.dao.MealPlanDao
import com.shelfie.app.data.local.dao.SavedRecipeDao
import com.shelfie.app.data.local.entity.MealPlanEntity
import com.shelfie.app.domain.model.MealPlanEntry
import com.shelfie.app.domain.model.MealType
import com.shelfie.app.domain.repository.MealPlanRepository
import com.shelfie.app.domain.repository.RecipeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MealPlanRepositoryImpl @Inject constructor(
    private val mealPlanDao: MealPlanDao,
    private val savedRecipeDao: SavedRecipeDao,
    private val recipeRepository: RecipeRepository,
    private val clock: Clock,
) : MealPlanRepository {

    /** Slots point at recipes by sync id (stable across devices); the UI wants local recipes. */
    override fun observeWeek(start: LocalDate): Flow<List<MealPlanEntry>> = combine(
        mealPlanDao.observeBetween(start, start.plusDays(DAYS_IN_WEEK - 1)),
        savedRecipeDao.observeIdPairs(),
        recipeRepository.observeSavedRecipes(),
    ) { slots, idPairs, recipes ->
        val localIdBySyncId = idPairs.associate { it.syncId to it.id }
        val recipeById = recipes.associateBy { it.id }
        slots.map { slot ->
            MealPlanEntry(
                date = slot.date,
                mealType = slot.mealType,
                recipe = slot.recipeSyncId?.let(localIdBySyncId::get)?.let(recipeById::get),
                note = slot.note,
            )
        }
    }

    override suspend fun setRecipe(date: LocalDate, mealType: MealType, recipeId: Long) {
        val recipeSyncId = savedRecipeDao.getSyncId(recipeId) ?: return
        val current = current(date, mealType)
        mealPlanDao.upsert(slot(date, mealType, recipeSyncId, current?.note.orEmpty()))
    }

    override suspend fun setNote(date: LocalDate, mealType: MealType, note: String) {
        val current = current(date, mealType)
        val trimmed = note.trim().take(MAX_NOTE_LENGTH)
        if (trimmed.isEmpty() && current?.recipeSyncId == null) {
            clear(date, mealType)
        } else {
            mealPlanDao.upsert(slot(date, mealType, current?.recipeSyncId, trimmed))
        }
    }

    override suspend fun clear(date: LocalDate, mealType: MealType) {
        mealPlanDao.delete(MealPlanEntity.syncIdFor(date, mealType))
    }

    private suspend fun current(date: LocalDate, mealType: MealType): MealPlanEntity? =
        mealPlanDao.observeBetween(date, date).first().firstOrNull { it.mealType == mealType }

    private fun slot(date: LocalDate, mealType: MealType, recipeSyncId: String?, note: String) = MealPlanEntity(
        syncId = MealPlanEntity.syncIdFor(date, mealType),
        date = date,
        mealType = mealType,
        recipeSyncId = recipeSyncId,
        note = note,
        updatedAt = clock.instant(),
    )

    private companion object {
        const val DAYS_IN_WEEK = 7L
        const val MAX_NOTE_LENGTH = 80
    }
}
