package com.shelfie.app.presentation.recipe.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shelfie.app.R
import com.shelfie.app.domain.model.PantryItem
import com.shelfie.app.domain.model.Recipe
import com.shelfie.app.domain.model.RecipeIngredient
import com.shelfie.app.domain.model.RecipeStep
import com.shelfie.app.domain.model.Category
import com.shelfie.app.presentation.common.emoji
import com.shelfie.app.presentation.common.foodEmoji

/** Used when the model gave no usable emoji and the ingredient isn't a known pantry item. */
const val FALLBACK_INGREDIENT_EMOJI = "🥄"

/**
 * Emoji for [ingredient]: the model's choice, else the category emoji of the matching pantry
 * item, else [FALLBACK_INGREDIENT_EMOJI].
 */
fun ingredientEmoji(ingredient: RecipeIngredient, pantryItems: List<PantryItem>): String =
    ingredient.emoji
        ?: pantryItems.firstOrNull { it.name.equals(ingredient.name, ignoreCase = true) }
            ?.let { foodEmoji(it.name, it.category) }
        ?: foodEmoji(ingredient.name, Category.OTHER).takeIf { it != Category.OTHER.emoji }
        ?: FALLBACK_INGREDIENT_EMOJI

/** Header card of the recipe: a warm gradient with the title and its key facts. */
@Composable
fun RecipeHeroCard(
    recipe: Recipe,
    rescuedEmojis: List<String>,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    // Rescuing is the good news here, so it gets the brand green, not the "critical" red.
    val accent = scheme.primary
    ElevatedCard(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(scheme.primaryContainer, scheme.tertiaryContainer),
                    ),
                )
                .padding(24.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy((-10).dp)) {
                rescuedEmojis.forEach { emoji ->
                    Surface(
                        shape = CircleShape,
                        color = scheme.surface,
                        shadowElevation = 2.dp,
                        modifier = Modifier.size(56.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = emoji, fontSize = 28.sp)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = recipe.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = scheme.onPrimaryContainer,
            )
            Spacer(modifier = Modifier.height(16.dp))
            FlowRowFacts(
                facts = listOf(
                    "⏱" to pluralStringResource(
                        R.plurals.recipe_time_minutes,
                        recipe.cookingTimeMinutes,
                        recipe.cookingTimeMinutes,
                    ),
                    "🍽" to pluralStringResource(R.plurals.recipe_servings, recipe.servings, recipe.servings),
                    "♻" to pluralStringResource(
                        R.plurals.recipe_rescued_count,
                        recipe.rescuedIngredients.size,
                        recipe.rescuedIngredients.size,
                    ),
                ),
                highlightLast = accent,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRowFacts(facts: List<Pair<String, String>>, highlightLast: Color) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        facts.forEachIndexed { index, (icon, label) ->
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = icon)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (index == facts.lastIndex) highlightLast else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

/** Grid of big ingredient icons. Rescued ingredients get the "critical" tint. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IngredientGrid(
    ingredients: List<RecipeIngredient>,
    pantryItems: List<PantryItem>,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        maxItemsInEachRow = 4,
    ) {
        ingredients.forEach { ingredient ->
            IngredientTile(
                emoji = ingredientEmoji(ingredient, pantryItems),
                name = ingredient.name,
                amount = ingredient.amount,
                isRescued = ingredient.isFromPantry,
                modifier = Modifier.width(76.dp),
            )
        }
    }
}

@Composable
fun IngredientTile(
    emoji: String,
    name: String,
    amount: String,
    isRescued: Boolean,
    modifier: Modifier = Modifier,
) {
    val container = if (isRescued) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
    Column(
        modifier = modifier.clearAndSetSemantics { contentDescription = "$name $amount" },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(container, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = emoji, fontSize = 36.sp)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (amount.isNotEmpty()) {
            Text(
                text = amount,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** One step of the overview list, with its number in a circle. */
@Composable
fun StepOverviewCard(number: Int, step: RecipeStep, modifier: Modifier = Modifier) {
    OutlinedCard(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp)) {
            StepNumber(number = number)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = step.instruction, style = MaterialTheme.typography.bodyLarge)
                if (step.durationMinutes > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "⏲ " + pluralStringResource(
                            R.plurals.recipe_step_wait,
                            step.durationMinutes,
                            step.durationMinutes,
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
fun StepNumber(number: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(32.dp)
            .background(MaterialTheme.colorScheme.primary, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = number.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = modifier.padding(top = 8.dp),
    )
}
