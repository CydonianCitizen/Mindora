package com.cydoniancitizen.mindora.core.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.cydoniancitizen.mindora.MainActivity
import com.cydoniancitizen.mindora.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class ReminderNotificationPublisher @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    fun postIfAllowed(): Boolean {
        if (!notificationsAllowed(context)) return false

        createChannel()
        val notification = buildNotification()
        return try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            true
        } catch (error: SecurityException) {
            false
        }
    }

    internal fun buildNotification(): android.app.Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_REMINDER_NOTIFICATION
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            CONTENT_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_mindfulness_reminder)
            .setContentTitle(context.getString(R.string.reminder_notification_title))
            .setContentText(context.getString(R.string.reminder_notification_body))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()
        return notification
    }

    internal fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.reminder_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.reminder_channel_description)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        const val ACTION_REMINDER_NOTIFICATION = "com.cydoniancitizen.mindora.ACTION_REMINDER_NOTIFICATION"
        const val CHANNEL_ID = "mindfulness_reminders"
        const val NOTIFICATION_ID = 810
        private const val CONTENT_REQUEST_CODE = 811
    }
}

fun notificationsAllowed(context: Context): Boolean =
    ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS,
    ) == PackageManager.PERMISSION_GRANTED &&
        NotificationManagerCompat.from(context).areNotificationsEnabled() &&
        reminderChannelAllowsNotifications(reminderChannel(context))

internal fun reminderChannelAllowsNotifications(channel: NotificationChannel?): Boolean =
    channel == null || channel.importance != NotificationManager.IMPORTANCE_NONE

internal fun reminderNotificationSettingsIntent(context: Context): Intent {
    val channelExists = reminderChannel(context) != null
    return Intent(
        if (channelExists) {
            Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS
        } else {
            Settings.ACTION_APP_NOTIFICATION_SETTINGS
        },
    ).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName).apply {
        if (channelExists) {
            putExtra(Settings.EXTRA_CHANNEL_ID, ReminderNotificationPublisher.CHANNEL_ID)
        }
    }
}

private fun reminderChannel(context: Context): NotificationChannel? =
    context.getSystemService(NotificationManager::class.java)
        .getNotificationChannel(ReminderNotificationPublisher.CHANNEL_ID)
