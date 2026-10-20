package com.shelfie.app.presentation.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shelfie.app.domain.model.Category
import com.shelfie.app.domain.model.FoodCategories
import com.shelfie.app.domain.model.ShoppingItem
import com.shelfie.app.domain.repository.PantryRepository
import com.shelfie.app.domain.repository.ShoppingListRepository
import com.shelfie.app.domain.usecase.StockUpFromShoppingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Duration
import javax.inject.Inject

/** Items to buy under one aisle, so the list follows a walk through the shop. */
data class Aisle(val category: Category, val items: List<ShoppingItem>)

data class ShoppingListUiState(
    val isLoading: Boolean = true,
    val toBuy: List<ShoppingItem> = emptyList(),
    val aisles: List<Aisle> = emptyList(),
    val bought: List<ShoppingItem> = emptyList(),
    /** Names of products that recently ran out (eaten or thrown away) and aren't listed yet. */
    val suggestions: List<String> = emptyList(),
)

@HiltViewModel
class ShoppingListViewModel @Inject constructor(
    private val shoppingListRepository: ShoppingListRepository,
    private val stockUp: StockUpFromShoppingUseCase,
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
            aisles = toBuy
                .groupBy { FoodCategories.guess(it.name) ?: Category.OTHER }
                .map { (category, aisleItems) -> Aisle(category, aisleItems) }
                .sortedBy { AISLE_ORDER.indexOf(it.category) },
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

    private val _effects = Channel<ShoppingEffect>(Channel.BUFFERED)
    val effects: Flow<ShoppingEffect> = _effects.receiveAsFlow()

    fun setChecked(item: ShoppingItem, isChecked: Boolean) {
        viewModelScope.launch {
            shoppingListRepository.setChecked(item.id, isChecked)
            if (isChecked) _effects.send(ShoppingEffect.Bought(item))
        }
    }

    /** One item straight from the list into the pantry (the "Bought" snackbar's action). */
    fun stockUp(item: ShoppingItem) {
        viewModelScope.launch { _effects.send(ShoppingEffect.StockedUp(stockUp(listOf(item)))) }
    }

    fun stockUpBought() {
        val bought = uiState.value.bought
        viewModelScope.launch { _effects.send(ShoppingEffect.StockedUp(stockUp(bought))) }
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

        /** Roughly the way through a Turkish supermarket: produce first, frozen last. */
        val AISLE_ORDER = listOf(
            Category.FRUITS,
            Category.VEGETABLES,
            Category.BAKERY,
            Category.DAIRY,
            Category.MEAT,
            Category.SEAFOOD,
            Category.GRAINS,
            Category.CANNED,
            Category.CONDIMENTS,
            Category.SNACKS,
            Category.BEVERAGES,
            Category.FROZEN,
            Category.OTHER,
        )
    }
}

sealed interface ShoppingEffect {
    /** Ticked off; the UI offers to put it in the pantry. */
    data class Bought(val item: ShoppingItem) : ShoppingEffect

    data class StockedUp(val count: Int) : ShoppingEffect
}
