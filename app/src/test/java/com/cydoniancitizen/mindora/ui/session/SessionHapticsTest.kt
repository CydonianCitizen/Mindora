package com.cydoniancitizen.mindora.ui.session

import com.cydoniancitizen.mindora.feature.breathing.BreathingPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionHapticsTest {
    @Test
    fun `breathing grows and recedes with safe edges and exact remaining duration`() {
        val inhale = requireNotNull(breathingHapticWaveform(BreathingPhase.INHALE, 0f, 4_000, 0.4f))
        val exhale = requireNotNull(breathingHapticWaveform(BreathingPhase.EXHALE, 0f, 6_000, 0.4f))
        assertEquals(4_000L, inhale.timings.sum())
        assertEquals(6_000L, exhale.timings.sum())
        assertTrue(inhale.amplitudes[20] < inhale.amplitudes[60])
        assertTrue(exhale.amplitudes[20] > exhale.amplitudes[80])
        for (waveform in listOf(inhale, exhale)) {
            assertEquals(0, waveform.amplitudes.first())
            assertEquals(0, waveform.amplitudes.last())
            assertTrue(waveform.amplitudes.all { it in 0..102 })
        }
        val resumed = requireNotNull(breathingHapticWaveform(BreathingPhase.INHALE, 0.5f, 2_013, 0.4f))
        assertEquals(2_013L, resumed.timings.sum())
        assertTrue(resumed.amplitudes[5] > inhale.amplitudes[5])
        assertNull(breathingHapticWaveform(BreathingPhase.INHALE, 0f, 4_000, 0f))
        assertNull(breathingHapticWaveform(BreathingPhase.EXHALE, 1f, 0, 0.4f))
        assertNull(breathingHapticWaveform(BreathingPhase.HOLD_AFTER_INHALE, 0f, 4_000, 0.4f))
        assertNull(breathingHapticWaveform(BreathingPhase.HOLD_AFTER_EXHALE, 0f, 4_000, 0.4f))
    }

    @Test
    fun `orbit has eight soft pulses and resumes without restarting the cycle`() {
        val full = requireNotNull(meditationHapticWaveform(0, 60_000, 0.4f))
        val pulses = full.amplitudes.toList().zipWithNext().count { (a, b) -> a == 0 && b > 0 }
        assertEquals(8, pulses)
        assertEquals(10_000L, full.timings.sum())
        assertEquals(0.25f, meditationCycleProgress(12_500))
        assertEquals(0f, meditationCycleProgress(20_000))
        val resumed = requireNotNull(meditationHapticWaveform(2_500, 60_000, 0.4f))
        assertEquals(7_500L, resumed.timings.sum())
        // After the short resume fade, both patterns have exactly the same beat and amplitude.
        assertEquals(full.amplitudes.drop(54), resumed.amplitudes.drop(4))
        assertEquals(137L, requireNotNull(meditationHapticWaveform(2_500, 137, 0.4f)).timings.sum())
        assertNull(meditationHapticWaveform(2_500, 0, 0.4f))
        assertNull(meditationHapticWaveform(0, 60_000, 0f))
    }

    @Test
    fun `long phases stay bounded and invalid intensity is rejected`() {
        val long = requireNotNull(breathingHapticWaveform(BreathingPhase.INHALE, 0f, 120_000, 1f))
        assertEquals(10_000L, long.timings.sum())
        for (intensity in listOf(-1f, 2f, Float.NaN)) {
            val failure = runCatching { meditationHapticWaveform(0, 1_000, intensity) }.exceptionOrNull()
            assertTrue(failure is IllegalArgumentException)
        }
    }
}
