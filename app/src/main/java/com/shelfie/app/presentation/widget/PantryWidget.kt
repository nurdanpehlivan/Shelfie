package com.shelfie.app.presentation.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.shelfie.app.core.ui.R
import com.shelfie.app.domain.model.FreshnessStatus
import com.shelfie.app.domain.model.PantryItem
import com.shelfie.app.domain.repository.PantryRepository
import com.shelfie.app.domain.repository.SettingsRepository
import com.shelfie.app.presentation.MainActivity
import com.shelfie.app.presentation.common.displayEmoji
import com.shelfie.app.presentation.theme.LightFreshnessPalette
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Home-screen widget: the products that run out soonest, so the pantry nudges without opening
 * the app. Tapping it opens the pantry. Updated whenever the pantry changes (ShelfieApplication)
 * and at least hourly by the launcher, which also rolls "days left" over at midnight.
 */
class PantryWidget : GlanceAppWidget() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WidgetEntryPoint {
        fun pantryRepository(): PantryRepository
        fun settingsRepository(): SettingsRepository
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        val today = LocalDate.now()
        val thresholds = entryPoint.settingsRepository().settings.first().freshnessThresholds
        val soonest = entryPoint.pantryRepository().observePantryItems().first().take(MAX_ROWS)
        val rows = soonest.map { item ->
            WidgetRow(
                emoji = item.displayEmoji,
                name = item.name,
                daysText = daysText(context, ChronoUnit.DAYS.between(today, item.expiryDate)),
                status = item.freshnessStatus(today, thresholds),
            )
        }
        provideContent { WidgetContent(context, rows) }
    }

    private data class WidgetRow(val emoji: String, val name: String, val daysText: String, val status: FreshnessStatus)

    @Composable
    private fun WidgetContent(context: Context, rows: List<WidgetRow>) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(24.dp)
                .background(CREAM)
                .padding(14.dp)
                .clickable(actionStartActivity<MainActivity>()),
        ) {
            Text(
                text = context.getString(R.string.widget_title),
                style = TextStyle(color = INK, fontSize = 15.sp, fontWeight = FontWeight.Bold),
            )
            Spacer(GlanceModifier.height(8.dp))
            if (rows.isEmpty()) {
                Text(
                    text = context.getString(R.string.widget_empty),
                    style = TextStyle(color = MUTED, fontSize = 13.sp),
                )
            }
            rows.forEach { row ->
                Row(
                    modifier = GlanceModifier.fillMaxWidth().padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = row.emoji, style = TextStyle(fontSize = 16.sp))
                    Text(
                        text = "  " + row.name,
                        maxLines = 1,
                        style = TextStyle(color = INK, fontSize = 14.sp),
                        modifier = GlanceModifier.defaultWeight(),
                    )
                    Text(
                        text = row.daysText,
                        maxLines = 1,
                        style = TextStyle(color = ColorProvider(row.status.accent()), fontSize = 13.sp, fontWeight = FontWeight.Bold),
                    )
                }
            }
        }
    }

    private fun FreshnessStatus.accent(): Color = LightFreshnessPalette.forStatus(this).accent

    private fun daysText(context: Context, days: Long): String = when {
        days < 0 -> context.resources.getQuantityString(R.plurals.dashboard_expired_days_ago, (-days).toInt(), (-days).toInt())
        days == 0L -> context.getString(R.string.dashboard_expires_today)
        days == 1L -> context.getString(R.string.dashboard_expires_tomorrow)
        else -> context.resources.getQuantityString(R.plurals.dashboard_days_left, days.toInt(), days.toInt())
    }

    private companion object {
        const val MAX_ROWS = 4

        // The widget keeps the brand's light look on any wallpaper, like the app's default theme.
        val CREAM = ColorProvider(Color(0xFFFFF9F2))
        val INK = ColorProvider(Color(0xFF2A2420))
        val MUTED = ColorProvider(Color(0xFF7A6F66))
    }
}

class PantryWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PantryWidget()
}
