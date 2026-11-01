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
import com.shelfie.app.core.ui.R
import com.shelfie.app.domain.model.HouseholdActivity
import com.shelfie.app.presentation.MainActivity
import com.shelfie.app.presentation.common.activityLabelRes
import com.shelfie.app.presentation.common.displayName
import com.shelfie.app.presentation.navigation.ACCOUNT_DEEP_LINK
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** "Ayşe · Added: Milk": what the rest of the household did, in one updating notification. */
@Singleton
class HouseholdActivityNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val notificationManager = NotificationManagerCompat.from(context)

    fun createChannel() {
        notificationManager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName(context.getString(R.string.notification_channel_household))
                .setDescription(context.getString(R.string.notification_channel_household_description))
                .build(),
        )
    }

    /** [events] oldest first; does nothing for none or without notification permission. */
    fun show(events: List<HouseholdActivity>) {
        if (events.isEmpty() || !canPostNotifications()) return
        val lines = events.map { event ->
            context.getString(
                R.string.notification_household_line,
                displayName(event.actor),
                context.getString(event.action.activityLabelRes),
                listOfNotNull(event.emoji, event.name).joinToString(" "),
            )
        }
        val title = context.resources.getQuantityString(R.plurals.notification_household_title, events.size, events.size)
        val style = NotificationCompat.InboxStyle().setBigContentTitle(title)
        lines.takeLast(MAX_LINES).asReversed().forEach(style::addLine)
        val intent = Intent(Intent.ACTION_VIEW, ACCOUNT_DEEP_LINK.toUri(), context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(lines.last())
            .setStyle(style)
            .setNumber(events.size)
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    REQUEST_CODE,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                ),
            )
            .setAutoCancel(true)
            .build()
        try {
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // Permission revoked between the check and the call.
        }
    }

    private fun canPostNotifications(): Boolean {
        if (!notificationManager.areNotificationsEnabled()) return false
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    private companion object {
        const val CHANNEL_ID = "household_activity"
        const val NOTIFICATION_ID = 1003
        const val REQUEST_CODE = 3
        const val MAX_LINES = 6
    }
}
