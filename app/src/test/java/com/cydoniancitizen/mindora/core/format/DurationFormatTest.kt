package com.cydoniancitizen.mindora.core.format

import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Test

class DurationFormatTest {

    @Test
    fun `remaining formats whole minutes and seconds`() {
        assertEquals("00:01", formatRemaining(Duration.ofSeconds(1)))
        assertEquals("01:00", formatRemaining(Duration.ofMinutes(1)))
        assertEquals("10:00", formatRemaining(Duration.ofMinutes(10)))
    }

    @Test
    fun `remaining rounds up so a countdown never skips its first second`() {
        assertEquals("00:01", formatRemaining(Duration.ofMillis(1)))
        assertEquals("05:00", formatRemaining(Duration.ofMillis(299_001)))
        assertEquals("00:00", formatRemaining(Duration.ZERO))
        assertEquals("00:00", formatRemaining(Duration.ofSeconds(-30)))
    }

    @Test
    fun `elapsed rounds down to the second already passed`() {
        assertEquals("00:00", formatElapsed(Duration.ofMillis(999)))
        assertEquals("00:04", formatElapsed(Duration.ofMillis(4_900)))
        assertEquals("05:00", formatElapsed(Duration.ofMinutes(5)))
    }

    @Test
    fun `elapsed clamps negative durations to zero`() {
        assertEquals("00:00", formatElapsed(Duration.ofSeconds(-1)))
    }

    @Test
    fun `durations past an hour keep counting in minutes`() {
        assertEquals("90:00", formatElapsed(Duration.ofMinutes(90)))
        assertEquals("60:01", formatRemaining(Duration.ofMinutes(60).plusSeconds(1)))
    }
}
