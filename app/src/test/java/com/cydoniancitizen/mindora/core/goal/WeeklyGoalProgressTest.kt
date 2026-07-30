package com.cydoniancitizen.mindora.core.goal

import com.cydoniancitizen.mindora.core.session.model.MindfulnessSession
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSessionStatus
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSessionType
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeeklyGoalProgressTest {
    @Test
    fun `no goal is unconfigured with zero fraction`() {
        val progress = progress(goalMinutes = null, sessions = listOf(session("one", 5)))

        assertFalse(progress.isConfigured)
        assertFalse(progress.isReached)
        assertEquals(0f, progress.fraction)
        assertEquals(Duration.ofMinutes(5), progress.practicedDuration)
    }

    @Test
    fun `no sessions produces zero practiced duration`() {
        val progress = progress(goalMinutes = 60)

        assertEquals(Duration.ZERO, progress.practicedDuration)
        assertEquals(0f, progress.fraction)
        assertFalse(progress.isReached)
    }

    @Test
    fun `completed interrupted standalone and path sessions all count`() {
        val sessions = listOf(
            session("completed", 10, MindfulnessSessionStatus.COMPLETED),
            session("interrupted", 15, MindfulnessSessionStatus.INTERRUPTED),
            session("path", 20, sourcePathId = "path", sourceStepId = "step"),
        )

        assertEquals(Duration.ofMinutes(45), progress(60, sessions).practicedDuration)
    }

    @Test
    fun `sessions outside current week are ignored and duplicate records each count`() {
        val sessions = listOf(
            session("before", 50, startedAt = "2026-07-26T23:59:59Z"),
            session("first", 10),
            session("second", 10),
            session("after", 50, startedAt = "2026-08-03T00:00:00Z"),
        )

        assertEquals(Duration.ofMinutes(20), progress(60, sessions).practicedDuration)
    }

    @Test
    fun `exact threshold reaches goal but one millisecond below does not`() {
        val exact = session("exact", duration = Duration.ofMinutes(60))
        val below = session("below", duration = Duration.ofMinutes(60).minusMillis(1))

        assertTrue(progress(60, listOf(exact)).isReached)
        assertFalse(progress(60, listOf(below)).isReached)
    }

    @Test
    fun `fraction is correct and clamped`() {
        assertEquals(0.5f, progress(60, listOf(session("half", 30))).fraction)
        assertEquals(1f, progress(60, listOf(session("over", 90))).fraction)
    }

    @Test
    fun `first day input changes current week boundaries`() {
        val sunday = session("sunday", 10, startedAt = "2026-07-26T12:00:00Z")

        assertEquals(Duration.ZERO, progress(60, listOf(sunday)).practicedDuration)
        assertEquals(
            Duration.ofMinutes(10),
            progress(60, listOf(sunday), firstDay = DayOfWeek.SUNDAY).practicedDuration,
        )
    }

    @Test
    fun `time zone uses local started-at date`() {
        val lateSundayUtc = session(
            id = "zone",
            minutes = 10,
            startedAt = "2026-07-26T23:30:00Z",
        )
        val mondayNow = Instant.parse("2026-07-27T12:00:00Z")

        assertEquals(
            Duration.ofMinutes(10),
            progress(60, listOf(lateSundayUtc), mondayNow, ZoneId.of("Europe/Rome"))
                .practicedDuration,
        )
        assertEquals(
            Duration.ZERO,
            progress(60, listOf(lateSundayUtc), mondayNow, ZoneId.of("UTC"))
                .practicedDuration,
        )
    }

    @Test
    fun `session duration belongs entirely to starting week`() {
        val startsThisWeek = session(
            id = "crosses-end",
            duration = Duration.ofDays(8),
            startedAt = "2026-08-02T23:59:00Z",
        )
        val startsPreviousWeek = session(
            id = "crosses-start",
            duration = Duration.ofDays(8),
            startedAt = "2026-07-26T23:59:00Z",
        )

        assertEquals(
            Duration.ofDays(8),
            progress(120, listOf(startsThisWeek, startsPreviousWeek)).practicedDuration,
        )
    }

    private fun progress(
        goalMinutes: Int?,
        sessions: List<MindfulnessSession> = emptyList(),
        now: Instant = NOW,
        zoneId: ZoneId = ZoneId.of("UTC"),
        firstDay: DayOfWeek = DayOfWeek.MONDAY,
    ) = calculateWeeklyGoalProgress(sessions, goalMinutes, now, zoneId, firstDay)

    private fun session(
        id: String,
        minutes: Long,
        status: MindfulnessSessionStatus = MindfulnessSessionStatus.COMPLETED,
        startedAt: String = "2026-07-29T12:00:00Z",
        sourcePathId: String? = null,
        sourceStepId: String? = null,
    ) = session(
        id = id,
        duration = Duration.ofMinutes(minutes),
        status = status,
        startedAt = startedAt,
        sourcePathId = sourcePathId,
        sourceStepId = sourceStepId,
    )

    private fun session(
        id: String,
        duration: Duration,
        status: MindfulnessSessionStatus = MindfulnessSessionStatus.COMPLETED,
        startedAt: String = "2026-07-29T12:00:00Z",
        sourcePathId: String? = null,
        sourceStepId: String? = null,
    ) = MindfulnessSession(
        id = id,
        type = MindfulnessSessionType.FREE_MEDITATION,
        status = status,
        sourcePathId = sourcePathId,
        sourceStepId = sourceStepId,
        startedAt = Instant.parse(startedAt),
        activeDuration = duration,
        plannedDuration = null,
    )

    private companion object {
        val NOW: Instant = Instant.parse("2026-07-29T12:00:00Z")
    }
}
