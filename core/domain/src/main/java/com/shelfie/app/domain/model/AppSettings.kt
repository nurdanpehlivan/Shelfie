package com.shelfie.app.domain.model

import java.time.LocalTime

data class AppSettings(
    val notificationsEnabled: Boolean = true,
    /** Hour of day (0–23) of the daily freshness check. */
    val reminderHour: Int = DEFAULT_REMINDER_HOUR,
    val freshnessThresholds: FreshnessThresholds = FreshnessThresholds.DEFAULT,
    /**
     * What an average product is worth to the user, in their own currency. Used only for the
     * "estimated savings" statistic, since the app has no real prices.
     */
    val averageItemValue: Double = DEFAULT_AVERAGE_ITEM_VALUE,
    /** The first-run introduction was finished or skipped. */
    val onboardingCompleted: Boolean = false,
    /** Light by default: the warm, fresh look is the app's identity; dark is opt-in. */
    val themeMode: ThemeMode = ThemeMode.LIGHT,
) {
    val reminderTime: LocalTime get() = LocalTime.of(reminderHour, 0)

    companion object {
        const val DEFAULT_REMINDER_HOUR = 9
        const val MIN_REMINDER_HOUR = 6
        const val MAX_REMINDER_HOUR = 22
        const val DEFAULT_AVERAGE_ITEM_VALUE = 50.0
    }
}

enum class ThemeMode {
    LIGHT,
    DARK,

    /** Follows the phone's dark mode. */
    SYSTEM;

    companion object {
        fun fromName(name: String?): ThemeMode = entries.firstOrNull { it.name == name } ?: LIGHT
    }
}
