package com.cydoniancitizen.mindora.core.media

import com.cydoniancitizen.mindora.core.session.MindfulnessSessionRepository
import com.cydoniancitizen.mindora.core.session.SessionTimeSource
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSession
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSessionStatus
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSessionType
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackSessionRuntimeTest {
    @Test
    fun `natural end saves one completed session with catalogue identity`() = runTest {
        val repository = FakeRepository()
        val time = FakeTimeSource(elapsedMillis = 1_000)
        val runtime = guidedRuntime(repository, time)
        runtime.setPlaying(true)
        time.elapsedMillis = 8_500

        val result = runtime.finalize(MindfulnessSessionStatus.COMPLETED)
        val duplicate = runtime.finalize(MindfulnessSessionStatus.INTERRUPTED)

        assertTrue(result is PlaybackSaveResult.Saved)
        assertSame(PlaybackSaveResult.Ignored, duplicate)
        val session = repository.saved.single()
        assertEquals("fixed-id", session.id)
        assertEquals(MindfulnessSessionType.GUIDED_MEDITATION, session.type)
        assertEquals(MindfulnessSessionStatus.COMPLETED, session.status)
        assertEquals("path-one", session.sourcePathId)
        assertEquals("guided-one", session.sourceStepId)
        assertEquals(STARTED_AT, session.startedAt)
        assertEquals(Duration.ofMillis(7_500), session.activeDuration)
        assertEquals(Duration.ofMinutes(5), session.plannedDuration)
        assertEquals(1, repository.attempts.size)
    }

    @Test
    fun `early end saves interrupted and excludes paused time`() = runTest {
        val repository = FakeRepository()
        val time = FakeTimeSource(elapsedMillis = 10_000)
        val runtime = guidedRuntime(repository, time)
        runtime.setPlaying(true)
        time.elapsedMillis = 12_000
        runtime.setPlaying(false)
        time.elapsedMillis = 90_000
        runtime.setPlaying(true)
        time.elapsedMillis = 91_250

        runtime.finalize(MindfulnessSessionStatus.INTERRUPTED)

        val session = repository.saved.single()
        assertEquals(MindfulnessSessionStatus.INTERRUPTED, session.status)
        assertEquals(Duration.ofMillis(3_250), session.activeDuration)
    }

    @Test
    fun `player error before active time creates no session`() = runTest {
        val repository = FakeRepository()
        val runtime = guidedRuntime(repository, FakeTimeSource())

        val result = runtime.finalize(MindfulnessSessionStatus.INTERRUPTED)

        assertSame(PlaybackSaveResult.NothingToSave, result)
        assertTrue(repository.attempts.isEmpty())
    }

    @Test
    fun `player error after active time creates interrupted session`() = runTest {
        val repository = FakeRepository()
        val time = FakeTimeSource(elapsedMillis = 50)
        val runtime = guidedRuntime(repository, time)
        runtime.setPlaying(true)
        time.elapsedMillis = 1_050

        runtime.finalize(MindfulnessSessionStatus.INTERRUPTED)

        assertEquals(MindfulnessSessionStatus.INTERRUPTED, repository.saved.single().status)
        assertEquals(Duration.ofSeconds(1), repository.saved.single().activeDuration)
    }

    @Test
    fun `save failure retains exact session and retry reuses UUID`() = runTest {
        val repository = FakeRepository(fail = true)
        val time = FakeTimeSource(elapsedMillis = 1_000)
        val runtime = guidedRuntime(repository, time)
        runtime.setPlaying(true)
        time.elapsedMillis = 2_000

        val failed = runtime.finalize(MindfulnessSessionStatus.COMPLETED)
        runtime.pendingSession
        repository.fail = false
        val retried = runtime.retry()

        assertTrue(failed is PlaybackSaveResult.Failed)
        assertTrue(retried is PlaybackSaveResult.Saved)
        assertEquals(listOf("fixed-id", "fixed-id"), repository.attempts.map { it.id })
        assertEquals("fixed-id", repository.saved.single().id)
        assertNull(runtime.pendingSession)
    }

    @Test
    fun `discard clears failed pending session without saving`() = runTest {
        val repository = FakeRepository(fail = true)
        val time = FakeTimeSource(elapsedMillis = 0)
        val runtime = guidedRuntime(repository, time)
        runtime.setPlaying(true)
        time.elapsedMillis = 1_000
        runtime.finalize(MindfulnessSessionStatus.INTERRUPTED)

        assertTrue(runtime.discard())
        assertFalse(runtime.discard())
        assertNull(runtime.pendingSession)
        assertTrue(repository.saved.isEmpty())
    }

    @Test
    fun `white noise identity saves a standalone completed session`() = runTest {
        val repository = FakeRepository()
        val time = FakeTimeSource(elapsedMillis = 0)
        val runtime = PlaybackSessionRuntime(
            identity = PlaybackSessionIdentity(
                type = MindfulnessSessionType.WHITE_NOISE,
                sourcePathId = null,
                sourceStepId = "white-noise",
                plannedDuration = Duration.ofMinutes(10),
            ),
            startedAt = STARTED_AT,
            timeSource = time,
            repository = repository,
            idFactory = { "fixed-id" },
        )
        runtime.setPlaying(true)
        time.elapsedMillis = Duration.ofMinutes(10).toMillis()

        val result = runtime.finalize(MindfulnessSessionStatus.COMPLETED)
        val duplicate = runtime.finalize(MindfulnessSessionStatus.COMPLETED)

        assertTrue(result is PlaybackSaveResult.Saved)
        assertSame(PlaybackSaveResult.Ignored, duplicate)
        val session = repository.saved.single()
        assertEquals(MindfulnessSessionType.WHITE_NOISE, session.type)
        assertNull(session.sourcePathId)
        assertEquals("white-noise", session.sourceStepId)
        assertEquals(Duration.ofMinutes(10), session.activeDuration)
        assertEquals(Duration.ofMinutes(10), session.plannedDuration)
        assertEquals(1, repository.attempts.size)
    }

    private fun guidedRuntime(
        repository: FakeRepository,
        time: FakeTimeSource,
    ) = PlaybackSessionRuntime(
        identity = PlaybackSessionIdentity(
            type = MindfulnessSessionType.GUIDED_MEDITATION,
            sourcePathId = "path-one",
            sourceStepId = "guided-one",
            plannedDuration = Duration.ofMinutes(5),
        ),
        startedAt = STARTED_AT,
        timeSource = time,
        repository = repository,
        idFactory = { "fixed-id" },
    )

    private class FakeTimeSource(
        var instant: Instant = STARTED_AT,
        var elapsedMillis: Long = 0L,
    ) : SessionTimeSource {
        override fun nowInstant(): Instant = instant
        override fun elapsedRealtimeMillis(): Long = elapsedMillis
    }

    private class FakeRepository(var fail: Boolean = false) : MindfulnessSessionRepository {
        val attempts = mutableListOf<MindfulnessSession>()
        val saved = mutableListOf<MindfulnessSession>()

        override fun observeSessions(): Flow<List<MindfulnessSession>> = flowOf(saved)

        override suspend fun addSession(session: MindfulnessSession) {
            attempts += session
            if (fail) error("save failed")
            saved += session
        }
    }

    private companion object {
        val STARTED_AT: Instant = Instant.parse("2026-07-29T08:00:00Z")
    }
}
