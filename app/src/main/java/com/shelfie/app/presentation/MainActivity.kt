package com.shelfie.app.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.shelfie.app.domain.repository.SettingsRepository
import com.shelfie.app.presentation.navigation.ShelfieNavHost
import com.shelfie.app.presentation.onboarding.OnboardingScreen
import com.shelfie.app.presentation.theme.ShelfieTheme
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShelfieTheme {
                val viewModel: MainViewModel = hiltViewModel()
                val onboardingCompleted by viewModel.onboardingCompleted.collectAsStateWithLifecycle()
                AnimatedContent(
                    targetState = onboardingCompleted,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "onboarding",
                ) { completed ->
                    when (completed) {
                        // Settings still loading: plain brand background, no flash of either screen.
                        null -> Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
                        false -> OnboardingScreen(onFinished = viewModel::completeOnboarding)
                        true -> ShelfieNavHost()
                    }
                }
            }
        }
    }
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    /** `null` until DataStore has been read. */
    val onboardingCompleted: StateFlow<Boolean?> = settingsRepository.settings
        .map { it.onboardingCompleted }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun completeOnboarding() {
        viewModelScope.launch { settingsRepository.setOnboardingCompleted() }
    }
}
