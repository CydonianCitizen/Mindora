package com.cydoniancitizen.mindora.core.media

import java.time.Duration
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionCountdownTest {
    @Test
    fun `fires once after the duration elapses`() = runTest {
        var fired = 0
        val countdown = SessionCountdown { fired++ }

        countdown.start(this, Duration.ofMinutes(10))
        assertTrue(countdown.isRunning)

        advanceTimeBy(Duration.ofMinutes(10).toMillis())
        runCurrent()

        assertEquals(1, fired)
        assertFalse(countdown.isRunning)
    }

    @Test
    fun `does not fire before the duration elapses`() = runTest {
        var fired = 0
        val countdown = SessionCountdown { fired++ }

        countdown.start(this, Duration.ofMinutes(10))
        advanceTimeBy(Duration.ofMinutes(9).toMillis())
        runCurrent()

        assertEquals(0, fired)
    }

    @Test
    fun `cancel before expiry stops it firing`() = runTest {
        var fired = 0
        val countdown = SessionCountdown { fired++ }

        countdown.start(this, Duration.ofMinutes(10))
        advanceTimeBy(Duration.ofMinutes(5).toMillis())
        countdown.cancel()
        advanceTimeBy(Duration.ofMinutes(30).toMillis())
        runCurrent()

        assertEquals(0, fired)
        assertFalse(countdown.isRunning)
    }

    @Test
    fun `restart replaces the pending expiry`() = runTest {
        var fired = 0
        val countdown = SessionCountdown { fired++ }

        countdown.start(this, Duration.ofMinutes(10))
        advanceTimeBy(Duration.ofMinutes(4).toMillis())
        countdown.start(this, Duration.ofMinutes(10))
        advanceTimeBy(Duration.ofMinutes(9).toMillis())
        runCurrent()
        assertEquals(0, fired)

        advanceTimeBy(Duration.ofMinutes(1).toMillis())
        runCurrent()
        assertEquals(1, fired)
    }

    @Test
    fun `a non-positive duration fires immediately`() = runTest {
        var fired = 0
        val countdown = SessionCountdown { fired++ }

        countdown.start(this, Duration.ZERO)

        assertEquals(1, fired)
        assertFalse(countdown.isRunning)
    }
}
