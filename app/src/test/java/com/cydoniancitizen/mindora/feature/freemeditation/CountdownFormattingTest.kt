package com.cydoniancitizen.mindora.feature.freemeditation

import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Test

class CountdownFormattingTest {
    @Test
    fun `formats countdown values`() {
        assertEquals("00:01", formatCountdown(Duration.ofSeconds(1)))
        assertEquals("01:00", formatCountdown(Duration.ofMinutes(1)))
        assertEquals("10:00", formatCountdown(Duration.ofMinutes(10)))
    }

    @Test
    fun `rounds positive subsecond remainder up`() {
        assertEquals("00:01", formatCountdown(Duration.ofMillis(1)))
        assertEquals("00:00", formatCountdown(Duration.ZERO))
    }
}
