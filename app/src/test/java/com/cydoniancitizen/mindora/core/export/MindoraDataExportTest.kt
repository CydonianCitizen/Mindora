package com.cydoniancitizen.mindora.core.export

import com.cydoniancitizen.mindora.core.preferences.model.MindoraPreferences
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSession
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSessionStatus
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSessionType
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MindoraDataExportTest {

    private val reader = Json { ignoreUnknownKeys = false }

    private val guidedSession = MindfulnessSession(
        id = "session-1",
        type = MindfulnessSessionType.GUIDED_MEDITATION,
        status = MindfulnessSessionStatus.COMPLETED,
        sourcePathId = "path-1",
        sourceStepId = "step-1",
        startedAt = Instant.parse("2026-09-05T07:30:00Z"),
        activeDuration = Duration.ofMinutes(10),
        plannedDuration = Duration.ofMinutes(12),
    )

    private val freeSession = MindfulnessSession(
        id = "session-2",
        type = MindfulnessSessionType.FREE_MEDITATION,
        status = MindfulnessSessionStatus.INTERRUPTED,
        sourcePathId = null,
        sourceStepId = null,
        startedAt = Instant.parse("2026-09-05T19:00:00Z"),
        activeDuration = Duration.ofMillis(1_999),
        plannedDuration = null,
    )

    private fun export(
        preferences: MindoraPreferences = MindoraPreferences(),
        sessions: List<MindfulnessSession> = emptyList(),
    ): ExportDocument = reader.decodeFromString(
        buildExportJson(
            appVersionName = "1.0.5",
            appVersionCode = 3,
            exportedAt = Instant.parse("2026-09-05T20:15:00Z"),
            preferences = preferences,
            sessions = sessions,
        ),
    )

    @Test
    fun `document names the app, its version and when it was written`() {
        val document = export()

        assertEquals("Mindora", document.application)
        assertEquals(2, document.format)
        assertEquals("1.0.5", document.appVersionName)
        assertEquals(3, document.appVersionCode)
        assertEquals("2026-09-05T20:15:00Z", document.exportedAt)
    }

    @Test
    fun `preferences are carried across`() {
        val document = export(
            preferences = MindoraPreferences(
                weeklyGoalMinutes = 60,
                dailyReminderEnabled = true,
                dailyReminderTime = LocalTime.of(7, 45),
            ),
        )

        assertEquals(60, document.preferences.weeklyGoalMinutes)
        assertEquals(true, document.preferences.dailyReminderEnabled)
        assertEquals("07:45", document.preferences.dailyReminderTime)
    }

    @Test
    fun `every session is exported in order, with its durations in milliseconds`() {
        val document = export(sessions = listOf(guidedSession, freeSession))

        assertEquals(listOf("session-1", "session-2"), document.sessions.map { it.id })

        val guided = document.sessions.first()
        assertEquals("GUIDED_MEDITATION", guided.type)
        assertEquals("COMPLETED", guided.status)
        assertEquals("path-1", guided.sourcePathId)
        assertEquals("2026-09-05T07:30:00Z", guided.startedAt)
        assertEquals(600_000L, guided.activeDurationMillis)
        assertEquals(720_000L, guided.plannedDurationMillis)
    }

    @Test
    fun `sub-second and fractional durations survive the export`() {
        val document = export(
            sessions = listOf(
                freeSession,
                freeSession.copy(id = "session-3", activeDuration = Duration.ofMillis(500)),
            ),
        )

        assertEquals(listOf(1_999L, 500L), document.sessions.map { it.activeDurationMillis })
    }

    @Test
    fun `values the app never recorded stay null instead of disappearing`() {
        val document = export(sessions = listOf(freeSession))

        assertNull(document.preferences.weeklyGoalMinutes)
        assertNull(document.sessions.single().sourcePathId)
        assertNull(document.sessions.single().sourceStepId)
        assertNull(document.sessions.single().plannedDurationMillis)
    }

    @Test
    fun `an empty history still produces a readable document`() {
        val raw = buildExportJson(
            appVersionName = "1.0.5",
            appVersionCode = 3,
            exportedAt = Instant.EPOCH,
            preferences = MindoraPreferences(),
            sessions = emptyList(),
        )

        assertTrue("export should be pretty printed", raw.contains("\n"))
        assertEquals(emptyList<ExportSession>(), reader.decodeFromString<ExportDocument>(raw).sessions)
    }

    @Test
    fun `the suggested file name carries the export date`() {
        assertEquals(
            "mindora-data-2026-09-05.json",
            defaultExportFileName(LocalDate.of(2026, 9, 5)),
        )
    }
}
