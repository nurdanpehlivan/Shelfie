package com.shelfie.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class ExpiryDateParserTest {

    private val today = LocalDate.of(2026, 10, 20)

    private fun parse(text: String) = ExpiryDateParser.parse(text, today)

    @Test
    fun `reads common label formats`() {
        assertEquals(LocalDate.of(2027, 5, 12), parse("SKT: 12.05.2027"))
        assertEquals(LocalDate.of(2027, 5, 12), parse("EXP 2027-05-12"))
        assertEquals(LocalDate.of(2027, 5, 12), parse("MHD 12/05/27"))
        assertEquals(LocalDate.of(2026, 11, 3), parse("Son Kullanma Tarihi 3.11.2026"))
    }

    @Test
    fun `month-only dates mean the end of the month`() {
        assertEquals(LocalDate.of(2027, 5, 31), parse("TETT 05/2027"))
        assertEquals(LocalDate.of(2027, 2, 28), parse("Best before 02.27"))
    }

    @Test
    fun `the expiry wins over the production date`() {
        val label = "ÜT: 01.10.2026\nSKT: 01.04.2027\nParti No: 4521"
        assertEquals(LocalDate.of(2027, 4, 1), parse(label))
        // A keyword on its own line labels the date below it.
        assertEquals(LocalDate.of(2027, 1, 15), parse("Üretim 10.10.2026\nTETT\n15.01.2027"))
    }

    @Test
    fun `without keywords the latest plausible date wins`() {
        assertEquals(LocalDate.of(2027, 3, 9), parse("10.10.2026  09.03.2027"))
    }

    @Test
    fun `tolerates OCR noise`() {
        assertEquals(LocalDate.of(2027, 5, 1), parse("SKT 01 . O5 . 2027"))
    }

    @Test
    fun `ignores numbers that are not plausible dates`() {
        assertNull(parse("Net 500 g  Lot 1234-56"))
        assertNull(parse("SKT 31.02.2027"))
        assertNull(parse("EXP 12.05.2052"))
        assertNull(parse(""))
    }
}
