package com.cydoniancitizen.mindora.core.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

class AlarmManagerMindfulnessReminderScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : MindfulnessReminderScheduler {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    override fun scheduleDaily(time: LocalTime) {
        val trigger = calculateNextReminderInstant(
            now = Instant.now(),
            time = time,
            zoneId = ZoneId.systemDefault(),
        )
        val pendingIntent = reminderPendingIntent(
            context = context,
            flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        ) ?: error("Reminder PendingIntent could not be created.")
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            trigger.toEpochMilli(),
            pendingIntent,
        )
    }

    override fun cancel() {
        reminderPendingIntent(
            context = context,
            flags = PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )?.let { pendingIntent ->
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}

internal const val DAILY_REMINDER_ACTION =
    "com.cydoniancitizen.mindora.action.DAILY_MINDFULNESS_REMINDER"
internal const val REMINDER_REQUEST_CODE = 810

internal fun reminderPendingIntent(
    context: Context,
    flags: Int,
): PendingIntent? = PendingIntent.getBroadcast(
    context,
    REMINDER_REQUEST_CODE,
    Intent(context, MindfulnessReminderReceiver::class.java).setAction(DAILY_REMINDER_ACTION),
    flags,
)
