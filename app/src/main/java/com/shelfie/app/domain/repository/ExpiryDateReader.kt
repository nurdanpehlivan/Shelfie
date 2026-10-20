package com.shelfie.app.domain.repository

import java.time.LocalDate

/**
 * Reads the printed expiry date off a photo of a package, on the device: no network, no AI
 * allowance. `null` when no plausible date was found.
 */
interface ExpiryDateReader {
    suspend fun read(imageUri: String, today: LocalDate): LocalDate?
}
