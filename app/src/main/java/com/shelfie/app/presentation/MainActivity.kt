package com.shelfie.app.presentation

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.shelfie.app.BuildConfig
import com.shelfie.app.data.install.InstallReferrerSource
import com.shelfie.app.domain.model.ThemeMode
import com.shelfie.app.domain.repository.SettingsRepository
import com.shelfie.app.domain.usecase.RepairCategoriesUseCase
import com.shelfie.app.presentation.common.AppInfo
import com.shelfie.app.presentation.common.LocalAppInfo
import com.shelfie.app.presentation.navigation.ShelfieNavHost
import com.shelfie.app.presentation.onboarding.OnboardingScreen
import com.shelfie.app.presentation.theme.ShelfieTheme
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
// AppCompatActivity, not ComponentActivity: it applies the in-app language on Android 12 and older.
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: MainViewModel = hiltViewModel()
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            // The status and navigation bar icons follow the app's theme, not the phone's:
            // a light app on a phone in dark mode needs dark icons.
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = if (darkTheme) {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    },
                    navigationBarStyle = if (darkTheme) {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    },
                )
                onDispose {}
            }
            ShelfieTheme(darkTheme = darkTheme) {
                CompositionLocalProvider(LocalAppInfo provides APP_INFO) {
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
                            true -> {
                                val inviteCode by viewModel.inviteCode.collectAsStateWithLifecycle()
                                ShelfieNavHost(inviteCode = inviteCode, onInviteCodeHandled = viewModel::inviteCodeHandled)
                            }
                        }
                    }
                }
            }
        }
    }
}

private val APP_INFO = AppInfo(
    versionName = BuildConfig.VERSION_NAME,
    packageName = BuildConfig.APPLICATION_ID,
    webBaseUrl = com.shelfie.app.core.data.BuildConfig.SHELFIE_PROXY_URL,
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    repairCategories: RepairCategoriesUseCase,
    installReferrer: InstallReferrerSource,
) : ViewModel() {

    private val _inviteCode = MutableStateFlow<String?>(null)

    /** A family invite code from the Play link this app was installed from, until handled. */
    val inviteCode: StateFlow<String?> = _inviteCode.asStateFlow()

    init {
        // Cheap and once per install; a failure just tries again next launch.
        viewModelScope.launch { runCatching { repairCategories() } }
        viewModelScope.launch { _inviteCode.value = runCatching { installReferrer.consumeInviteCode() }.getOrNull() }
    }

    fun inviteCodeHandled() {
        _inviteCode.value = null
    }

    /** `null` until DataStore has been read. */
    val onboardingCompleted: StateFlow<Boolean?> = settingsRepository.settings
        .map { it.onboardingCompleted }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Light until DataStore has been read, matching the window background. */
    val themeMode: StateFlow<ThemeMode> = settingsRepository.settings
        .map { it.themeMode }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.LIGHT)

    fun completeOnboarding() {
        viewModelScope.launch { settingsRepository.setOnboardingCompleted() }
    }
}
