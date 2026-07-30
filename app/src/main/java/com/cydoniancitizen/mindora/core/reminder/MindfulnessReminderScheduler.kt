package com.cydoniancitizen.mindora.core.reminder

import java.time.LocalTime

interface MindfulnessReminderScheduler {
    fun scheduleDaily(time: LocalTime)

    fun cancel()
}
