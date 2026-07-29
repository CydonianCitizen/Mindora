package com.cydoniancitizen.mindora.feature.breathing

import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BreathingTimelineTest {
    private val config = ProductionBreathingExerciseConfig

    @Test
    fun `start is cycle one inhale with correct remaining time`() {
        val state = activeAt(Duration.ZERO)

        assertEquals(BreathingPhase.INHALE, state.phase)
        assertEquals(1, state.cycle)
        assertEquals(Duration.ofSeconds(4), state.phaseRemainingDuration)
        assertEquals(Duration.ofSeconds(50), state.totalRemainingDuration)
        assertEquals(0f, state.phaseProgress)
    }

    @Test
    fun `four seconds skips zero hold and reaches exhale`() {
        val state = activeAt(Duration.ofSeconds(4))

        assertEquals(BreathingPhase.EXHALE, state.phase)
        assertEquals(1, state.cycle)
        assertEquals(Duration.ofSeconds(6), state.phaseRemainingDuration)
        assertEquals(0f, state.phaseProgress)
    }

    @Test
    fun `ten seconds reaches cycle two inhale`() {
        val state = activeAt(Duration.ofSeconds(10))

        assertEquals(BreathingPhase.INHALE, state.phase)
        assertEquals(2, state.cycle)
        assertEquals(Duration.ofSeconds(4), state.phaseRemainingDuration)
    }

    @Test
    fun `delayed recalculation crosses phases and cycles directly`() {
        val withinFourthCycle = activeAt(Duration.ofMillis(36_500))

        assertEquals(BreathingPhase.EXHALE, withinFourthCycle.phase)
        assertEquals(4, withinFourthCycle.cycle)
        assertEquals(Duration.ofMillis(3_500), withinFourthCycle.phaseRemainingDuration)
        assertEquals(Duration.ofMillis(13_500), withinFourthCycle.totalRemainingDuration)
        assertEquals(0.41666666f, withinFourthCycle.phaseProgress, 0.0001f)
    }

    @Test
    fun `cycle and progress stay within display bounds`() {
        listOf(0L, 4_000L, 10_000L, 39_999L, 49_999L).forEach { millis ->
            val state = activeAt(Duration.ofMillis(millis))
            assertTrue(state.cycle in 1..config.cycles)
            assertTrue(state.phaseProgress in 0f..1f)
        }
    }

    @Test
    fun `completion clamps at planned duration and never returns active`() {
        listOf(Duration.ofSeconds(50), Duration.ofSeconds(75)).forEach { elapsed ->
            val state = calculateBreathingTimeline(config, elapsed)
            assertTrue(state is BreathingTimeline.Complete)
            assertEquals(Duration.ofSeconds(50), state.activeDuration)
            assertEquals(Duration.ZERO, state.totalRemainingDuration)
            assertEquals(5, state.completedCycles)
        }
    }

    @Test
    fun `zero duration leading phases are skipped`() {
        val exhaleOnly = BreathingExerciseConfig(
            inhaleDuration = Duration.ZERO,
            holdAfterInhaleDuration = Duration.ZERO,
            exhaleDuration = Duration.ofSeconds(2),
            holdAfterExhaleDuration = Duration.ZERO,
            cycles = 1,
        )

        val state = calculateBreathingTimeline(exhaleOnly, Duration.ZERO)
            as BreathingTimeline.Active

        assertEquals(BreathingPhase.EXHALE, state.phase)
        assertEquals(Duration.ofSeconds(2), state.phaseRemainingDuration)
    }

    private fun activeAt(elapsed: Duration): BreathingTimeline.Active {
        val state = calculateBreathingTimeline(config, elapsed)
        assertTrue("Expected active timeline, was $state", state is BreathingTimeline.Active)
        return state as BreathingTimeline.Active
    }
}
