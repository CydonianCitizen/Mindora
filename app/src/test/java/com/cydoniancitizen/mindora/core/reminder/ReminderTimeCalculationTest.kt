package com.cydoniancitizen.mindora.core.reminder

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderTimeCalculationTest {
    @Test
    fun `later time returns today`() {
        assertEquals(
            Instant.parse("2026-07-30T18:00:00Z"),
            calculateNextReminderInstant(
                Instant.parse("2026-07-30T12:00:00Z"),
                LocalTime.of(20, 0),
                ZoneId.of("Europe/Rome"),
            ),
        )
    }

    @Test
    fun `passed and equal times return future occurrence tomorrow`() {
        val zone = ZoneId.of("UTC")
        val passed = calculateNextReminderInstant(
            Instant.parse("2026-07-30T21:00:00Z"),
            LocalTime.of(20, 0),
            zone,
        )
        val equal = calculateNextReminderInstant(
            Instant.parse("2026-07-30T20:00:00Z"),
            LocalTime.of(20, 0),
            zone,
        )

        assertEquals(Instant.parse("2026-07-31T20:00:00Z"), passed)
        assertEquals(Instant.parse("2026-07-31T20:00:00Z"), equal)
    }

    @Test
    fun `time zone and date rollover are respected`() {
        val now = Instant.parse("2026-07-30T22:30:00Z")

        assertEquals(
            Instant.parse("2026-07-31T18:00:00Z"),
            calculateNextReminderInstant(now, LocalTime.of(20, 0), ZoneId.of("Europe/Rome")),
        )
        assertEquals(
            Instant.parse("2026-07-31T20:00:00Z"),
            calculateNextReminderInstant(now, LocalTime.of(20, 0), ZoneId.of("UTC")),
        )
    }

    @Test
    fun `daylight saving gap still returns future instant`() {
        val now = Instant.parse("2026-03-29T00:00:00Z")
        val result = calculateNextReminderInstant(
            now,
            LocalTime.of(2, 30),
            ZoneId.of("Europe/Rome"),
        )

        assertTrue(result.isAfter(now))
        assertEquals(Instant.parse("2026-03-29T01:30:00Z"), result)
    }
}
