package com.shelfie.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PremiumOfferTest {

    private fun offer(period: String, micros: Long, currency: String = "TRY") =
        PremiumOffer("plan", "", period, hasIntroOffer = false, offerToken = "t", priceMicros = micros, currencyCode = currency)

    @Test
    fun `yearly savings compare with twelve months`() {
        assertEquals(40, yearlySavingsPercent(offer("P1M", 69_990_000), offer("P1Y", 499_990_000)))
    }

    @Test
    fun `no savings across currencies or when yearly costs more`() {
        assertNull(yearlySavingsPercent(offer("P1M", 3_990_000, "USD"), offer("P1Y", 499_990_000)))
        assertNull(yearlySavingsPercent(offer("P1M", 1_000_000), offer("P1Y", 13_000_000)))
    }
}
