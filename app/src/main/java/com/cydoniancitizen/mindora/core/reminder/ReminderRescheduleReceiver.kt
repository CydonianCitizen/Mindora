package com.cydoniancitizen.mindora.core.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.cydoniancitizen.mindora.core.preferences.MindoraPreferencesRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ReminderRescheduleReceiver : BroadcastReceiver() {
    @Inject lateinit var preferencesRepository: MindoraPreferencesRepository
    @Inject lateinit var scheduler: MindfulnessReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in supportedActions) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                rescheduleReminder(preferencesRepository, scheduler)
            } catch (error: Exception) {
                Log.e(TAG, "Daily reminder could not be rescheduled.", error)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        const val TAG = "MindoraReminder"
        val supportedActions = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
        )
    }
}

internal suspend fun rescheduleReminder(
    preferencesRepository: MindoraPreferencesRepository,
    scheduler: MindfulnessReminderScheduler,
) {
    val preferences = preferencesRepository.preferences.first()
    if (preferences.dailyReminderEnabled) {
        scheduler.scheduleDaily(preferences.dailyReminderTime)
    } else {
        scheduler.cancel()
    }
}
