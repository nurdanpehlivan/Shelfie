package com.shelfie.app.notification

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.shelfie.app.R
import com.shelfie.app.domain.model.MealType
import com.shelfie.app.domain.model.PantryItem
import com.shelfie.app.domain.model.SavedRecipe
import com.shelfie.app.presentation.MainActivity
import com.shelfie.app.presentation.navigation.RECIPE_DEEP_LINK
import com.shelfie.app.presentation.navigation.savedRecipeDeepLink
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** Posts the daily "use these before they spoil" notification that opens the rescue recipe. */
@Singleton
class FreshnessNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val notificationManager = NotificationManagerCompat.from(context)

    /** Safe to call repeatedly; re-creating an existing channel only updates its name. */
    fun createChannel() {
        notificationManager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName(context.getString(R.string.notification_channel_freshness))
                .setDescription(context.getString(R.string.notification_channel_freshness_description))
                .build(),
        )
    }

    /**
     * With a [saved] recipe the notification promises a ready-to-cook dish ("A 10-minute breakfast
     * recipe for the milk and tomatoes in your fridge is ready!"); without one (generation
     * failed) it still nudges the user to open the recipe screen, which will try again.
     *
     * Does nothing if the user hasn't allowed notifications.
     */
    fun showRescueRecipe(ingredients: List<PantryItem>, saved: SavedRecipe?, mealType: MealType) {
        if (!canPostNotifications() || ingredients.isEmpty()) return

        val locale = Locale.getDefault()
        // Mid-sentence names read better lower-cased ("milk and tomatoes"), except in German,
        // where nouns are always capitalised ("Milch und Tomaten").
        val names = joinNames(
            ingredients.map { item ->
                item.name.trim().let { if (locale.language == "de") it else it.lowercase(locale) }
            },
        )
        val title: String
        val text: String
        val recipe = saved?.recipe
        if (recipe != null) {
            title = context.getString(R.string.notification_recipe_ready_title)
            text = context.getString(
                R.string.notification_recipe_ready_text,
                names,
                recipe.cookingTimeMinutes,
                context.getString(mealType.labelRes),
            )
        } else {
            title = context.getString(R.string.notification_expiring_title)
            text = context.getString(R.string.notification_expiring_text, names)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(recipePendingIntent(saved?.id))
            .setAutoCancel(true)
            .build()

        try {
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // Permission revoked between the check and the call; nothing useful to do.
        }
    }

    private fun canPostNotifications(): Boolean {
        if (!notificationManager.areNotificationsEnabled()) return false
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    /**
     * Opens [MainActivity] in a fresh task with the deep link as data; Navigation matches it to
     * the recipe destination and puts the dashboard underneath, so Back lands there.
     */
    private fun recipePendingIntent(savedRecipeId: Long?): PendingIntent {
        val uri = savedRecipeId?.let(::savedRecipeDeepLink) ?: RECIPE_DEEP_LINK
        val intent = Intent(Intent.ACTION_VIEW, uri.toUri(), context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        return PendingIntent.getActivity(
            context,
            REQUEST_CODE_RECIPE,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    /** "milk", "milk and tomato", "milk, tomato and chicken". */
    private fun joinNames(names: List<String>): String =
        if (names.size == 1) {
            names.single()
        } else {
            context.getString(R.string.notification_join_last, names.dropLast(1).joinToString(", "), names.last())
        }

    private val MealType.labelRes: Int
        get() = when (this) {
            MealType.BREAKFAST -> R.string.meal_breakfast
            MealType.LUNCH -> R.string.meal_lunch
            MealType.DINNER -> R.string.meal_dinner
        }

    private companion object {
        const val CHANNEL_ID = "freshness_alerts"
        const val NOTIFICATION_ID = 1001
        const val REQUEST_CODE_RECIPE = 1
    }
}
