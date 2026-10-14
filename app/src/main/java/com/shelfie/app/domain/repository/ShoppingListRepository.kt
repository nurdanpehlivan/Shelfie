package com.shelfie.app.domain.repository

import com.shelfie.app.domain.model.ShoppingItem
import kotlinx.coroutines.flow.Flow

interface ShoppingListRepository {

    /** Unchecked items first, newest first within each group. */
    fun observeItems(): Flow<List<ShoppingItem>>

    /**
     * Adds items by name, skipping names already on the list and not yet bought (case
     * insensitive). Returns how many were actually added.
     */
    suspend fun addItems(items: List<Pair<String, String>>): Int

    suspend fun setChecked(id: Long, isChecked: Boolean)

    suspend fun delete(id: Long)

    /** Removes everything already bought; returns how many were removed. */
    suspend fun clearChecked(): Int
}
