package com.cydoniancitizen.mindora.core.database.entity

import com.cydoniancitizen.mindora.core.session.model.MindfulnessSession
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSessionStatus
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSessionType
import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class MindfulnessSessionMappingTest {
    @Test
    fun `domain session maps to millisecond entity`() {
        val entity = session.toEntity()

        assertEquals("session-id", entity.id)
        assertEquals(MindfulnessSessionType.BREATHING_EXERCISE, entity.type)
        assertEquals(MindfulnessSessionStatus.INTERRUPTED, entity.status)
        assertEquals("path-id", entity.sourcePathId)
        assertEquals("step-id", entity.sourceStepId)
        assertEquals(1_767_323_045_123, entity.startedAtEpochMillis)
        assertEquals(123_456L, entity.activeDurationMillis)
        assertEquals(180_000L, entity.plannedDurationMillis)
    }

    @Test
    fun `entity maps to domain session`() {
        assertEquals(session, session.toEntity().toDomain())
    }

    private companion object {
        val session = MindfulnessSession(
            id = "session-id",
            type = MindfulnessSessionType.BREATHING_EXERCISE,
            status = MindfulnessSessionStatus.INTERRUPTED,
            sourcePathId = "path-id",
            sourceStepId = "step-id",
            startedAt = Instant.parse("2026-01-02T03:04:05.123Z"),
            activeDuration = Duration.ofMillis(123_456),
            plannedDuration = Duration.ofMinutes(3),
        )
    }
}
