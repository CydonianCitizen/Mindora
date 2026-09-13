package com.cydoniancitizen.mindora.feature.freemeditation

import com.cydoniancitizen.mindora.core.content.model.MeditationStep
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StepCueTest {
    private val planned: Duration = Duration.ofMinutes(4)
    private val openSteps = listOf("first", "second", "third", "fourth").map(::MeditationStep)

    private fun List<MeditationStep>.cueAfter(elapsed: Duration): String? =
        stepCue(planned.minus(elapsed), planned)

    @Test
    fun `steps without a stated length share the session equally`() {
        assertEquals("first", openSteps.cueAfter(Duration.ZERO))
        assertEquals("first", openSteps.cueAfter(Duration.ofSeconds(59)))
        assertEquals("second", openSteps.cueAfter(Duration.ofMinutes(1)))
        assertEquals("third", openSteps.cueAfter(Duration.ofMinutes(2)))
        assertEquals("fourth", openSteps.cueAfter(Duration.ofMinutes(3)))
    }

    @Test
    fun `a stated length is kept and the rest share what is left`() {
        val steps = listOf(
            MeditationStep("three minutes of breath", durationSeconds = 180),
            MeditationStep("then this"),
            MeditationStep("and this"),
        )

        assertEquals("three minutes of breath", steps.cueAfter(Duration.ofSeconds(179)))
        // Three of the four minutes are spoken for, so the two open steps take thirty seconds each.
        assertEquals("then this", steps.cueAfter(Duration.ofMinutes(3)))
        assertEquals("and this", steps.cueAfter(Duration.ofSeconds(210)))
    }

    @Test
    fun `the last step stays until the session ends`() {
        assertEquals("fourth", openSteps.cueAfter(planned))
        assertEquals("fourth", openSteps.cueAfter(planned.plusMinutes(1)))
    }

    @Test
    fun `nothing to say without steps or without a duration`() {
        assertNull(emptyList<MeditationStep>().stepCue(Duration.ofMinutes(1), planned))
        assertNull(openSteps.stepCue(Duration.ZERO, Duration.ZERO))
    }
}
