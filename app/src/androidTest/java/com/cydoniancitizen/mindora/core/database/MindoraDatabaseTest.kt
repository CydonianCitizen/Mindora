package com.cydoniancitizen.mindora.core.database

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cydoniancitizen.mindora.core.database.entity.MindfulnessSessionEntity
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSessionStatus
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSessionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MindoraDatabaseTest {
    private lateinit var database: MindoraDatabase

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            MindoraDatabase::class.java,
        ).build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun databaseStartsWithNoSessions() = runBlocking {
        assertTrue(database.mindfulnessSessionDao().observeAll().first().isEmpty())
    }

    @Test
    fun insertedSessionRoundTripsAllFields() = runBlocking {
        val session = session(
            id = "session-id",
            startedAtEpochMillis = 1_767_326_645_123,
        )

        database.mindfulnessSessionDao().insert(session)

        assertEquals(
            session,
            database.mindfulnessSessionDao().observeAll().first().single(),
        )
    }

    @Test
    fun sessionsAreObservedNewestFirstWithIDTieBreak() = runBlocking {
        val dao = database.mindfulnessSessionDao()
        dao.insert(session(id = "a", startedAtEpochMillis = 1_000))
        dao.insert(session(id = "older", startedAtEpochMillis = 500))
        dao.insert(session(id = "b", startedAtEpochMillis = 1_000))

        assertEquals(
            listOf("b", "a", "older"),
            dao.observeAll().first().map { it.id },
        )
    }

    @Test
    fun duplicatePrimaryKeyFailsWithoutReplacing() = runBlocking {
        val dao = database.mindfulnessSessionDao()
        val original = session(id = "duplicate", startedAtEpochMillis = 1_000)
        dao.insert(original)

        try {
            dao.insert(original.copy(activeDurationMillis = 999_999))
            fail("Duplicate primary key insertion should fail.")
        } catch (_: SQLiteConstraintException) {
            // Expected: OnConflictStrategy.ABORT preserves the original row.
        }

        assertEquals(original, dao.observeAll().first().single())
    }

    private fun session(
        id: String,
        startedAtEpochMillis: Long,
    ) = MindfulnessSessionEntity(
        id = id,
        type = MindfulnessSessionType.GUIDED_MEDITATION,
        status = MindfulnessSessionStatus.INTERRUPTED,
        sourcePathId = "path-id",
        sourceStepId = "step-id",
        startedAtEpochMillis = startedAtEpochMillis,
        activeDurationMillis = 123_456,
        plannedDurationMillis = 180_000,
    )
}
