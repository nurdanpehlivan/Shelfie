package com.shelfie.app.data.mapper

import com.shelfie.app.data.local.entity.PantryItemEntity
import com.shelfie.app.domain.model.PantryItem

fun PantryItemEntity.toDomain(): PantryItem = PantryItem(
    id = id,
    name = name,
    category = category,
    quantity = quantity,
    unit = unit,
    location = location,
    emoji = emoji,
    isExpiryEstimated = isExpiryEstimated,
    imageUri = imageUri,
    purchaseDate = purchaseDate,
    expiryDate = expiryDate,
    isConsumed = isConsumed,
    isWasted = isWasted,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun PantryItem.toEntity(): PantryItemEntity = PantryItemEntity(
    id = id,
    name = name.trim(),
    category = category,
    quantity = quantity,
    unit = unit,
    location = location,
    emoji = emoji,
    isExpiryEstimated = isExpiryEstimated,
    imageUri = imageUri,
    purchaseDate = purchaseDate,
    expiryDate = expiryDate,
    isConsumed = isConsumed,
    isWasted = isWasted,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun List<PantryItemEntity>.toDomain(): List<PantryItem> = map(PantryItemEntity::toDomain)
