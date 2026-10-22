package com.shelfie.app.domain.model

import java.time.DateTimeException
import java.time.LocalDate
import java.time.YearMonth

/**
 * Finds the expiry date in text read off a package ("SKT: 12.05.2027", "TETT 05/27",
 * "EXP 2027-05-12", "MHD 12.05.27").
 *
 * Packages often print a production date too, so dates on a line with an expiry keyword win;
 * otherwise the latest plausible date does (expiry comes after production). A month-only date
 * ("05/2027") means the end of that month. Anything more than a year in the past or ten years
 * ahead is a misread, not a date.
 */
object ExpiryDateParser {

    fun parse(text: String, today: LocalDate): LocalDate? {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val candidates = lines.flatMapIndexed { index, line ->
            // The keyword may sit on its own line right above the date.
            val labelled = EXPIRY_KEYWORD.containsMatchIn(line) ||
                (index > 0 && EXPIRY_KEYWORD.containsMatchIn(lines[index - 1]) && !DATE_HINT.containsMatchIn(lines[index - 1]))
            val produced = PRODUCTION_KEYWORD.containsMatchIn(line) && !EXPIRY_KEYWORD.containsMatchIn(line)
            datesIn(line).map { Candidate(it, labelled, produced) }
        }.filter { it.date >= today.minusYears(1) && it.date <= today.plusYears(10) }

        return candidates.filter { it.labelled }.maxOfOrNull { it.date }
            ?: candidates.filterNot { it.produced }.maxOfOrNull { it.date }
    }

    private data class Candidate(val date: LocalDate, val labelled: Boolean, val produced: Boolean)

    private fun datesIn(line: String): List<LocalDate> = buildList {
        // OCR reads "O" for "0" and spaces around separators; tidy the digits first.
        val clean = line.replace(Regex("\\s*([./-])\\s*"), "$1")
            .replace(Regex("(?<=\\d)[oO](?=\\d)|(?<=[./-])[oO]|[oO](?=[./-]\\d)"), "0")
        ISO.findAll(clean).forEach { m -> date(m.groupValues[1], m.groupValues[2], m.groupValues[3])?.let(::add) }
        DAY_MONTH_YEAR.findAll(clean).forEach { m -> date(m.groupValues[3], m.groupValues[2], m.groupValues[1])?.let(::add) }
        if (isEmpty()) {
            MONTH_YEAR.findAll(clean).forEach { m ->
                val year = fullYear(m.groupValues[2]) ?: return@forEach
                val month = m.groupValues[1].toIntOrNull() ?: return@forEach
                runCatching { YearMonth.of(year, month).atEndOfMonth() }.getOrNull()?.let(::add)
            }
        }
    }

    private fun date(year: String, month: String, day: String): LocalDate? {
        val y = fullYear(year) ?: return null
        return try {
            LocalDate.of(y, month.toInt(), day.toInt())
        } catch (_: DateTimeException) {
            null
        } catch (_: NumberFormatException) {
            null
        }
    }

    private fun fullYear(year: String): Int? = when (year.length) {
        2 -> year.toIntOrNull()?.plus(2000)
        4 -> year.toIntOrNull()
        else -> null
    }

    private val ISO = Regex("""(?<!\d)(\d{4})[./-](\d{1,2})[./-](\d{1,2})(?!\d)""")
    private val DAY_MONTH_YEAR = Regex("""(?<!\d)(\d{1,2})[./-](\d{1,2})[./-](\d{4}|\d{2})(?!\d)""")
    private val MONTH_YEAR = Regex("""(?<!\d)(\d{1,2})[./-](\d{4}|\d{2})(?!\d)""")
    private val DATE_HINT = Regex("""\d{1,4}[./-]\d{1,2}""")

    /** Turkish, English, German, Spanish and French labels for "use by" / "best before". */
    private val EXPIRY_KEYWORD = Regex(
        """(?i)\b(s\.?\s?k\.?\s?t|son kullanma|t\.?\s?e\.?\s?t\.?\s?t|tavsiye edilen|exp|expiry|expires|use by|best before|bb|bbe|mhd|mindestens haltbar|verbrauchen bis|cad|caducidad|consumir antes|consommer avant|dluo|dlc|à consommer)""",
    )

    private val PRODUCTION_KEYWORD = Regex("""(?i)\b(ü\.?\s?t|üretim|prod|production|produced|mfg|manufactured|herstell|fabricación|fabrication)""")
}
