package com.shelfie.app.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.shelfie.app.data.local.dao.PantryDao
import com.shelfie.app.data.local.dao.SavedRecipeDao
import com.shelfie.app.data.local.dao.ShoppingItemDao
import com.shelfie.app.data.local.entity.PantryItemEntity
import com.shelfie.app.data.local.entity.SavedRecipeEntity
import com.shelfie.app.data.local.entity.ShoppingItemEntity
import com.shelfie.app.domain.model.Category
import com.shelfie.app.domain.model.MeasurementUnit
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import javax.inject.Inject

/**
 * Debug builds only: fills the app with realistic demo data for UI work and store screenshots.
 *
 *     adb shell am broadcast -n com.nurdanpehlivan.shelfie/com.shelfie.app.debug.SeedDemoDataReceiver
 *
 * Add `--es lang tr` for Turkish names and recipes (Turkish store screenshots).
 *
 * Guarded by the DUMP permission in the debug manifest, so only adb (shell) can trigger it.
 * Existing data is wiped first.
 */
@AndroidEntryPoint
class SeedDemoDataReceiver : BroadcastReceiver() {

    @Inject lateinit var pantryDao: PantryDao
    @Inject lateinit var savedRecipeDao: SavedRecipeDao
    @Inject lateinit var shoppingItemDao: ShoppingItemDao
    @Inject lateinit var clock: Clock

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                seed(turkish = intent.getStringExtra("lang") == "tr")
                Log.i(TAG, "Demo data seeded")
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun seed(turkish: Boolean) {
        fun t(english: String): String = if (turkish) TURKISH[english] ?: english else english

        pantryDao.deleteAll()
        savedRecipeDao.deleteAll()
        shoppingItemDao.deleteAll()
        val today = LocalDate.now(clock)
        val now = clock.instant()

        fun item(
            name: String,
            category: Category,
            daysLeft: Long,
            quantity: Double = 1.0,
            unit: MeasurementUnit = MeasurementUnit.PIECE,
            removedDaysAgo: Long? = null,
            wasted: Boolean = false,
        ) = PantryItemEntity(
            name = t(name),
            category = category,
            quantity = quantity,
            unit = unit,
            imageUri = null,
            purchaseDate = today.minusDays(4),
            expiryDate = today.plusDays(daysLeft),
            isConsumed = removedDaysAgo != null,
            isWasted = wasted,
            createdAt = now.minus(Duration.ofDays(5)),
            updatedAt = removedDaysAgo?.let { now.minus(Duration.ofDays(it)) } ?: now,
        )

        val active = listOf(
            item("Milk", Category.DAIRY, daysLeft = 1, quantity = 1.0, unit = MeasurementUnit.LITER),
            item("Tomatoes", Category.VEGETABLES, daysLeft = 0, quantity = 4.0),
            item("Chicken breast", Category.MEAT, daysLeft = 2, quantity = 500.0, unit = MeasurementUnit.GRAM),
            item("Greek yogurt", Category.DAIRY, daysLeft = 4),
            item("Spinach", Category.VEGETABLES, daysLeft = 3, quantity = 1.0, unit = MeasurementUnit.PACKAGE),
            item("Sourdough bread", Category.BAKERY, daysLeft = 5),
            item("Apples", Category.FRUITS, daysLeft = 9, quantity = 6.0),
            item("Rice", Category.GRAINS, daysLeft = 160, quantity = 1.0, unit = MeasurementUnit.KILOGRAM),
            item("Orange juice", Category.BEVERAGES, daysLeft = 21, quantity = 1.0, unit = MeasurementUnit.LITER),
        )
        val ids = pantryDao.insertAll(active)

        // Eight weeks of history so the statistics chart has a shape: mostly eaten, some waste.
        val history = (0 until 8).flatMap { week ->
            val base = week * 7L + 1
            buildList {
                repeat(2 + (week % 3)) { i ->
                    val (name, category) = EATEN[(week * 3 + i) % EATEN.size]
                    add(item(name, category, 0, removedDaysAgo = base + i))
                }
                if (week % 2 == 0) add(item("Lettuce", Category.VEGETABLES, 0, removedDaysAgo = base, wasted = true))
                if (week == 1 || week == 4) add(item("Cream", Category.DAIRY, 0, removedDaysAgo = base + 2, wasted = true))
            }
        }
        pantryDao.insertAll(history)

        savedRecipeDao.insert(
            SavedRecipeEntity(
                title = t("Creamy tomato & spinach scramble"),
                recipeJson = if (turkish) SCRAMBLE_JSON_TR else SCRAMBLE_JSON,
                pantryItemIds = SavedRecipeEntity.encodeIds(listOf(ids[0], ids[1])),
                isFavorite = true,
                createdAt = now.minus(Duration.ofDays(2)),
                cookedAt = now.minus(Duration.ofDays(2)),
            ),
        )
        savedRecipeDao.insert(
            SavedRecipeEntity(
                title = t("15-minute chicken & tomato skillet"),
                recipeJson = if (turkish) SKILLET_JSON_TR else SKILLET_JSON,
                pantryItemIds = SavedRecipeEntity.encodeIds(listOf(ids[1], ids[2])),
                createdAt = now.minus(Duration.ofHours(3)),
            ),
        )

        shoppingItemDao.insertAll(
            listOf("Eggs" to "6 pcs", "Parmesan" to "100 g", "Olive oil" to "", "Garlic" to "1 bulb")
                .map { (name, amount) -> ShoppingItemEntity(name = t(name), amount = t(amount), isChecked = false, createdAt = now) } +
                ShoppingItemEntity(name = t("Butter"), amount = "", isChecked = true, createdAt = now),
        )
    }

    private companion object {
        const val TAG = "SeedDemoData"

        val EATEN = listOf(
            "Bananas" to Category.FRUITS, "Cheddar" to Category.DAIRY, "Carrots" to Category.VEGETABLES,
            "Salmon" to Category.SEAFOOD, "Strawberries" to Category.FRUITS, "Ham" to Category.MEAT,
            "Mushrooms" to Category.VEGETABLES, "Croissants" to Category.BAKERY,
        )

        /** Demo names in Turkish; anything missing stays English. */
        val TURKISH = mapOf(
            "Milk" to "Süt", "Tomatoes" to "Domates", "Chicken breast" to "Tavuk göğsü", "Greek yogurt" to "Süzme yoğurt",
            "Spinach" to "Ispanak", "Sourdough bread" to "Ekşi mayalı ekmek", "Apples" to "Elma", "Rice" to "Pirinç",
            "Orange juice" to "Portakal suyu", "Lettuce" to "Marul", "Cream" to "Krema", "Bananas" to "Muz",
            "Cheddar" to "Kaşar peyniri", "Carrots" to "Havuç", "Salmon" to "Somon", "Strawberries" to "Çilek",
            "Ham" to "Hindi füme", "Mushrooms" to "Mantar", "Croissants" to "Kruvasan", "Eggs" to "Yumurta",
            "Parmesan" to "Parmesan", "Olive oil" to "Zeytinyağı", "Garlic" to "Sarımsak", "Butter" to "Tereyağı",
            "6 pcs" to "6 adet", "1 bulb" to "1 baş",
            "Creamy tomato & spinach scramble" to "Ispanaklı menemen",
            "15-minute chicken & tomato skillet" to "15 dakikada domatesli tavuk sote",
        )

        val SCRAMBLE_JSON_TR = """
            {"title":"Ispanaklı menemen","cooking_time_minutes":10,"servings":2,
             "ingredients":[
               {"name":"Domates","amount":"2 adet","emoji":"🍅","from_pantry":true},
               {"name":"Süt","amount":"50 ml","emoji":"🥛","from_pantry":true},
               {"name":"Yumurta","amount":"4 adet","emoji":"🥚","from_pantry":false},
               {"name":"Tereyağı","amount":"1 yemek kaşığı","emoji":"🧈","from_pantry":false},
               {"name":"Tuz ve karabiber","amount":"bir tutam","emoji":"🧂","from_pantry":false}],
             "steps":[
               {"instruction":"Domatesleri küp küp doğra, yumurtaları sütle ve bir tutam tuzla çırp.","duration_minutes":0},
               {"instruction":"Tereyağını tavada orta ateşte erit, domatesleri 3 dakika yumuşat.","duration_minutes":3},
               {"instruction":"Yumurtaları ekle, kıvamlanana kadar yavaşça karıştır.","duration_minutes":2},
               {"instruction":"Karabiber serp, sıcak servis et.","duration_minutes":0}]}
        """.trimIndent()

        val SKILLET_JSON_TR = """
            {"title":"15 dakikada domatesli tavuk sote","cooking_time_minutes":15,"servings":2,
             "ingredients":[
               {"name":"Tavuk göğsü","amount":"300 g","emoji":"🍗","from_pantry":true},
               {"name":"Domates","amount":"2 adet","emoji":"🍅","from_pantry":true},
               {"name":"Sarımsak","amount":"2 diş","emoji":"🧄","from_pantry":false},
               {"name":"Zeytinyağı","amount":"1 yemek kaşığı","emoji":"🫒","from_pantry":false}],
             "steps":[
               {"instruction":"Tavuğu ince şeritler halinde doğra, tuz ve karabiberle tatlandır.","duration_minutes":0},
               {"instruction":"Zeytinyağında 5 dakika, bir kez çevirerek kızart.","duration_minutes":5},
               {"instruction":"Dilimlenmiş sarımsak ve doğranmış domatesi ekle, tavuk pişene kadar çevir.","duration_minutes":6},
               {"instruction":"Tadına bak, tuzunu ayarla ve servis et.","duration_minutes":0}]}
        """.trimIndent()

        val SCRAMBLE_JSON = """
            {"title":"Creamy tomato & spinach scramble","cooking_time_minutes":10,"servings":2,
             "ingredients":[
               {"name":"Milk","amount":"50 ml","emoji":"🥛","from_pantry":true},
               {"name":"Tomatoes","amount":"2","emoji":"🍅","from_pantry":true},
               {"name":"Eggs","amount":"4","emoji":"🥚","from_pantry":false},
               {"name":"Butter","amount":"1 tbsp","emoji":"🧈","from_pantry":false},
               {"name":"Salt & pepper","amount":"a pinch","emoji":"🧂","from_pantry":false}],
             "steps":[
               {"instruction":"Dice the tomatoes and whisk the eggs with the milk and a pinch of salt.","duration_minutes":0},
               {"instruction":"Melt the butter in a pan over medium heat and soften the tomatoes for 3 minutes.","duration_minutes":3},
               {"instruction":"Pour in the eggs and stir slowly until just set and creamy.","duration_minutes":2},
               {"instruction":"Season with pepper and serve straight away.","duration_minutes":0}]}
        """.trimIndent()

        val SKILLET_JSON = """
            {"title":"15-minute chicken & tomato skillet","cooking_time_minutes":15,"servings":2,
             "ingredients":[
               {"name":"Chicken breast","amount":"300 g","emoji":"🍗","from_pantry":true},
               {"name":"Tomatoes","amount":"2","emoji":"🍅","from_pantry":true},
               {"name":"Garlic","amount":"2 cloves","emoji":"🧄","from_pantry":false},
               {"name":"Olive oil","amount":"1 tbsp","emoji":"🫒","from_pantry":false}],
             "steps":[
               {"instruction":"Slice the chicken thinly and season with salt and pepper.","duration_minutes":0},
               {"instruction":"Brown the chicken in olive oil for 5 minutes, turning once.","duration_minutes":5},
               {"instruction":"Add sliced garlic and chopped tomatoes and simmer until the chicken is cooked through.","duration_minutes":6},
               {"instruction":"Taste, adjust the seasoning and serve.","duration_minutes":0}]}
        """.trimIndent()
    }
}
