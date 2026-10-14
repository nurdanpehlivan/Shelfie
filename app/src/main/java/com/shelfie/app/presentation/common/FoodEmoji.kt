package com.shelfie.app.presentation.common

import com.shelfie.app.domain.model.Category
import java.util.Locale

/**
 * A specific emoji for a product name ("Tomatoes" → 🍅) instead of its category's (🥕), in the
 * app's five languages; falls back to [Category.emoji].
 *
 * Rules are checked in order, so more specific ones come first ("orange juice" is a juice,
 * "pomme de terre" a potato, not an apple). Keywords of up to three letters must match a whole
 * word, longer ones the start of a word ("tomato" matches "tomatoes"), and keywords with a
 * space anywhere in the name.
 */
fun foodEmoji(name: String, category: Category): String {
    val normalized = name.lowercase(Locale.ROOT)
    val words = normalized.split(NON_LETTER).filter { it.isNotEmpty() }
    return RULES.firstOrNull { (_, keywords) ->
        keywords.any { keyword ->
            when {
                ' ' in keyword -> keyword in normalized
                keyword.length <= 3 -> words.any { it == keyword }
                else -> words.any { it.startsWith(keyword) }
            }
        }
    }?.first ?: category.emoji
}

private val NON_LETTER = Regex("[^\\p{L}]+")

private val RULES: List<Pair<String, List<String>>> = listOf(
    "🧃" to listOf("juice", "meyve suyu", "saft", "zumo", "jugo", "jus"),
    "🥔" to listOf("pomme de terre", "potato", "patates", "kartoffel", "patata"),
    "🍆" to listOf("eggplant", "aubergine", "patlıcan", "berenjena"),
    "🍅" to listOf("tomato", "domates", "tomate"),
    // Before lettuce ("salatalık" starts like "salat") and before milk ("laitue" like "lait").
    "🥒" to listOf("cucumber", "salatalık", "gurke", "pepino", "concombre", "zucchini", "kabak"),
    "🥬" to listOf("spinach", "lettuce", "cabbage", "ıspanak", "marul", "lahana", "spinat", "salat", "kohl", "espinaca", "lechuga", "épinard", "laitue", "kale"),
    "🥣" to listOf("yogurt", "yoghurt", "yoğurt", "joghurt", "yogur", "yaourt"),
    "🧈" to listOf("butter", "tereyağ", "mantequilla", "beurre"),
    "🧀" to listOf("cheese", "peynir", "käse", "queso", "fromage", "parmesan", "mozzarella", "feta"),
    "🥛" to listOf("milk", "süt", "milch", "leche", "lait"),
    "🥚" to listOf("egg", "eggs", "yumurta", "eier", "huevo", "œuf", "oeuf"),
    "🍗" to listOf("chicken", "tavuk", "hähnchen", "huhn", "pollo", "poulet"),
    "🥓" to listOf("bacon", "speck", "pastırma"),
    "🥩" to listOf("beef", "steak", "dana", "kıyma", "rind", "ternera", "boeuf", "bœuf", "mince"),
    "🐟" to listOf("fish", "balık", "fisch", "pescado", "poisson", "salmon", "somon", "lachs", "saumon", "tuna", "ton"),
    "🍞" to listOf("bread", "ekmek", "brot", "pan", "pain", "sourdough", "baguette"),
    "🥐" to listOf("croissant"),
    "🍎" to listOf("apple", "elma", "apfel", "manzana", "pomme"),
    "🍌" to listOf("banana", "muz", "banane", "plátano"),
    "🍋" to listOf("lemon", "limon", "zitrone", "limón", "citron"),
    "🍊" to listOf("orange", "portakal", "naranja", "mandarin"),
    "🍓" to listOf("strawberr", "çilek", "erdbeer", "fresa", "fraise"),
    "🍇" to listOf("grape", "üzüm", "traube", "uva", "raisin"),
    "🍐" to listOf("pear", "armut", "birne", "pera", "poire"),
    "🍑" to listOf("peach", "şeftali", "pfirsich", "melocotón", "pêche"),
    "🥑" to listOf("avocado", "avokado", "aguacate", "avocat"),
    "🥦" to listOf("broccoli", "brokoli", "brokkoli", "brócoli", "brocoli"),
    "🥕" to listOf("carrot", "havuç", "karotte", "möhre", "zanahoria", "carotte"),
    "🧅" to listOf("onion", "soğan", "zwiebel", "cebolla", "oignon"),
    "🧄" to listOf("garlic", "sarımsak", "knoblauch", "ajo", "ail"),
    "🫑" to listOf("bell pepper", "biber", "paprika", "pimiento", "poivron"),
    "🍄" to listOf("mushroom", "mantar", "pilz", "champignon", "seta"),
    "🌽" to listOf("corn", "mısır", "mais", "maíz", "maïs"),
    "🫘" to listOf("bean", "fasulye", "bohne", "frijol", "haricot", "lentil", "mercimek", "linse"),
    "🍚" to listOf("rice", "pirinç", "reis", "arroz", "riz"),
    "🍝" to listOf("spaghetti", "noodle", "makarna", "nudel", "pâtes", "pasta"),
    "🍫" to listOf("chocolate", "çikolata", "schokolade", "chocolat"),
    "🥜" to listOf("peanut", "nut", "fıstık", "nuss", "cacahuete", "noix"),
    "☕" to listOf("coffee", "kahve", "kaffee", "café"),
    "🍵" to listOf("tea", "çay", "tee", "té", "thé"),
    "🍷" to listOf("wine", "şarap", "wein", "vino", "vin"),
    "🍺" to listOf("beer", "bira", "bier", "cerveza", "bière"),
    "🫒" to listOf("olive", "zeytin", "oliva"),
)
