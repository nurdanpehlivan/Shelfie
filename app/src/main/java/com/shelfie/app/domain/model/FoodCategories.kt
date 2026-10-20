package com.shelfie.app.domain.model

/**
 * The likely [Category] of a product from its name, in the app's five languages. Used to
 * pre-fill a manual entry and to repair items an older scan filed under the wrong category.
 * Ordered like the emoji rules: specific names ("ton balığı konservesi", "fıstık ezmesi") come
 * before the words they contain.
 */
object FoodCategories {

    fun guess(name: String): Category? =
        RULES.firstOrNull { (_, keywords) -> FoodNames.matches(name, keywords) }?.first

    private val RULES: List<Pair<Category, List<String>>> = listOf(
        // Before nuts and meat: "hindistan cevizi" starts like "hindi" and contains "ceviz".
        Category.FRUITS to listOf("hindistan cevizi", "coconut"),
        Category.FROZEN to listOf("dondurma", "ice cream", "frozen", "donuk", "dondurulmuş", "tiefkühl", "congelad", "surgelé"),
        Category.CANNED to listOf("konserve", "canned", "salça", "tomato paste", "dose", "lata", "conserve"),
        Category.CONDIMENTS to listOf(
            "ketçap", "ketchup", "mayonez", "mayonnaise", "mayo", "hardal", "mustard", "senf", "mostaza", "moutarde",
            "sos", "sosu", "sauce", "soße", "salsa", "pesto", "reçel", "jam", "marmelat", "marmalade", "konfitüre",
            "mermelada", "confiture", "bal", "honey", "honig", "miel", "pekmez", "tahin", "tahini", "fıstık ezmesi",
            "peanut butter", "tuz", "salt", "salz", "şeker", "sugar", "zucker", "azúcar", "sucre", "baharat", "spice",
            "karabiber", "pul biber", "sirke", "vinegar", "essig", "vinagre", "vinaigre", "yağ", "oil", "öl", "aceite",
            "huile", "zeytinyağı", "olive oil",
        ),
        Category.BEVERAGES to listOf(
            "kola", "cola", "gazoz", "soda", "limonata", "lemonade", "maden suyu", "mineral water", "meyve suyu",
            "suyu", "juice", "saft", "zumo", "jus", "su", "water", "wasser", "agua", "eau", "kahve", "coffee", "kaffee",
            "café", "çay", "tea", "tee", "té", "thé", "bira", "beer", "bier", "şarap", "wine", "wein", "vino", "vin",
        ),
        Category.SNACKS to listOf(
            "cips", "chips", "crisps", "bisküvi", "biscuit", "kurabiye", "cookie", "keks", "galleta", "kraker", "cracker",
            "gofret", "wafer", "çikolata", "chocolate", "schokolade", "chocolat", "fıstık", "fındık", "badem", "ceviz",
            "peanut", "nut", "almond", "walnut", "nuss", "cacahuete", "noix", "kuruyemiş",
        ),
        Category.SEAFOOD to listOf(
            "balık", "somon", "levrek", "çupra", "hamsi", "uskumru", "karides", "kalamar", "midye", "fish", "salmon",
            "tuna", "shrimp", "prawn", "squid", "mussel", "fisch", "lachs", "pescado", "poisson", "saumon",
        ),
        Category.MEAT to listOf(
            "kıyma", "tavuk", "hindi", "dana", "kuzu", "et", "sucuk", "salam", "sosis", "jambon", "pastırma", "köfte",
            "chicken", "turkey", "beef", "lamb", "steak", "mince", "sausage", "salami", "ham", "bacon", "hähnchen",
            "rind", "wurst", "pollo", "ternera", "poulet", "boeuf", "bœuf",
        ),
        Category.DAIRY to listOf(
            "süt", "yoğurt", "ayran", "kefir", "peynir", "kaşar", "lor", "tereyağ", "krema", "kaymak", "yumurta", "milk",
            "yogurt", "yoghurt", "cheese", "butter", "cream", "egg", "eggs", "milch", "joghurt", "käse", "eier", "leche",
            "queso", "huevo", "lait", "fromage", "beurre", "œuf", "oeuf",
        ),
        Category.BAKERY to listOf(
            "ekmek", "simit", "poğaça", "açma", "lavaş", "pide", "yufka", "börek", "kek", "bread", "bagel", "croissant",
            "baguette", "tortilla", "cake", "brot", "kuchen", "pan", "pain", "gâteau",
        ),
        Category.GRAINS to listOf(
            "mısır gevreği", "cereal", "corn flakes", "müsli", "muesli", "granola", "yulaf", "oat", "un", "flour", "mehl",
            "harina", "farine", "pirinç", "rice", "reis", "arroz", "riz", "bulgur", "irmik", "makarna", "erişte", "spaghetti",
            "pasta", "noodle", "nudel", "pâtes", "mercimek", "nohut", "kuru fasulye", "lentil", "chickpea", "linse",
        ),
        Category.FRUITS to listOf(
            "elma", "armut", "muz", "portakal", "mandalina", "limon", "greyfurt", "çilek", "kiraz", "vişne", "üzüm", "şeftali",
            "kayısı", "erik", "kivi", "ananas", "mango", "karpuz", "kavun", "nar", "incir", "avokado", "hindistan cevizi",
            "apple", "pear", "banana", "orange", "mandarin", "lemon", "lime", "strawberr", "cherr", "grape", "peach", "apricot",
            "plum", "kiwi", "pineapple", "watermelon", "melon", "avocado", "apfel", "birne", "banane", "erdbeer", "manzana",
            "plátano", "naranja", "fresa", "pomme", "poire", "fraise",
        ),
        Category.VEGETABLES to listOf(
            "domates", "salatalık", "biber", "patlıcan", "kabak", "havuç", "patates", "soğan", "sarımsak", "marul", "roka",
            "ıspanak", "maydanoz", "dereotu", "nane", "lahana", "brokoli", "karnabahar", "mantar", "bezelye", "taze fasulye",
            "pırasa", "kereviz", "turp", "tomato", "cucumber", "pepper", "eggplant", "zucchini", "carrot", "potato", "onion",
            "garlic", "lettuce", "spinach", "parsley", "cabbage", "broccoli", "cauliflower", "mushroom", "peas", "leek",
            "tomate", "gurke", "karotte", "kartoffel", "zwiebel", "patata", "cebolla", "zanahoria", "carotte", "oignon",
        ),
    )
}
