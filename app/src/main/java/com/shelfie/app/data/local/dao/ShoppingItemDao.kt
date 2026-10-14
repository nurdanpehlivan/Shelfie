package com.shelfie.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.shelfie.app.data.local.entity.ShoppingItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingItemDao {

    @Query("SELECT * FROM shopping_items ORDER BY is_checked ASC, created_at DESC")
    fun observeAll(): Flow<List<ShoppingItemEntity>>

    /** Lower-cased names of items still to buy. */
    @Query("SELECT LOWER(name) FROM shopping_items WHERE is_checked = 0")
    suspend fun getUncheckedNamesLowercase(): List<String>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(items: List<ShoppingItemEntity>): List<Long>

    @Query("UPDATE shopping_items SET is_checked = :isChecked WHERE id = :id")
    suspend fun setChecked(id: Long, isChecked: Boolean): Int

    @Query("DELETE FROM shopping_items WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("DELETE FROM shopping_items WHERE is_checked = 1")
    suspend fun deleteChecked(): Int

    @Query("DELETE FROM shopping_items")
    suspend fun deleteAll()
}
