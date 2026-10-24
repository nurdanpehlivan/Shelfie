package com.shelfie.app.data.repository

import com.shelfie.app.data.local.dao.ShoppingItemDao
import com.shelfie.app.data.local.entity.ShoppingItemEntity
import com.shelfie.app.data.translation.ItemNameTranslator
import com.shelfie.app.domain.model.ShoppingItem
import com.shelfie.app.domain.repository.ShoppingListRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShoppingListRepositoryImpl @Inject constructor(
    private val dao: ShoppingItemDao,
    private val clock: Clock,
    private val translator: ItemNameTranslator,
) : ShoppingListRepository {

    override fun observeItems(): Flow<List<ShoppingItem>> =
        translator.translate(
            dao.observeAll().map { rows -> rows.map { it.toDomain() } },
            ShoppingItem::name,
        ) { item, name -> item.copy(name = name) }

    override suspend fun addItems(items: List<Pair<String, String>>): Int {
        val existing = dao.getUncheckedNamesLowercase().toMutableSet()
        val now = clock.instant()
        val toInsert = items.mapNotNull { (rawName, rawAmount) ->
            val name = rawName.trim().take(MAX_NAME_LENGTH)
            // `add` returns false for a name already on the list (or earlier in this batch).
            if (name.isEmpty() || !existing.add(name.lowercase())) return@mapNotNull null
            ShoppingItemEntity(
                name = name,
                amount = rawAmount.trim().take(MAX_AMOUNT_LENGTH),
                isChecked = false,
                createdAt = now,
            )
        }
        if (toInsert.isEmpty()) return 0
        translator.rememberAsCurrentLanguage(toInsert.map { it.name })
        return dao.insertAll(toInsert).size
    }

    override suspend fun setChecked(id: Long, isChecked: Boolean) {
        dao.setChecked(id, isChecked)
    }

    override suspend fun delete(id: Long) {
        dao.deleteById(id)
    }

    override suspend fun clearChecked(): Int = dao.deleteChecked()

    private fun ShoppingItemEntity.toDomain() = ShoppingItem(
        id = id,
        name = name,
        amount = amount,
        isChecked = isChecked,
        createdAt = createdAt,
    )

    private companion object {
        const val MAX_NAME_LENGTH = 60
        const val MAX_AMOUNT_LENGTH = 30
    }
}
