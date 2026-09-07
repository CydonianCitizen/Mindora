package com.cydoniancitizen.mindora.core.reminder

import com.cydoniancitizen.mindora.core.preferences.MindoraPreferencesRepository
import com.cydoniancitizen.mindora.core.preferences.model.MindoraPreferences
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderReschedulingTest {
    @Test
    fun `enabled preference schedules selected time`() = runTest {
        val scheduler = FakeScheduler()

        rescheduleReminder(repository(enabled = true), scheduler)

        assertEquals(listOf(LocalTime.of(7, 45)), scheduler.scheduled)
        assertEquals(0, scheduler.cancelCount)
    }

    @Test
    fun `disabled preference cancels pending alarm`() = runTest {
        val scheduler = FakeScheduler()

        rescheduleReminder(repository(enabled = false), scheduler)

        assertEquals(emptyList<LocalTime>(), scheduler.scheduled)
        assertEquals(1, scheduler.cancelCount)
    }

    private fun repository(enabled: Boolean) = object : MindoraPreferencesRepository {
        override val preferences: Flow<MindoraPreferences> = flowOf(
            MindoraPreferences(
                dailyReminderEnabled = enabled,
                dailyReminderTime = LocalTime.of(7, 45),
            ),
        )

        override suspend fun update(transform: (MindoraPreferences) -> MindoraPreferences) = Unit
    }

    private class FakeScheduler : MindfulnessReminderScheduler {
        val scheduled = mutableListOf<LocalTime>()
        var cancelCount = 0

        override fun scheduleDaily(time: LocalTime) {
            scheduled += time
        }

        override fun cancel() {
            cancelCount++
        }
    }
}
