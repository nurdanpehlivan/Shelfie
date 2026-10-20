package com.shelfie.app.presentation.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shelfie.app.R
import com.shelfie.app.presentation.mascot.Frizzy
import com.shelfie.app.presentation.mascot.FrizzyMood
import com.shelfie.app.presentation.theme.ShelfieTheme
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val mood: FrizzyMood,
    @param:StringRes val titleRes: Int,
    @param:StringRes val bodyRes: Int,
)

private val pages = listOf(
    OnboardingPage(FrizzyMood.HAPPY, R.string.onboarding_welcome_title, R.string.onboarding_welcome_body),
    OnboardingPage(FrizzyMood.WORRIED, R.string.onboarding_track_title, R.string.onboarding_track_body),
    OnboardingPage(FrizzyMood.CHEF, R.string.onboarding_recipes_title, R.string.onboarding_recipes_body),
    OnboardingPage(FrizzyMood.CELEBRATING, R.string.onboarding_notify_title, R.string.onboarding_notify_body),
)

/**
 * First-run introduction by Frizzy. The last page explains the daily reminder before asking
 * for the notification permission, which is far more likely to be granted than a cold prompt.
 */
@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val pagerState = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val isLast = pagerState.currentPage == pages.lastIndex
    // Whatever the user answers, the introduction is done.
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        onFinished()
    }

    fun finish() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            onFinished()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.background),
                ),
            )
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.End) {
            if (!isLast) {
                TextButton(onClick = onFinished) { Text(stringResource(R.string.onboarding_skip)) }
            } else {
                Spacer(modifier = Modifier.height(48.dp))
            }
        }

        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { index ->
            val page = pages[index]
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(260.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.7f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Frizzy(mood = page.mood, size = 200.dp)
                }
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = stringResource(page.titleRes),
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(page.bodyRes),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }

        PageDots(count = pages.size, current = pagerState.currentPage, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                if (isLast) finish() else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(56.dp),
        ) {
            Text(stringResource(if (isLast) R.string.onboarding_start else R.string.onboarding_next))
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun PageDots(count: Int, current: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(count) { index ->
            val selected = index == current
            val width by animateDpAsState(if (selected) 24.dp else 8.dp, label = "dotWidth")
            val color by animateColorAsState(
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                label = "dotColor",
            )
            Box(modifier = Modifier.height(8.dp).width(width).background(color, CircleShape))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingPreview() {
    ShelfieTheme { OnboardingScreen(onFinished = {}) }
}
