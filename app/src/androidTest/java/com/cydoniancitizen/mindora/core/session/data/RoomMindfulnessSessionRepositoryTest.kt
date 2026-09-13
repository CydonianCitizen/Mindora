package com.cydoniancitizen.mindora.core.session.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cydoniancitizen.mindora.core.database.MindoraDatabase
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSession
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSessionStatus
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSessionType
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises the real persistence path: a real Room database, the real DAO, and the real
 * [RoomMindfulnessSessionRepository] with its validation. Nothing here is mocked, so a session that
 * saves in this test is a session that saves on a device.
 */
@RunWith(AndroidJUnit4::class)
class RoomMindfulnessSessionRepositoryTest {
    private lateinit var database: MindoraDatabase
    private lateinit var repository: RoomMindfulnessSessionRepository

    @Before
    fun createRepository() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, MindoraDatabase::class.java).build()
        repository = RoomMindfulnessSessionRepository(database.mindfulnessSessionDao())
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun libraryMeditationSessionWithoutPathIsSavedAndReadBack() = runBlocking {
        // The shape FreeMeditationViewModel produces for a meditation started from the library:
        // it records the meditation id it ran, but no path, because a library meditation has none.
        val session = MindfulnessSession(
            id = "library-session",
            type = MindfulnessSessionType.FREE_MEDITATION,
            status = MindfulnessSessionStatus.COMPLETED,
            sourcePathId = null,
            sourceStepId = "meditation-calm",
            startedAt = Instant.parse("2026-07-29T08:00:00Z"),
            activeDuration = Duration.ofMinutes(5),
            plannedDuration = Duration.ofMinutes(5),
        )

        repository.addSession(session)

        assertEquals(listOf(session), repository.observeSessions().first())
    }

    @Test
    fun pathStepSessionStillSavesAndReadsBack() = runBlocking {
        val session = MindfulnessSession(
            id = "path-session",
            type = MindfulnessSessionType.GUIDED_MEDITATION,
            status = MindfulnessSessionStatus.INTERRUPTED,
            sourcePathId = "first-path",
            sourceStepId = "free-step",
            startedAt = Instant.parse("2026-07-29T09:00:00Z"),
            activeDuration = Duration.ofSeconds(90),
            plannedDuration = Duration.ofMinutes(3),
        )

        repository.addSession(session)

        assertEquals(listOf(session), repository.observeSessions().first())
    }

    @Test
    fun sourcePathWithoutSourceStepIsRejectedBeforePersisting() = runBlocking {
        val session = MindfulnessSession(
            id = "invalid-session",
            type = MindfulnessSessionType.GUIDED_MEDITATION,
            status = MindfulnessSessionStatus.COMPLETED,
            sourcePathId = "first-path",
            sourceStepId = null,
            startedAt = Instant.parse("2026-07-29T10:00:00Z"),
            activeDuration = Duration.ofMinutes(1),
            plannedDuration = Duration.ofMinutes(1),
        )

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repository.addSession(session) }
        }
        assertEquals(emptyList<MindfulnessSession>(), repository.observeSessions().first())
    }
}
