package com.cydoniancitizen.mindora.feature.breathing

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.DpRect
import com.cydoniancitizen.mindora.ui.theme.MindoraTheme
import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test

class BreathingExerciseLayoutTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun createRunningState(
        phase: BreathingPhase = BreathingPhase.INHALE,
        phaseProgress: Float = 0f,
        phaseRemainingDuration: Duration = Duration.ofSeconds(4),
    ) = BreathingExerciseUiState.Running(
        startedAt = Instant.EPOCH,
        accumulatedActiveDuration = Duration.ZERO,
        resumedAtElapsedRealtimeMillis = 0L,
        currentPhase = phase,
        currentCycle = 1,
        totalCycles = 5,
        phaseRemainingDuration = phaseRemainingDuration,
        totalRemainingDuration = Duration.ofSeconds(50),
        activeDuration = Duration.ZERO,
        phaseProgress = phaseProgress,
    )

    private fun createPausedState() = BreathingExerciseUiState.Paused(
        startedAt = Instant.EPOCH,
        currentPhase = BreathingPhase.INHALE,
        currentCycle = 1,
        totalCycles = 5,
        phaseRemainingDuration = Duration.ofSeconds(4),
        totalRemainingDuration = Duration.ofSeconds(50),
        activeDuration = Duration.ofSeconds(10),
        phaseProgress = 0.5f,
    )

    /**
     * Shows the screen once and hands back the state driving it.
     *
     * The rule accepts a single `setContent` for the whole test, so comparing two states means
     * changing the state the screen already reads, never composing the screen a second time.
     */
    private fun showBreathingExercise(
        initialState: BreathingExerciseUiState,
        onPause: () -> Unit = {},
        onRequestEnd: () -> Unit = {},
    ): MutableState<BreathingExerciseUiState> {
        val state = mutableStateOf(initialState)
        composeRule.setContent {
            MindoraTheme {
                BreathingExerciseScreen(
                    uiState = state.value,
                    onBack = {},
                    onStart = {},
                    onPause = onPause,
                    onResume = {},
                    onRequestEnd = onRequestEnd,
                    onDismissEnd = {},
                    onConfirmEnd = {},
                    onRetrySave = {},
                    onDiscard = {},
                    onDone = {},
                )
            }
        }
        return state
    }

    /**
     * Every lookup here reads the unmerged tree.
     *
     * The circle, the phase label and the countdown live inside the `clearAndSetSemantics` block
     * that composes one spoken summary for TalkBack, so in the merged tree they do not exist at
     * all — their test tags are only reachable unmerged.
     */
    private fun boundsOf(tag: String): DpRect =
        composeRule.onNodeWithTag(tag, useUnmergedTree = true).getUnclippedBoundsInRoot()

    private fun circleVisualScale(): Float? =
        composeRule.onNodeWithTag(BreathingTestTags.CIRCLE, useUnmergedTree = true)
            .fetchSemanticsNode()
            .config[VisualScaleSemanticsKey]

    @Test
    fun layoutBoundsRemainUnchangedWhenCircleProgressChanges() {
        val state = showBreathingExercise(createRunningState(phaseProgress = 0f))

        val containerBoundsMin = boundsOf(BreathingTestTags.CONTAINER)
        val phaseTextBoundsMin = boundsOf(BreathingTestTags.PHASE_TEXT)
        val countdownBoundsMin = boundsOf(BreathingTestTags.COUNTDOWN_TEXT)
        val primaryButtonBoundsMin = boundsOf(BreathingTestTags.PRIMARY_BUTTON)
        val endButtonBoundsMin = boundsOf(BreathingTestTags.END_BUTTON)
        val scaleMin = circleVisualScale()

        // Advance progress to 1f (max scale)
        state.value = createRunningState(phaseProgress = 1f)
        composeRule.waitForIdle()

        val containerBoundsMax = boundsOf(BreathingTestTags.CONTAINER)
        val phaseTextBoundsMax = boundsOf(BreathingTestTags.PHASE_TEXT)
        val countdownBoundsMax = boundsOf(BreathingTestTags.COUNTDOWN_TEXT)
        val primaryButtonBoundsMax = boundsOf(BreathingTestTags.PRIMARY_BUTTON)
        val endButtonBoundsMax = boundsOf(BreathingTestTags.END_BUTTON)
        val scaleMax = circleVisualScale()

        // Verify bounds are identical
        assertEquals("Container bounds must not change", containerBoundsMin, containerBoundsMax)
        assertEquals("Phase text bounds must not change", phaseTextBoundsMin, phaseTextBoundsMax)
        assertEquals("Countdown bounds must not change", countdownBoundsMin, countdownBoundsMax)
        assertEquals("Primary button bounds must not change", primaryButtonBoundsMin, primaryButtonBoundsMax)
        assertEquals("End button bounds must not change", endButtonBoundsMin, endButtonBoundsMax)

        // Verify visual scales are different
        assertNotEquals("Visual circle scale must change with phase progress", scaleMin, scaleMax)
    }

    @Test
    fun inhaleAndExhaleProduceDifferentVisualScales() {
        val state = showBreathingExercise(
            createRunningState(phase = BreathingPhase.INHALE, phaseProgress = 0.8f),
        )
        val inhaleScale = circleVisualScale()

        state.value = createRunningState(phase = BreathingPhase.EXHALE, phaseProgress = 0.8f)
        composeRule.waitForIdle()
        val exhaleScale = circleVisualScale()

        assertNotEquals("Inhale and exhale at same progress produce different visual scale", inhaleScale, exhaleScale)
    }

    @Test
    fun circleIsCenteredInFixedContainer() {
        // Measured at full openness on purpose. getUnclippedBoundsInRoot moves the origin by the
        // graphics layer's scale but reports the untransformed size, so at any smaller scale the
        // two disagree by half the shrinkage and the comparison measures that, not the centring.
        showBreathingExercise(createRunningState(phaseProgress = 1f))

        val containerBounds = boundsOf(BreathingTestTags.CONTAINER)
        val circleBounds = boundsOf(BreathingTestTags.CIRCLE)

        val containerCenterX = (containerBounds.left + containerBounds.right) / 2
        val containerCenterY = (containerBounds.top + containerBounds.bottom) / 2

        val circleCenterX = (circleBounds.left + circleBounds.right) / 2
        val circleCenterY = (circleBounds.top + circleBounds.bottom) / 2

        val where = "container=$containerBounds circle=$circleBounds"
        assertEquals("Circle center X matches container center X, $where", containerCenterX.value, circleCenterX.value, 0.5f)
        assertEquals("Circle center Y matches container center Y, $where", containerCenterY.value, circleCenterY.value, 0.5f)
    }

    @Test
    fun pauseResumeAndEndActionsWorkAsExpected() {
        var pauseCalled = false
        var endCalled = false

        showBreathingExercise(
            createRunningState(),
            onPause = { pauseCalled = true },
            onRequestEnd = { endCalled = true },
        )

        composeRule.onNodeWithTag(BreathingTestTags.PRIMARY_BUTTON).performClick()
        assertEquals(true, pauseCalled)

        composeRule.onNodeWithTag(BreathingTestTags.END_BUTTON).performClick()
        assertEquals(true, endCalled)
    }

    @Test
    fun pausedStateDisplaysResumeButtonInSameBounds() {
        val state = showBreathingExercise(createRunningState())
        val runningButtonBounds = boundsOf(BreathingTestTags.PRIMARY_BUTTON)

        state.value = createPausedState()
        composeRule.waitForIdle()
        val pausedButtonBounds = boundsOf(BreathingTestTags.PRIMARY_BUTTON)

        assertEquals("Primary action button bounds remain stable between running and paused states", runningButtonBounds, pausedButtonBounds)
        composeRule.onNodeWithText("Resume").assertIsDisplayed()
    }
}
