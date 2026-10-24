package com.shelfie.app.presentation.premium

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shelfie.app.core.ui.R
import com.shelfie.app.domain.model.PremiumOffer
import com.shelfie.app.domain.model.PurchaseHost
import com.shelfie.app.domain.model.QuotaStatus
import com.shelfie.app.presentation.common.HighlightCard
import com.shelfie.app.presentation.common.ShelfieCard
import com.shelfie.app.presentation.common.ShelfieTopBar
import com.shelfie.app.presentation.common.glow
import com.shelfie.app.presentation.mascot.Frizzy
import com.shelfie.app.presentation.mascot.FrizzyMood
import com.shelfie.app.presentation.theme.extendedColors
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun PremiumRoute(
    onNavigateUp: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: PremiumViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val resources = LocalResources.current
    LaunchedEffect(state.messageRes) {
        val message = state.messageRes ?: return@LaunchedEffect
        viewModel.messageShown()
        snackbarHostState.showSnackbar(resources.getString(message))
    }
    PremiumScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onNavigateUp = onNavigateUp,
        onOpenSettings = onOpenSettings,
        onSelect = viewModel::select,
        onBuy = { viewModel.buy(PurchaseHost(context)) },
        manageUrl = viewModel.manageUrl,
        onRestore = viewModel::restore,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumScreen(
    state: PremiumUiState,
    onNavigateUp: () -> Unit,
    onOpenSettings: () -> Unit,
    onSelect: (PremiumOffer) -> Unit,
    onBuy: () -> Unit,
    onRestore: () -> Unit,
    manageUrl: String = "",
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val uriHandler = LocalUriHandler.current
    val isPremium = state.quota?.isPremium == true
    Scaffold(
        topBar = { ShelfieTopBar(title = stringResource(R.string.premium_title), onNavigateUp = onNavigateUp) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            PremiumHero(isPremium = isPremium, premiumUntil = state.quota)
            state.quota?.let { UsageCard(it) }

            when {
                isPremium -> OutlinedButton(
                    onClick = { uriHandler.openUri(manageUrl) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) { Text(stringResource(R.string.premium_manage)) }

                !state.isSignedIn -> ShelfieCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(R.string.premium_needs_account), style = MaterialTheme.typography.bodyMedium)
                        Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.account_sign_in_or_register))
                        }
                    }
                }

                state.offersUnavailable -> Text(
                    text = stringResource(R.string.premium_unavailable),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                state.offers.isEmpty() -> Box(Modifier.fillMaxWidth().padding(24.dp), Alignment.Center) {
                    CircularProgressIndicator()
                }

                else -> {
                    state.offers.forEach { offer ->
                        OfferCard(offer = offer, selected = offer == state.selectedOffer, onClick = { onSelect(offer) })
                    }
                    Button(
                        onClick = onBuy,
                        enabled = !state.isWorking,
                        modifier = Modifier.fillMaxWidth().height(56.dp).glow(MaterialTheme.extendedColors.glow, CircleShape),
                    ) {
                        if (state.isWorking) {
                            CircularProgressIndicator(modifier = Modifier.height(20.dp).width(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text("✨ " + stringResource(R.string.premium_upgrade), style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    Text(
                        text = stringResource(R.string.premium_terms),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (state.isSignedIn) {
                TextButton(onClick = onRestore, enabled = !state.isWorking, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text(stringResource(R.string.premium_restore))
                }
            }
        }
    }
}

@Composable
private fun PremiumHero(isPremium: Boolean, premiumUntil: QuotaStatus?) {
    val extended = MaterialTheme.extendedColors
    HighlightCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(if (isPremium) R.string.premium_active_title else R.string.premium_hero_title),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    val until = premiumUntil?.premiumUntil
                    if (isPremium && until != null) {
                        val date = until.atZone(ZoneId.systemDefault()).toLocalDate()
                            .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
                        Text(
                            text = stringResource(R.string.premium_active_until, date),
                            style = MaterialTheme.typography.bodyMedium,
                            color = extended.onHeroVariant,
                        )
                    }
                }
                Frizzy(mood = FrizzyMood.CELEBRATING, size = 84.dp)
            }
            Spacer(Modifier.height(12.dp))
            listOf(R.string.premium_benefit_ai, R.string.premium_benefit_household, R.string.premium_benefit_support).forEach {
                Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.width(22.dp).height(22.dp).background(extended.heroTile, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { Text("✓", style = MaterialTheme.typography.labelMedium) }
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(it), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun UsageCard(quota: QuotaStatus) {
    val resets = remember(quota.resetsAt) {
        quota.resetsAt.atZone(ZoneId.systemDefault()).toLocalTime().format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
    }
    ShelfieCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.premium_usage_title), style = MaterialTheme.typography.titleMedium)
            UsageRow(label = stringResource(R.string.premium_usage_scans), used = quota.scansUsed, limit = quota.scanLimit)
            UsageRow(label = stringResource(R.string.premium_usage_recipes), used = quota.recipesUsed, limit = quota.recipeLimit)
            Text(
                text = stringResource(R.string.premium_usage_resets, resets),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun UsageRow(label: String, used: Int, limit: Int) {
    val scheme = MaterialTheme.colorScheme
    val fraction = if (limit == 0) 1f else (used.toFloat() / limit).coerceIn(0f, 1f)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row {
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text("$used / $limit", style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant)
        }
        Box(Modifier.fillMaxWidth().height(6.dp).background(scheme.surfaceContainerHigh, CircleShape)) {
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .height(6.dp)
                    .background(if (fraction >= 1f) scheme.error else scheme.primary, CircleShape),
            )
        }
    }
}

@Composable
private fun OfferCard(offer: PremiumOffer, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val isYearly = offer.isYearly
    ShelfieCard(
        onClick = onClick,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) scheme.primary else MaterialTheme.extendedColors.glassBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(if (isYearly) R.string.premium_plan_yearly else R.string.premium_plan_monthly),
                    style = MaterialTheme.typography.titleMedium,
                )
                if (offer.hasIntroOffer) {
                    Text(stringResource(R.string.premium_intro_offer), style = MaterialTheme.typography.bodySmall, color = scheme.primary)
                }
            }
            Text(
                text = stringResource(
                    if (isYearly) R.string.premium_price_per_year else R.string.premium_price_per_month,
                    offer.formattedPrice,
                ),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}
