package com.shelfie.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.shelfie.app.data.local.entity.MealPlanEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface MealPlanDao {

    /** Slots in the inclusive range [[start], [end]], by day. */
    @Query("SELECT * FROM meal_plan WHERE date BETWEEN :start AND :end ORDER BY date")
    fun observeBetween(start: LocalDate, end: LocalDate): Flow<List<MealPlanEntity>>

    @Upsert
    suspend fun upsert(entry: MealPlanEntity)

    @Query("DELETE FROM meal_plan WHERE sync_id = :syncId")
    suspend fun delete(syncId: String)
}
