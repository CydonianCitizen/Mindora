package com.cydoniancitizen.mindora.core.preferences.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.time.LocalTime
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DataStoreMindoraPreferencesRepositoryTest {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val createdDirectories = mutableListOf<File>()

    @After
    fun cleanUp() {
        scope.cancel()
        createdDirectories.forEach(File::deleteRecursively)
    }

    @Test
    fun defaultsDisableGoalAndReminderAtTwentyHundred() = runBlocking {
        val repository = repository("defaults")

        val preferences = repository.preferences.first()

        assertNull(preferences.weeklyGoalMinutes)
        assertFalse(preferences.dailyReminderEnabled)
        assertEquals(LocalTime.of(20, 0), preferences.dailyReminderTime)
    }

    @Test
    fun supportedGoalsPersistAndNullDisablesGoal() = runBlocking {
        val repository = repository("goals")

        listOf(30, 60, 90, 120).forEach { minutes ->
            repository.setWeeklyGoalMinutes(minutes)
            assertEquals(minutes, repository.preferences.first().weeklyGoalMinutes)
        }
        repository.setWeeklyGoalMinutes(null)

        assertNull(repository.preferences.first().weeklyGoalMinutes)
    }

    @Test
    fun unsupportedGoalIsRejectedWithoutChangingStoredValue() = runBlocking {
        val repository = repository("invalid")
        repository.setWeeklyGoalMinutes(60)

        val failure = captureFailure { repository.setWeeklyGoalMinutes(45) }

        assertTrue(failure is IllegalArgumentException)
        assertEquals(60, repository.preferences.first().weeklyGoalMinutes)
    }

    @Test
    fun reminderStateAndTimePersistAndFlowEmitsUpdates() = runBlocking {
        val repository = repository("reminder")
        val emissions = async(start = CoroutineStart.UNDISPATCHED) {
            repository.preferences.take(2).toList()
        }

        repository.setDailyReminderEnabled(true)
        val values = emissions.await()
        repository.setDailyReminderTime(LocalTime.of(7, 35))
        val preferences = repository.preferences.first()

        assertFalse(values.first().dailyReminderEnabled)
        assertTrue(values.last().dailyReminderEnabled)
        assertTrue(preferences.dailyReminderEnabled)
        assertEquals(LocalTime.of(7, 35), preferences.dailyReminderTime)
    }

    @Test
    fun readAndWriteFailuresArePropagated() = runBlocking {
        val directory = testDirectory("directory-not-file")
        val repository = DataStoreMindoraPreferencesRepository(
            PreferenceDataStoreFactory.create(scope = scope, produceFile = { directory }),
        )

        val readFailure = captureFailure { repository.preferences.first() }
        val writeFailure = captureFailure { repository.setDailyReminderEnabled(true) }

        assertNotNull(readFailure)
        assertNotNull(writeFailure)
    }

    private fun repository(name: String): DataStoreMindoraPreferencesRepository {
        val file = File(testDirectory(name), "preferences.preferences_pb")
        return DataStoreMindoraPreferencesRepository(
            PreferenceDataStoreFactory.create(scope = scope, produceFile = { file }),
        )
    }

    private fun testDirectory(name: String): File {
        val root = InstrumentationRegistry.getInstrumentation().targetContext.cacheDir
        return File(root, "datastore-test-$name-${UUID.randomUUID()}").also {
            check(it.mkdirs())
            createdDirectories += it
        }
    }

    private suspend fun captureFailure(block: suspend () -> Unit): Throwable? = try {
        block()
        null
    } catch (error: Throwable) {
        error
    }
}
