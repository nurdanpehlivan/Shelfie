package com.shelfie.app.presentation.dashboard.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.shelfie.app.R
import com.shelfie.app.presentation.common.glow
import com.shelfie.app.presentation.theme.extendedColors

/** Fixed in both themes: the scan button is the brand moment, like a logo. */
private val ScanGradient = Brush.linearGradient(listOf(Color(0xFF4CCB7D), Color(0xFF2BA95C), Color(0xFF1E8A48)))

/** Dark ink on the leaf green: white on mid-green doesn't reach a readable contrast. */
private val OnScanGradient = Color(0xFF04361C)

/**
 * Bottom-right actions of the pantry: the glowing gradient "Scan" button (photo → AI) and,
 * above it, a small glass "Quick add" pill for manual or barcode entry, which costs no AI quota.
 * The scan label folds away once the list scrolls, leaving the camera icon.
 */
@Composable
fun ScanActions(
    expanded: Boolean,
    onScan: () -> Unit,
    onQuickAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val extended = MaterialTheme.extendedColors
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Surface(
            onClick = onQuickAdd,
            shape = CircleShape,
            color = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
            border = BorderStroke(1.dp, extended.glassBorder),
            modifier = Modifier.background(extended.navBar, CircleShape),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = stringResource(R.string.dashboard_quick_add), style = MaterialTheme.typography.labelLarge)
            }
        }

        Surface(
            onClick = onScan,
            shape = CircleShape,
            color = Color.Transparent,
            contentColor = OnScanGradient,
            modifier = Modifier
                .glow(extended.glow, CircleShape, radius = 10.dp)
                .background(ScanGradient, CircleShape),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_photo_camera),
                    contentDescription = if (expanded) null else stringResource(R.string.dashboard_action_scan),
                    modifier = Modifier.size(24.dp),
                )
                AnimatedVisibility(
                    visible = expanded,
                    enter = fadeIn() + expandHorizontally(),
                    exit = fadeOut() + shrinkHorizontally(),
                ) {
                    Row {
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = stringResource(R.string.dashboard_action_scan), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}
