package com.shelfie.app.presentation.recipe.picker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shelfie.app.domain.model.PantryItem
import com.shelfie.app.domain.repository.PantryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

data class IngredientPickerUiState(
    val isLoading: Boolean = true,
    /** Active items that are still safe to cook, soonest expiry first. */
    val items: List<PantryItem> = emptyList(),
    val selectedIds: Set<Long> = emptySet(),
) {
    val canPickMore: Boolean get() = selectedIds.size < MAX_PICKED_INGREDIENTS
}

/**
 * More than five and a quick recipe stops being realistic; the rescue recipe itself caps at
 * three, but a hand-picked set may include pantry staples.
 */
const val MAX_PICKED_INGREDIENTS = 5

/** "Cook with what I have": pick any pantry items and get a recipe for exactly those. */
@HiltViewModel
class IngredientPickerViewModel @Inject constructor(
    pantryRepository: PantryRepository,
    private val clock: Clock,
) : ViewModel() {

    private val selectedIds = MutableStateFlow<Set<Long>>(emptySet())

    val uiState: StateFlow<IngredientPickerUiState> = combine(
        pantryRepository.observePantryItems(),
        selectedIds,
    ) { items, selected ->
        val today = LocalDate.now(clock)
        // Expired food is never offered for cooking, as with rescue recipes.
        val cookable = items.filterNot { it.isExpired(today) }
        IngredientPickerUiState(
            isLoading = false,
            items = cookable,
            selectedIds = selected.intersect(cookable.map { it.id }.toSet()),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), IngredientPickerUiState())

    fun toggle(itemId: Long) {
        selectedIds.update { selected ->
            when {
                itemId in selected -> selected - itemId
                selected.size >= MAX_PICKED_INGREDIENTS -> selected
                else -> selected + itemId
            }
        }
    }
}
