package com.shelfie.app.presentation.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shelfie.app.domain.model.ShoppingItem
import com.shelfie.app.domain.repository.PantryRepository
import com.shelfie.app.domain.repository.ShoppingListRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Duration
import javax.inject.Inject

data class ShoppingListUiState(
    val isLoading: Boolean = true,
    val toBuy: List<ShoppingItem> = emptyList(),
    val bought: List<ShoppingItem> = emptyList(),
    /** Names of products that recently ran out (eaten or thrown away) and aren't listed yet. */
    val suggestions: List<String> = emptyList(),
)

@HiltViewModel
class ShoppingListViewModel @Inject constructor(
    private val shoppingListRepository: ShoppingListRepository,
    pantryRepository: PantryRepository,
    clock: Clock,
) : ViewModel() {

    val uiState: StateFlow<ShoppingListUiState> = combine(
        shoppingListRepository.observeItems(),
        pantryRepository.observeRemovedItemsSince(clock.instant().minus(SUGGESTION_WINDOW)),
        pantryRepository.observePantryItems(),
    ) { items, removed, inPantry ->
        val (bought, toBuy) = items.partition { it.isChecked }
        // Something still in the pantry under the same name hasn't really run out.
        val known = (toBuy.map { it.name } + inPantry.map { it.name }).map { it.lowercase() }.toSet()
        ShoppingListUiState(
            isLoading = false,
            toBuy = toBuy,
            bought = bought,
            suggestions = removed
                .map { it.name.trim() }
                .distinctBy { it.lowercase() }
                .filterNot { it.lowercase() in known }
                .take(MAX_SUGGESTIONS),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ShoppingListUiState())

    fun add(name: String, amount: String = "") {
        if (name.isBlank()) return
        viewModelScope.launch { shoppingListRepository.addItems(listOf(name to amount)) }
    }

    fun setChecked(item: ShoppingItem, isChecked: Boolean) {
        viewModelScope.launch { shoppingListRepository.setChecked(item.id, isChecked) }
    }

    fun delete(item: ShoppingItem) {
        viewModelScope.launch { shoppingListRepository.delete(item.id) }
    }

    fun clearBought() {
        viewModelScope.launch { shoppingListRepository.clearChecked() }
    }

    private companion object {
        val SUGGESTION_WINDOW: Duration = Duration.ofDays(14)
        const val MAX_SUGGESTIONS = 8
    }
}
