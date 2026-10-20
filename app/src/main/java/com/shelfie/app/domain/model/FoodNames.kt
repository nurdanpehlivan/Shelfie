package com.shelfie.app.domain.model

/**
 * Keyword matching for product names in the app's five languages, shared by the emoji lookup
 * and the shelf-life table.
 *
 * Keywords of up to three letters must match a whole word ("bal" is honey, "balık" is not),
 * longer ones the start of a word ("tomato" matches "tomatoes"), and keywords with a space
 * anywhere in the name ("meyve suyu").
 */
object FoodNames {

    private val NON_LETTER = Regex("[^\\p{L}]+")

    fun matches(name: String, keywords: List<String>): Boolean {
        val normalized = name.lowercase()
        val words = normalized.split(NON_LETTER).filter { it.isNotEmpty() }
        return keywords.any { keyword ->
            when {
                ' ' in keyword -> keyword in normalized
                keyword.length <= 3 -> words.any { it == keyword }
                else -> words.any { it.startsWith(keyword) }
            }
        }
    }
}
