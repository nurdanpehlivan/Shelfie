package com.shelfie.app.domain.model

import java.time.Instant

data class ShoppingItem(
    val id: Long = NEW_ID,
    val name: String,
    /** Free-form, e.g. "2 pcs" or "200 g"; may be empty. */
    val amount: String = "",
    val isChecked: Boolean = false,
    val createdAt: Instant,
) {
    companion object {
        const val NEW_ID = 0L
    }
}
