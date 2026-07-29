package com.cydoniancitizen.mindora.core.session.data

import com.cydoniancitizen.mindora.core.session.model.MindfulnessSession
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSessionStatus
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSessionType
import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertThrows
import org.junit.Test

class MindfulnessSessionValidationTest {
    @Test
    fun `valid session passes validation`() {
        validateForPersistence(validSession)
        validateForPersistence(
            validSession.copy(
                type = MindfulnessSessionType.FREE_MEDITATION,
                sourcePathId = null,
                sourceStepId = null,
                plannedDuration = null,
            ),
        )
    }

    @Test
    fun `blank and padded session IDs are rejected`() {
        listOf("", " ", " session-id", "session-id ").forEach { id ->
            assertInvalid(validSession.copy(id = id))
        }
    }

    @Test
    fun `zero and negative active durations are rejected`() {
        listOf(Duration.ZERO, Duration.ofMillis(-1)).forEach { duration ->
            assertInvalid(validSession.copy(activeDuration = duration))
        }
    }

    @Test
    fun `zero and negative planned durations are rejected`() {
        listOf(Duration.ZERO, Duration.ofMillis(-1)).forEach { duration ->
            assertInvalid(validSession.copy(plannedDuration = duration))
        }
    }

    @Test
    fun `blank and padded source IDs are rejected`() {
        listOf("", " ", " path", "path ").forEach { sourcePathId ->
            assertInvalid(validSession.copy(sourcePathId = sourcePathId))
        }
        listOf("", " ", " step", "step ").forEach { sourceStepId ->
            assertInvalid(validSession.copy(sourceStepId = sourceStepId))
        }
    }

    @Test
    fun `source step without source path is rejected`() {
        assertInvalid(
            validSession.copy(
                sourcePathId = null,
                sourceStepId = "step-id",
            ),
        )
    }

    private fun assertInvalid(session: MindfulnessSession) {
        assertThrows(IllegalArgumentException::class.java) {
            validateForPersistence(session)
        }
    }

    private companion object {
        val validSession = MindfulnessSession(
            id = "session-id",
            type = MindfulnessSessionType.GUIDED_MEDITATION,
            status = MindfulnessSessionStatus.COMPLETED,
            sourcePathId = "path-id",
            sourceStepId = "step-id",
            startedAt = Instant.parse("2026-01-02T03:04:05Z"),
            activeDuration = Duration.ofMinutes(5),
            plannedDuration = Duration.ofMinutes(6),
        )
    }
}
