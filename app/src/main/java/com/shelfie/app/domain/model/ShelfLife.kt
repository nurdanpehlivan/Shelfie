package com.shelfie.app.domain.model

/**
 * How long food keeps at home, for items without a readable date (fresh produce, deli
 * counter, home-made). Figures are conservative home-storage times in the spirit of the USDA
 * FoodKeeper tables: [Rule.fridge] in the fridge, [Rule.pantry] at room temperature (`null` if
 * it shouldn't be kept there). The freezer always uses [Category.freezerShelfLifeDays].
 *
 * Packaged goods print their own date, which wins over anything here; this is the fallback,
 * and also caps what the AI guesses from a photo so a lettuce never gets three weeks.
 */
object ShelfLife {

    private data class Rule(val keywords: List<String>, val fridge: Int?, val pantry: Int?)

    /** Days from today for [name] kept in [location]; falls back to the category's default. */
    fun estimateDays(name: String, category: Category, location: StorageLocation): Int {
        if (location == StorageLocation.FREEZER) return category.freezerShelfLifeDays
        return knownDays(name, location) ?: category.defaultShelfLifeDays
    }

    /** `null` when the product isn't in the table. */
    fun knownDays(name: String, location: StorageLocation): Int? {
        if (location == StorageLocation.FREEZER) return null
        val rule = RULES.firstOrNull { FoodNames.matches(name, it.keywords) } ?: return null
        return when (location) {
            StorageLocation.PANTRY -> rule.pantry ?: rule.fridge?.let { (it / 3).coerceAtLeast(1) }
            else -> rule.fridge ?: rule.pantry
        }
    }

    // Ordered like the emoji rules: specific names before the words they contain.
    private val RULES = listOf(
        // Fruit.
        Rule(listOf("çilek", "strawberr", "ahududu", "raspberr", "böğürtlen", "blackberr", "blueberr", "yaban mersini", "erdbeer", "fresa", "fraise", "frambo"), fridge = 3, pantry = 1),
        Rule(listOf("kiraz", "vişne", "cherr", "kirsche", "cereza", "cerise"), fridge = 5, pantry = 1),
        Rule(listOf("muz", "banana", "banane", "plátano"), fridge = 7, pantry = 5),
        Rule(listOf("avokado", "avocado", "aguacate", "avocat"), fridge = 5, pantry = 4),
        Rule(listOf("elma", "apple", "apfel", "manzana", "pomme"), fridge = 30, pantry = 7),
        Rule(listOf("portakal", "mandalina", "limon", "greyfurt", "orange", "mandarin", "lemon", "lime", "grapefruit", "zitrone", "naranja", "limón", "citron"), fridge = 21, pantry = 7),
        Rule(listOf("üzüm", "grape", "traube", "uva"), fridge = 7, pantry = 2),
        Rule(listOf("armut", "şeftali", "erik", "kayısı", "nektarin", "pear", "peach", "plum", "apricot", "nectarine", "birne", "pfirsich", "pera", "melocotón", "poire", "pêche"), fridge = 5, pantry = 3),
        Rule(listOf("kivi", "kiwi"), fridge = 14, pantry = 5),
        Rule(listOf("mango"), fridge = 5, pantry = 4),
        Rule(listOf("karpuz", "kavun", "watermelon", "melon", "sandía", "pastèque"), fridge = 10, pantry = 7),
        Rule(listOf("ananas", "pineapple", "piña"), fridge = 5, pantry = 2),
        // Before poultry: "hindistan" starts like "hindi" (turkey).
        Rule(listOf("hindistan cevizi", "coconut", "kokos", "coco"), fridge = 7, pantry = 30),
        // Vegetables and herbs.
        Rule(listOf("domates", "tomato", "tomate"), fridge = 7, pantry = 5),
        Rule(listOf("marul", "roka", "ıspanak", "maydanoz", "dereotu", "nane", "fesleğen", "semizotu", "lettuce", "spinach", "arugula", "rocket", "parsley", "dill", "mint", "basil", "salat", "spinat", "lechuga", "espinaca", "laitue", "épinard"), fridge = 5, pantry = 1),
        Rule(listOf("lahana", "cabbage", "kohl", "repollo", "chou"), fridge = 14, pantry = 3),
        Rule(listOf("havuç", "carrot", "karotte", "möhre", "zanahoria", "carotte"), fridge = 21, pantry = 5),
        Rule(listOf("salatalık", "cucumber", "gurke", "pepino", "concombre"), fridge = 7, pantry = 2),
        // Dried spices, before fresh peppers ("karabiber", "black pepper", "pul biber").
        Rule(listOf("karabiber", "pul biber", "black pepper", "baharat", "spice", "gewürz", "especia", "épice"), fridge = 365, pantry = 365),
        Rule(listOf("biber", "pepper", "paprika", "pimiento", "poivron"), fridge = 7, pantry = 3),
        Rule(listOf("patlıcan", "kabak", "eggplant", "aubergine", "zucchini", "courgette", "berenjena", "calabacín"), fridge = 5, pantry = 2),
        Rule(listOf("brokoli", "karnabahar", "broccoli", "cauliflower", "brokkoli", "blumenkohl", "brócoli", "coliflor", "chou-fleur"), fridge = 5, pantry = 1),
        Rule(listOf("mantar", "mushroom", "pilz", "champignon", "seta"), fridge = 4, pantry = 1),
        Rule(listOf("taze fasulye", "green bean", "bezelye", "peas", "erbse", "guisante", "haricot vert"), fridge = 5, pantry = 1),
        Rule(listOf("mısır koçanı", "corn on the cob", "sweet corn"), fridge = 3, pantry = 1),
        Rule(listOf("pırasa", "kereviz", "leek", "celery", "lauch", "sellerie", "puerro", "apio", "poireau", "céleri"), fridge = 14, pantry = 3),
        // Keep best in a cool, dark cupboard, not the fridge.
        Rule(listOf("patates", "potato", "kartoffel", "patata", "pomme de terre"), fridge = 14, pantry = 30),
        Rule(listOf("soğan", "sarımsak", "onion", "garlic", "zwiebel", "knoblauch", "cebolla", "ajo", "oignon", "ail"), fridge = 14, pantry = 30),
        // Dairy and eggs (once opened; a printed date on a sealed pack wins).
        Rule(listOf("ayran", "kefir"), fridge = 5, pantry = null),
        Rule(listOf("yoğurt", "yogurt", "yoghurt", "joghurt", "yaourt"), fridge = 10, pantry = null),
        Rule(listOf("krema", "kaymak", "cream", "sahne", "nata", "crème"), fridge = 5, pantry = null),
        Rule(listOf("beyaz peynir", "lor", "feta", "ricotta", "mozzarella", "quark", "cottage"), fridge = 7, pantry = null),
        Rule(listOf("peynir", "kaşar", "cheese", "käse", "queso", "fromage", "parmesan", "cheddar"), fridge = 21, pantry = null),
        Rule(listOf("tereyağ", "butter", "mantequilla", "beurre"), fridge = 30, pantry = 2),
        Rule(listOf("süt", "milk", "milch", "leche", "lait"), fridge = 5, pantry = null),
        Rule(listOf("yumurta", "egg", "eggs", "eier", "huevo", "œuf", "oeuf"), fridge = 28, pantry = 14),
        // Meat and fish (fresh, uncooked).
        Rule(listOf("kıyma", "ground beef", "mince", "hackfleisch", "carne picada", "viande hachée"), fridge = 2, pantry = null),
        Rule(listOf("tavuk", "hindi", "chicken", "turkey", "hähnchen", "huhn", "pollo", "pavo", "poulet", "dinde"), fridge = 2, pantry = null),
        Rule(listOf("balık", "somon", "levrek", "çupra", "hamsi", "karides", "fish", "salmon", "shrimp", "prawn", "fisch", "lachs", "pescado", "salmón", "poisson", "saumon"), fridge = 2, pantry = null),
        Rule(listOf("sucuk", "pastırma", "salam", "sosis", "sausage", "salami", "wurst", "salchicha", "saucisse"), fridge = 14, pantry = null),
        Rule(listOf("jambon", "ham", "schinken", "jamón"), fridge = 5, pantry = null),
        Rule(listOf("dana", "kuzu", "et", "beef", "lamb", "steak", "rind", "lamm", "ternera", "cordero", "bœuf", "boeuf", "agneau"), fridge = 3, pantry = null),
        // Bakery and leftovers.
        Rule(listOf("simit", "poğaça", "açma", "börek", "croissant"), fridge = 3, pantry = 2),
        Rule(listOf("ekmek", "lavaş", "pide", "bread", "brot", "pan", "pain", "baguette", "tortilla"), fridge = 5, pantry = 3),
        Rule(listOf("pilav", "artan", "leftover", "cooked rice", "reste"), fridge = 4, pantry = null),
    )
}
