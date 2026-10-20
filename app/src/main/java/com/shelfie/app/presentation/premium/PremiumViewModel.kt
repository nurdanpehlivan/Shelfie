package com.shelfie.app.presentation.premium

import android.app.Activity
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shelfie.app.R
import com.shelfie.app.data.billing.PlayBillingManager
import com.shelfie.app.data.billing.PremiumOffer
import com.shelfie.app.data.billing.PurchaseEvent
import com.shelfie.app.domain.model.QuotaStatus
import com.shelfie.app.domain.repository.AccountRepository
import com.shelfie.app.domain.repository.HouseholdRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PremiumUiState(
    val isSignedIn: Boolean = false,
    val quota: QuotaStatus? = null,
    val offers: List<PremiumOffer> = emptyList(),
    val selectedPlanId: String? = null,
    /** Offers were looked up and none is available (no Play Store, or not set up in Play Console). */
    val offersUnavailable: Boolean = false,
    val isWorking: Boolean = false,
    @param:StringRes val messageRes: Int? = null,
) {
    val selectedOffer: PremiumOffer? get() = offers.firstOrNull { it.basePlanId == selectedPlanId } ?: offers.firstOrNull()
}

@HiltViewModel
class PremiumViewModel @Inject constructor(
    private val billing: PlayBillingManager,
    private val householdRepository: HouseholdRepository,
    accountRepository: AccountRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PremiumUiState())
    val uiState: StateFlow<PremiumUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            accountRepository.account.collect { account -> _uiState.update { it.copy(isSignedIn = account != null) } }
        }
        viewModelScope.launch {
            billing.events.collect { event ->
                _uiState.update {
                    it.copy(
                        isWorking = false,
                        messageRes = when (event) {
                            PurchaseEvent.Granted -> R.string.premium_granted
                            PurchaseEvent.Pending -> R.string.premium_pending
                            PurchaseEvent.Cancelled -> null
                            PurchaseEvent.Failed -> R.string.premium_failed
                        },
                    )
                }
                if (event == PurchaseEvent.Granted) refreshQuota()
            }
        }
        refreshQuota()
        viewModelScope.launch {
            val offers = billing.loadOffers()
            // Yearly preselected when there is one: it's the better deal and the usual pick.
            val preselected = offers.lastOrNull()?.basePlanId
            _uiState.update { it.copy(offers = offers, selectedPlanId = preselected, offersUnavailable = offers.isEmpty()) }
        }
    }

    fun select(offer: PremiumOffer) = _uiState.update { it.copy(selectedPlanId = offer.basePlanId) }

    fun buy(activity: Activity) {
        val offer = _uiState.value.selectedOffer ?: return
        _uiState.update { it.copy(isWorking = true, messageRes = null) }
        billing.launch(activity, offer)
    }

    fun restore() {
        _uiState.update { it.copy(isWorking = true, messageRes = null) }
        viewModelScope.launch {
            val granted = billing.restore()
            _uiState.update {
                it.copy(isWorking = false, messageRes = if (granted) R.string.premium_granted else R.string.premium_nothing_to_restore)
            }
            refreshQuota()
        }
    }

    fun messageShown() = _uiState.update { it.copy(messageRes = null) }

    private fun refreshQuota() {
        viewModelScope.launch {
            householdRepository.quota().onSuccess { quota -> _uiState.update { it.copy(quota = quota) } }
        }
    }
}
