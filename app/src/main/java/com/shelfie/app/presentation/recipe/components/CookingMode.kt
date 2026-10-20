package com.shelfie.app.presentation.recipe.components

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.animation.animateColorAsState
import com.shelfie.app.presentation.mascot.FrizzyMood
import com.shelfie.app.presentation.mascot.Frizzy
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shelfie.app.R
import com.shelfie.app.domain.model.Recipe
import com.shelfie.app.presentation.common.ShelfieCard
import com.shelfie.app.presentation.recipe.CookingProgress

/**
 * Focused, one-step-at-a-time view for cooking with messy hands: big text, one big
 * "Step N done" button, and the screen stays on.
 */
@Composable
fun CookingMode(
    recipe: Recipe,
    progress: CookingProgress,
    isCooked: Boolean,
    isMarkingCooked: Boolean,
    onMarkCooked: () -> Unit,
    onStepDone: () -> Unit,
    onPreviousStep: () -> Unit,
    onBackToRecipe: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    KeepScreenOn()
    Column(modifier = modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp)) {
        StepSegments(total = progress.totalSteps, completed = progress.completedSteps)
        Spacer(modifier = Modifier.height(12.dp))

        AnimatedContent(
            targetState = progress.currentStepIndex,
            transitionSpec = {
                val forward = targetState > initialState
                (slideInHorizontally { if (forward) it else -it } + fadeIn()) togetherWith
                    (slideOutHorizontally { if (forward) -it else it } + fadeOut())
            },
            modifier = Modifier.weight(1f),
            label = "cookingStep",
        ) { index ->
            if (index >= recipe.steps.size) {
                FinishedContent(
                    rescuedCount = recipe.rescuedIngredients.size,
                    isCooked = isCooked,
                    isMarkingCooked = isMarkingCooked,
                    onMarkCooked = onMarkCooked,
                    onBackToRecipe = onBackToRecipe,
                    onFinish = onFinish,
                )
            } else {
                StepContent(
                    recipe = recipe,
                    index = index,
                    onStepDone = onStepDone,
                    onPreviousStep = onPreviousStep,
                )
            }
        }
    }
}

@Composable
private fun StepContent(
    recipe: Recipe,
    index: Int,
    onStepDone: () -> Unit,
    onPreviousStep: () -> Unit,
) {
    val step = recipe.steps[index]
    val number = index + 1
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.recipe_step_counter, number, recipe.steps.size),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(12.dp))
        ShelfieCard(
            modifier = Modifier.fillMaxWidth().weight(1f),
            shape = RoundedCornerShape(28.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                // Chef Frizzy keeps you company; the instruction stays the focus below.
                Frizzy(mood = FrizzyMood.CHEF, size = 120.dp, modifier = Modifier.align(Alignment.CenterHorizontally))
                Spacer(modifier = Modifier.height(24.dp))
                StepNumber(number = number)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = step.instruction,
                    style = MaterialTheme.typography.headlineSmall,
                )
                if (step.durationMinutes > 0) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "⏲ " + pluralStringResource(
                            R.plurals.recipe_step_wait,
                            step.durationMinutes,
                            step.durationMinutes,
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onStepDone,
            modifier = Modifier.fillMaxWidth().height(64.dp),
        ) {
            Icon(painterResource(R.drawable.ic_check), contentDescription = null)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.recipe_action_step_done, number),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        TextButton(
            onClick = onPreviousStep,
            enabled = index > 0,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Text(stringResource(R.string.recipe_action_previous_step))
        }
    }
}

@Composable
private fun FinishedContent(
    rescuedCount: Int,
    isCooked: Boolean,
    isMarkingCooked: Boolean,
    onMarkCooked: () -> Unit,
    onBackToRecipe: () -> Unit,
    onFinish: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Frizzy(mood = FrizzyMood.CELEBRATING, size = 160.dp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.recipe_finished_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = pluralStringResource(R.plurals.recipe_finished_subtitle, rescuedCount, rescuedCount),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(32.dp))
        if (isCooked) {
            Text(
                text = "✓ " + stringResource(R.string.recipe_cooked_done),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onFinish, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Text(stringResource(R.string.recipe_action_done))
            }
        } else {
            // The point of the whole flow: the rescued items leave the pantry as eaten.
            Button(
                onClick = onMarkCooked,
                enabled = !isMarkingCooked,
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Text(stringResource(R.string.recipe_action_cooked_use_up))
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onFinish, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.recipe_action_done))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(onClick = onBackToRecipe, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.recipe_action_back_to_recipe))
        }
    }
}

@Composable
private fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}

/**
 * One segment per step: done steps solid, the current one half-tinted, the rest a light track.
 * Reads at a glance from across the kitchen, unlike a thin bar that starts empty.
 */
@Composable
private fun StepSegments(total: Int, completed: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(total) { index ->
            val color by animateColorAsState(
                when {
                    index < completed -> MaterialTheme.colorScheme.primary
                    index == completed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                    else -> MaterialTheme.colorScheme.surfaceContainerHighest
                },
                label = "stepSegment",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp)
                    .background(color, CircleShape),
            )
        }
    }
}
