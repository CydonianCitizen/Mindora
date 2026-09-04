package com.cydoniancitizen.mindora.feature.settings

import android.net.Uri
import com.cydoniancitizen.mindora.core.export.MindoraDataExporter
import com.cydoniancitizen.mindora.core.preferences.MindoraPreferencesRepository
import com.cydoniancitizen.mindora.core.preferences.model.MindoraPreferences
import com.cydoniancitizen.mindora.core.reminder.MindfulnessReminderScheduler
import com.cydoniancitizen.mindora.feature.practice.MainDispatcherRule
import java.time.LocalTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `initial state is loading then preferences produce content`() = runTest {
        val viewModel = SettingsViewModel(FakePreferencesRepository(), FakeScheduler(), FakeDataExporter())

        assertSame(SettingsUiState.Loading, viewModel.uiState.value)
        advanceUntilIdle()
        assertEquals(SettingsUiState.Content(MindoraPreferences()), viewModel.uiState.value)
    }

    @Test
    fun `valid goal and off persist`() = runTest {
        val repository = FakePreferencesRepository()
        val viewModel = SettingsViewModel(repository, FakeScheduler(), FakeDataExporter())
        advanceUntilIdle()

        viewModel.setWeeklyGoal(60)
        advanceUntilIdle()
        assertEquals(60, repository.state.value.weeklyGoalMinutes)

        viewModel.setWeeklyGoal(null)
        advanceUntilIdle()
        assertEquals(null, repository.state.value.weeklyGoalMinutes)
    }

    @Test
    fun `permission-granted enable persists and schedules then disable cancels`() = runTest {
        val repository = FakePreferencesRepository()
        val scheduler = FakeScheduler()
        val viewModel = SettingsViewModel(repository, scheduler, FakeDataExporter())
        advanceUntilIdle()

        viewModel.enableDailyReminder()
        advanceUntilIdle()
        assertTrue(repository.state.value.dailyReminderEnabled)
        assertEquals(listOf(LocalTime.of(20, 0)), scheduler.scheduledTimes)

        viewModel.disableDailyReminder()
        advanceUntilIdle()
        assertFalse(repository.state.value.dailyReminderEnabled)
        assertEquals(1, scheduler.cancelCount)
    }

    @Test
    fun `time change persists and schedules only when enabled`() = runTest {
        val repository = FakePreferencesRepository()
        val scheduler = FakeScheduler()
        val viewModel = SettingsViewModel(repository, scheduler, FakeDataExporter())
        advanceUntilIdle()

        viewModel.setDailyReminderTime(LocalTime.of(7, 15))
        advanceUntilIdle()
        assertEquals(LocalTime.of(7, 15), repository.state.value.dailyReminderTime)
        assertTrue(scheduler.scheduledTimes.isEmpty())

        repository.setDailyReminderEnabled(true)
        advanceUntilIdle()
        viewModel.setDailyReminderTime(LocalTime.of(8, 30))
        advanceUntilIdle()
        assertEquals(LocalTime.of(8, 30), repository.state.value.dailyReminderTime)
        assertEquals(listOf(LocalTime.of(8, 30)), scheduler.scheduledTimes)
    }

    @Test
    fun `scheduling failure does not report reminder enabled`() = runTest {
        val repository = FakePreferencesRepository()
        val scheduler = FakeScheduler(failSchedule = true)
        val viewModel = SettingsViewModel(repository, scheduler, FakeDataExporter())
        advanceUntilIdle()

        viewModel.enableDailyReminder()
        advanceUntilIdle()

        assertFalse(repository.state.value.dailyReminderEnabled)
        val state = viewModel.uiState.value as SettingsUiState.Content
        assertTrue(state.hasOperationError)
    }

    @Test
    fun `preference write failure produces recoverable error`() = runTest {
        val repository = FakePreferencesRepository(failWrites = true)
        val viewModel = SettingsViewModel(repository, FakeScheduler(), FakeDataExporter())
        advanceUntilIdle()

        viewModel.setWeeklyGoal(60)
        advanceUntilIdle()

        assertTrue((viewModel.uiState.value as SettingsUiState.Content).hasOperationError)
    }

    @Test
    fun `preference read failure produces error`() = runTest {
        val failingRepository = object : MindoraPreferencesRepository {
            override val preferences: Flow<MindoraPreferences> = flow { error("preferences") }
            override suspend fun setWeeklyGoalMinutes(minutes: Int?) = Unit
            override suspend fun setDailyReminderEnabled(enabled: Boolean) = Unit
            override suspend fun setDailyReminderTime(time: LocalTime) = Unit
        }
        val viewModel = SettingsViewModel(failingRepository, FakeScheduler(), FakeDataExporter())

        advanceUntilIdle()

        assertSame(SettingsUiState.Error, viewModel.uiState.value)
    }

    private class FakePreferencesRepository(
        initial: MindoraPreferences = MindoraPreferences(),
        private val failWrites: Boolean = false,
    ) : MindoraPreferencesRepository {
        val state = MutableStateFlow(initial)
        override val preferences: Flow<MindoraPreferences> = state

        override suspend fun setWeeklyGoalMinutes(minutes: Int?) {
            check(!failWrites) { "write" }
            state.value = state.value.copy(weeklyGoalMinutes = minutes)
        }

        override suspend fun setDailyReminderEnabled(enabled: Boolean) {
            check(!failWrites) { "write" }
            state.value = state.value.copy(dailyReminderEnabled = enabled)
        }

        override suspend fun setDailyReminderTime(time: LocalTime) {
            check(!failWrites) { "write" }
            state.value = state.value.copy(dailyReminderTime = time)
        }
    }

    /**
     * Present so the view model can be built. Exporting itself is driven by a content Uri, which
     * a JVM unit test cannot create, so the writing is covered by MindoraDataExportTest instead.
     */
    private class FakeDataExporter : MindoraDataExporter {
        val destinations = mutableListOf<Uri>()

        override suspend fun exportTo(destination: Uri) {
            destinations += destination
        }
    }

    private class FakeScheduler(
        private val failSchedule: Boolean = false,
    ) : MindfulnessReminderScheduler {
        val scheduledTimes = mutableListOf<LocalTime>()
        var cancelCount = 0

        override fun scheduleDaily(time: LocalTime) {
            check(!failSchedule) { "schedule" }
            scheduledTimes += time
        }

        override fun cancel() {
            cancelCount++
        }
    }
}
