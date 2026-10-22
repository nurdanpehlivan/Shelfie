package com.shelfie.app.domain.model

import java.time.LocalDate

/*
 * The rules for changing a pantry item, in one place, so the pantry, the scanner review and
 * any later screen apply them the same way.
 */

/**
 * The item after the user edited it. A new name drops the scanned emoji (it showed the old
 * product); a date the user changed is no longer an estimate.
 */
fun PantryItem.edited(
    name: String,
    quantity: Double,
    unit: MeasurementUnit,
    category: Category,
    expiryDate: LocalDate,
    location: StorageLocation,
): PantryItem = copy(
    name = name,
    quantity = quantity,
    unit = unit,
    category = category,
    expiryDate = expiryDate,
    location = location,
    emoji = emoji.takeIf { name.equals(this.name, ignoreCase = true) },
    isExpiryEstimated = isExpiryEstimated && expiryDate == this.expiryDate,
)

/** Part of it was used: half of what's left remains. */
fun PantryItem.halved(): PantryItem = copy(quantity = quantity / 2)

/**
 * Moved to the freezer: it keeps for the category's frozen shelf life from [today]. The date
 * is never shortened; something already good for longer keeps its own expiry.
 */
fun PantryItem.frozen(today: LocalDate): PantryItem {
    val expiry = maxOf(expiryDate, today.plusDays(category.freezerShelfLifeDays.toLong()))
    return copy(
        location = StorageLocation.FREEZER,
        expiryDate = expiry,
        isExpiryEstimated = if (expiry == expiryDate) isExpiryEstimated else true,
    )
}

/** The freeze action is offered: the product freezes well and isn't frozen already. */
val PantryItem.canFreeze: Boolean
    get() = category.isFreezable && location != StorageLocation.FREEZER

/**
 * Taken out of the freezer to thaw in the fridge: from [today] it keeps only for the
 * category's thawed shelf life, never longer than its own expiry.
 */
fun PantryItem.thawed(today: LocalDate): PantryItem {
    val expiry = minOf(expiryDate, today.plusDays(category.thawedShelfLifeDays.toLong()))
    return copy(
        location = StorageLocation.FRIDGE,
        expiryDate = expiry,
        isExpiryEstimated = if (expiry == expiryDate) isExpiryEstimated else true,
    )
}

/**
 * "I opened it" is offered while it would change something: the product has an opened shelf
 * life, isn't frozen, and that shelf life ends before its current date.
 */
fun PantryItem.canMarkOpened(today: LocalDate): Boolean {
    val days = category.openedShelfLifeDays ?: return false
    return location != StorageLocation.FREEZER && today.plusDays(days.toLong()) < expiryDate
}

/**
 * Opened: it keeps for the category's opened shelf life from [today], never longer than its
 * own date, and moves to the fridge when the product needs it once opened.
 */
fun PantryItem.opened(today: LocalDate): PantryItem {
    val days = category.openedShelfLifeDays ?: return this
    val expiry = minOf(expiryDate, today.plusDays(days.toLong()))
    return copy(
        location = if (category.goesInFridgeOnceOpened && location == StorageLocation.PANTRY) StorageLocation.FRIDGE else location,
        expiryDate = expiry,
        isExpiryEstimated = if (expiry == expiryDate) isExpiryEstimated else true,
    )
}

/** Whether [expiryDate] is still the date [ShelfLife] works out for this product. */
fun PantryItem.hasEstimatedExpiry(): Boolean =
    expiryDate == purchaseDate.plusDays(ShelfLife.estimateDays(name, category, location).toLong())
