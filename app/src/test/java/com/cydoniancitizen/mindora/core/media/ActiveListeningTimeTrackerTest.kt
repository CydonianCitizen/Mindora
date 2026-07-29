package com.cydoniancitizen.mindora.core.media

import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Test

class ActiveListeningTimeTrackerTest {
    @Test
    fun `time begins only when actual playback becomes active`() {
        val clock = FakeElapsedClock()
        val tracker = ActiveListeningTimeTracker(clock::read)

        clock.now = 4_000
        assertEquals(Duration.ZERO, tracker.duration())
        tracker.setPlaying(true)
        clock.now = 5_250

        assertEquals(Duration.ofMillis(1_250), tracker.duration())
    }

    @Test
    fun `buffering pause and focus suppression are excluded`() {
        val clock = FakeElapsedClock(1_000)
        val tracker = ActiveListeningTimeTracker(clock::read)

        tracker.setPlaying(true)
        clock.now = 2_000
        tracker.setPlaying(false)
        clock.now = 12_000
        tracker.setPlaying(false)
        clock.now = 22_000
        tracker.setPlaying(true)
        clock.now = 22_500
        tracker.setPlaying(false)

        assertEquals(Duration.ofMillis(1_500), tracker.duration())
    }

    @Test
    fun `multiple segments accumulate from delayed monotonic callbacks`() {
        val clock = FakeElapsedClock(100)
        val tracker = ActiveListeningTimeTracker(clock::read)

        tracker.setPlaying(true)
        clock.now = 2_600
        tracker.setPlaying(false)
        clock.now = 100_000
        tracker.setPlaying(true)
        clock.now = 104_250
        tracker.setPlaying(false)

        assertEquals(Duration.ofMillis(6_750), tracker.duration())
    }

    @Test
    fun `duration stops after finalization`() {
        val clock = FakeElapsedClock(5_000)
        val tracker = ActiveListeningTimeTracker(clock::read)
        tracker.setPlaying(true)
        clock.now = 7_000

        assertEquals(Duration.ofSeconds(2), tracker.finish())
        clock.now = 70_000
        tracker.setPlaying(true)

        assertEquals(Duration.ofSeconds(2), tracker.duration())
        assertEquals(Duration.ofSeconds(2), tracker.finish())
    }

    private class FakeElapsedClock(var now: Long = 0L) {
        fun read(): Long = now
    }
}
