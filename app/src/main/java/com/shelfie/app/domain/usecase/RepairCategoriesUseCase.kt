package com.shelfie.app.domain.usecase

import com.shelfie.app.domain.model.Category
import com.shelfie.app.domain.model.FoodCategories
import com.shelfie.app.domain.repository.PantryRepository
import com.shelfie.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * One-time repair: older scans filed every shelf product (ketchup, jam, coffee, …) under
 * [Category.GRAINS]. Items whose name clearly says otherwise get their real category. Runs once
 * per install, so a category the user picks by hand later is never changed back.
 */
class RepairCategoriesUseCase @Inject constructor(
    private val pantryRepository: PantryRepository,
    private val settingsRepository: SettingsRepository,
) {

    /** Returns how many items were moved. */
    suspend operator fun invoke(): Int {
        if (settingsRepository.isMaintenanceDone(KEY)) return 0
        var repaired = 0
        pantryRepository.observePantryItems().first()
            .filter { it.category == Category.GRAINS }
            .forEach { item ->
                val guess = FoodCategories.guess(item.name)
                if (guess != null && guess != Category.GRAINS) {
                    pantryRepository.updatePantryItem(item.copy(category = guess))
                    repaired++
                }
            }
        settingsRepository.markMaintenanceDone(KEY)
        return repaired
    }

    private companion object {
        const val KEY = "repair_scan_categories_v1"
    }
}
