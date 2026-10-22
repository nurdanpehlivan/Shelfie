package com.shelfie.app.presentation.common

import com.shelfie.app.domain.model.Category
import com.shelfie.app.domain.model.FoodNames
import com.shelfie.app.domain.model.PantryItem

/**
 * The icon of a pantry item: the emoji the AI picked when it scanned the item, else one found
 * from its name, else its category's.
 */
val PantryItem.displayEmoji: String
    get() = emoji ?: foodEmoji(name, category)

/**
 * A specific emoji for a product name ("Tomatoes" → 🍅) instead of its category's (🥕), in the
 * app's five languages; falls back to [Category.emoji].
 *
 * Rules are checked in order, so more specific ones come first ("orange juice" is a juice,
 * "domates salçası" a sauce, "mısır gevreği" cereal, not corn). See [FoodNames.matches] for how
 * keywords match.
 */
fun foodEmoji(name: String, category: Category): String =
    RULES.firstOrNull { (_, keywords) -> FoodNames.matches(name, keywords) }?.first ?: category.emoji

private val RULES: List<Pair<String, List<String>>> = listOf(
    // Prepared and packaged foods first: their names contain ingredient words.
    "🥣" to listOf("mısır gevreği", "corn flakes", "cornflakes", "cereal", "gevrek", "müsli", "muesli", "granola", "yulaf", "oat", "haferflocken", "avena", "flocons"),
    "🍟" to listOf("patates kızartması", "cips", "chips", "crisps", "fries", "pommes frites"),
    "🌿" to listOf("pesto", "maydanoz", "parsley", "dereotu", "dill", "nane", "mint", "fesleğen", "basil", "kekik", "thyme", "biberiye", "rosemary", "petersilie", "perejil", "persil", "albahaca", "basilic"),
    "🫙" to listOf("reçel", "jam", "marmelat", "marmalade", "konfitüre", "marmelade", "mermelada", "confiture", "tahin", "tahini"),
    "🍯" to listOf("bal", "honey", "honig", "miel", "pekmez"),
    "🥜" to listOf("fıstık ezmesi", "peanut butter", "erdnussbutter", "mantequilla de cacahuete", "beurre de cacahuète"),
    "🥫" to listOf("ketçap", "ketchup", "salça", "tomato paste", "mayonez", "mayonnaise", "mayo", "hardal", "mustard", "senf", "mostaza", "moutarde", "sos", "sosu", "sauce", "soße", "salsa", "konserve", "canned", "dose", "lata", "conserve"),
    "🥤" to listOf("kola", "cola", "gazoz", "soda", "limonata", "lemonade", "limonade", "limonada", "fanta", "sprite", "ice tea", "soğuk çay", "energy drink"),
    // Water before juice, so "maden suyu" isn't a juice but "elma suyu" is.
    "💧" to listOf("maden suyu", "mineral water", "su", "water", "wasser", "agua", "eau"),
    "🧃" to listOf("juice", "meyve suyu", "suyu", "saft", "zumo", "jugo", "jus"),
    "🌶️" to listOf("acı biber", "pul biber", "chili", "jalapeño", "piment"),
    "🥔" to listOf("pomme de terre", "potato", "patates", "kartoffel", "patata"),
    "🍆" to listOf("eggplant", "aubergine", "patlıcan", "berenjena"),
    "🍅" to listOf("tomato", "domates", "tomate"),
    // Before lettuce ("salatalık" starts like "salat") and before milk ("laitue" like "lait").
    "🥒" to listOf("cucumber", "salatalık", "gurke", "pepino", "concombre", "zucchini", "kabak", "courgette", "calabacín"),
    "🥬" to listOf("spinach", "lettuce", "cabbage", "ıspanak", "marul", "lahana", "roka", "arugula", "pırasa", "leek", "spinat", "salat", "kohl", "lauch", "espinaca", "lechuga", "épinard", "laitue", "poireau", "kale"),
    "🥦" to listOf("broccoli", "brokoli", "brokkoli", "brócoli", "brocoli", "karnabahar", "cauliflower", "blumenkohl", "coliflor", "chou-fleur"),
    "🫛" to listOf("bezelye", "peas", "erbse", "guisante", "petit pois", "edamame"),
    "🫚" to listOf("zencefil", "ginger", "ingwer", "jengibre", "gingembre"),
    "🥣" to listOf("yogurt", "yoghurt", "yoğurt", "joghurt", "yogur", "yaourt"),
    "🧈" to listOf("butter", "tereyağ", "margarin", "mantequilla", "beurre"),
    "🧀" to listOf("cheese", "peynir", "kaşar", "lor", "käse", "queso", "fromage", "parmesan", "mozzarella", "feta", "cheddar"),
    "🥛" to listOf("milk", "süt", "ayran", "kefir", "krema", "kaymak", "cream", "milch", "sahne", "leche", "nata", "lait", "crème"),
    "🥚" to listOf("egg", "eggs", "yumurta", "eier", "huevo", "œuf", "oeuf"),
    // Before poultry: "hindistan" starts like "hindi" (turkey).
    "🥥" to listOf("hindistan cevizi", "coconut", "kokos", "coco"),
    "🌭" to listOf("sucuk", "sosis", "sausage", "hot dog", "wurst", "salchicha", "saucisse"),
    "🍗" to listOf("chicken", "tavuk", "hindi", "turkey", "hähnchen", "huhn", "pute", "pollo", "pavo", "poulet", "dinde"),
    "🥓" to listOf("bacon", "speck", "pastırma"),
    "🍖" to listOf("kuzu", "lamb", "köfte", "meatball", "salam", "salami", "jambon", "ham", "schinken", "jamón", "lamm", "cordero", "agneau"),
    "🥩" to listOf("beef", "steak", "dana", "kıyma", "et", "rind", "ternera", "boeuf", "bœuf", "mince"),
    "🦐" to listOf("karides", "shrimp", "prawn", "garnele", "gamba", "crevette"),
    "🦑" to listOf("kalamar", "ahtapot", "squid", "octopus", "calamar", "pulpo", "poulpe"),
    "🐟" to listOf("fish", "balık", "fisch", "pescado", "poisson", "salmon", "somon", "lachs", "saumon", "tuna", "ton", "hamsi", "levrek", "çupra", "uskumru"),
    "🍕" to listOf("pizza"),
    "🍔" to listOf("hamburger", "burger"),
    "🥟" to listOf("mantı", "börek", "dumpling", "maultasche", "empanada"),
    "🥯" to listOf("simit", "bagel"),
    "🥐" to listOf("croissant", "poğaça", "açma"),
    "🫓" to listOf("lavaş", "lavash", "pide", "yufka", "tortilla", "wrap", "flatbread", "fladenbrot"),
    "🍞" to listOf("bread", "ekmek", "brot", "pan", "pain", "sourdough", "baguette", "toast"),
    "🍨" to listOf("dondurma", "ice cream", "eiscreme", "helado", "glace"),
    "🍰" to listOf("kek", "cake", "kuchen", "tarta", "gâteau", "gateau", "tatlı", "baklava"),
    "🍪" to listOf("bisküvi", "kurabiye", "biscuit", "cookie", "keks", "galleta"),
    "🍉" to listOf("karpuz", "watermelon", "wassermelone", "sandía", "pastèque"),
    "🍈" to listOf("kavun", "melon", "honigmelone"),
    "🍒" to listOf("kiraz", "vişne", "cherr", "kirsche", "cereza", "cerise"),
    "🍍" to listOf("ananas", "pineapple", "piña"),
    "🥭" to listOf("mango"),
    "🥝" to listOf("kivi", "kiwi"),
    "🍎" to listOf("apple", "elma", "apfel", "manzana", "pomme"),
    "🍌" to listOf("banana", "muz", "banane", "plátano"),
    "🍋" to listOf("lemon", "limon", "lime", "zitrone", "limón", "citron"),
    "🍊" to listOf("orange", "portakal", "naranja", "mandarin", "mandalina"),
    "🍓" to listOf("strawberr", "çilek", "erdbeer", "fresa", "fraise"),
    "🫐" to listOf("blueberr", "yaban mersini", "heidelbeer", "arándano", "myrtille"),
    "🍇" to listOf("grape", "üzüm", "traube", "uva", "raisin"),
    "🍐" to listOf("pear", "armut", "birne", "pera", "poire"),
    "🍑" to listOf("peach", "şeftali", "kayısı", "apricot", "nektarin", "pfirsich", "melocotón", "pêche"),
    "🥑" to listOf("avocado", "avokado", "aguacate", "avocat"),
    "🥕" to listOf("carrot", "havuç", "karotte", "möhre", "zanahoria", "carotte"),
    "🧅" to listOf("onion", "soğan", "zwiebel", "cebolla", "oignon"),
    "🧄" to listOf("garlic", "sarımsak", "knoblauch", "ajo", "ail"),
    "🧂" to listOf("tuz", "salt", "salz", "sal", "sel", "karabiber", "black pepper", "baharat", "spice", "şeker", "sugar", "zucker", "azúcar", "sucre"),
    "🫑" to listOf("bell pepper", "biber", "paprika", "pimiento", "poivron"),
    "🍄" to listOf("mushroom", "mantar", "pilz", "champignon", "seta"),
    "🌽" to listOf("corn", "mısır", "mais", "maíz", "maïs"),
    "🫘" to listOf("bean", "fasulye", "bohne", "frijol", "haricot", "lentil", "mercimek", "linse", "nohut", "chickpea", "kichererbse", "garbanzo", "pois chiche", "humus", "hummus"),
    "🍚" to listOf("rice", "pirinç", "pilav", "reis", "arroz", "riz"),
    "🍝" to listOf("spaghetti", "noodle", "makarna", "erişte", "nudel", "pâtes", "pasta"),
    "🌾" to listOf("un", "flour", "mehl", "harina", "farine", "bulgur", "irmik", "semolina", "grieß"),
    "🫒" to listOf("olive", "zeytin", "oliva"),
    "🫗" to listOf("sıvı yağ", "ayçiçek yağı", "yağ", "oil", "öl", "aceite", "huile"),
    "🍫" to listOf("chocolate", "çikolata", "schokolade", "chocolat"),
    "🥜" to listOf("peanut", "nut", "fıstık", "fındık", "ceviz", "badem", "almond", "walnut", "nuss", "cacahuete", "noix"),
    "☕" to listOf("coffee", "kahve", "kaffee", "café"),
    "🍵" to listOf("tea", "çay", "tee", "té", "thé"),
    "🍷" to listOf("wine", "şarap", "wein", "vino", "vin"),
    "🍺" to listOf("beer", "bira", "bier", "cerveza", "bière"),
)

/** Used when the model gave no usable emoji and the ingredient isn't a known pantry item. */
const val FALLBACK_INGREDIENT_EMOJI = "🥄"
