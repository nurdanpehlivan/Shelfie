package com.shelfie.app.presentation.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shelfie.app.domain.model.PantryStatistics
import com.shelfie.app.domain.model.StatisticsPeriod
import com.shelfie.app.domain.repository.SettingsRepository
import com.shelfie.app.domain.usecase.ObserveStatisticsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StatisticsUiState(
    val period: StatisticsPeriod = StatisticsPeriod.MONTH,
    /** `null` while loading. */
    val statistics: PantryStatistics? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StatisticsViewModel @Inject constructor(
    observeStatistics: ObserveStatisticsUseCase,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val period = MutableStateFlow(StatisticsPeriod.MONTH)

    val uiState: StateFlow<StatisticsUiState> = period
        .flatMapLatest { selected ->
            observeStatistics(selected).map { StatisticsUiState(period = selected, statistics = it) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatisticsUiState())

    fun selectPeriod(value: StatisticsPeriod) {
        period.value = value
    }

    /** From the breakdown sheet: what an average product costs the user; every amount follows it. */
    fun setAverageItemValue(value: Double) {
        viewModelScope.launch { settingsRepository.setAverageItemValue(value) }
    }
}
