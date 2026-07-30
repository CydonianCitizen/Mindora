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
class MindfulnessReminderReceiver : BroadcastReceiver() {
    @Inject lateinit var preferencesRepository: MindoraPreferencesRepository
    @Inject lateinit var scheduler: MindfulnessReminderScheduler
    @Inject lateinit var notificationPublisher: ReminderNotificationPublisher

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != DAILY_REMINDER_ACTION) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val preferences = preferencesRepository.preferences.first()
                if (preferences.dailyReminderEnabled) {
                    notificationPublisher.postIfAllowed()
                    scheduler.scheduleDaily(preferences.dailyReminderTime)
                } else {
                    scheduler.cancel()
                }
            } catch (error: Exception) {
                Log.e(TAG, "Daily reminder could not be handled.", error)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        const val TAG = "MindoraReminder"
    }
}
