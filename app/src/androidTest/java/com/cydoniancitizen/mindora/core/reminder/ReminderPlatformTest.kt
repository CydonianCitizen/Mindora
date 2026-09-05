package com.cydoniancitizen.mindora.core.reminder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.provider.Settings
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cydoniancitizen.mindora.R
import java.time.LocalTime
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReminderPlatformTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val scheduler = AlarmManagerMindfulnessReminderScheduler(context)

    @After
    fun cancelAlarm() {
        scheduler.cancel()
    }

    @Test
    fun schedulingUsesOneStablePendingIntentAndCancelMatchesIt() {
        scheduler.scheduleDaily(LocalTime.of(20, 0))
        val first = existingReminderPendingIntent()

        scheduler.scheduleDaily(LocalTime.of(7, 30))
        val replacement = existingReminderPendingIntent()

        assertNotNull(first)
        assertEquals(first, replacement)
        scheduler.cancel()
        assertNull(existingReminderPendingIntent())
    }

    @Test
    fun reminderChannelAndNeutralNotificationAreBuilt() {
        val publisher = ReminderNotificationPublisher(context)
        publisher.createChannel()

        val channel = context.getSystemService(NotificationManager::class.java)
            .getNotificationChannel(ReminderNotificationPublisher.CHANNEL_ID)
        val notification = publisher.buildNotification()

        assertEquals(context.getString(R.string.reminder_channel_name), channel.name.toString())
        assertEquals(NotificationManager.IMPORTANCE_DEFAULT, channel.importance)
        val settingsIntent = reminderNotificationSettingsIntent(context)
        assertEquals(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS, settingsIntent.action)
        assertEquals(
            ReminderNotificationPublisher.CHANNEL_ID,
            settingsIntent.getStringExtra(Settings.EXTRA_CHANNEL_ID),
        )
        assertEquals(
            context.getString(R.string.reminder_notification_title),
            notification.extras.getString(Notification.EXTRA_TITLE),
        )
        assertEquals(
            context.getString(R.string.reminder_notification_body),
            notification.extras.getString(Notification.EXTRA_TEXT),
        )
        assertFalse(notification.flags and Notification.FLAG_AUTO_CANCEL == 0)
        assertEquals(context.packageName, notification.contentIntent.creatorPackage)
    }

    @Test
    fun publishingDoesNotCrashWhenNotificationsAreUnavailable() {
        ReminderNotificationPublisher(context).postIfAllowed()
    }

    @Test
    fun missingChannelIsAllowedButDisabledChannelIsBlocked() {
        assertTrue(reminderChannelAllowsNotifications(null))
        assertFalse(
            reminderChannelAllowsNotifications(
                NotificationChannel("disabled", "Disabled", NotificationManager.IMPORTANCE_NONE),
            ),
        )
    }

    private fun existingReminderPendingIntent() = reminderPendingIntent(
        context,
        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
    )
}
