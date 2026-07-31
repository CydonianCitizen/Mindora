package com.cydoniancitizen.mindora.feature.breathing

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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

    @Test
    fun layoutBoundsRemainUnchangedWhenCircleProgressChanges() {
        var currentState = createRunningState(phaseProgress = 0f)

        composeRule.setContent {
            MindoraTheme {
                BreathingExerciseScreen(
                    uiState = currentState,
                    onBack = {},
                    onStart = {},
                    onPause = {},
                    onResume = {},
                    onRequestEnd = {},
                    onDismissEnd = {},
                    onConfirmEnd = {},
                    onRetrySave = {},
                    onDiscard = {},
                    onDone = {},
                )
            }
        }

        val containerBoundsMin = composeRule.onNodeWithTag(BreathingTestTags.CONTAINER).getUnclippedBoundsInRoot()
        val phaseTextBoundsMin = composeRule.onNodeWithTag(BreathingTestTags.PHASE_TEXT).getUnclippedBoundsInRoot()
        val countdownBoundsMin = composeRule.onNodeWithTag(BreathingTestTags.COUNTDOWN_TEXT).getUnclippedBoundsInRoot()
        val primaryButtonBoundsMin = composeRule.onNodeWithTag(BreathingTestTags.PRIMARY_BUTTON).getUnclippedBoundsInRoot()
        val endButtonBoundsMin = composeRule.onNodeWithTag(BreathingTestTags.END_BUTTON).getUnclippedBoundsInRoot()

        val scaleMin = composeRule.onNodeWithTag(BreathingTestTags.CIRCLE)
            .fetchSemanticsNode()
            .config[VisualScaleSemanticsKey]

        // Advance progress to 1f (max scale)
        currentState = createRunningState(phaseProgress = 1f)
        composeRule.setContent {
            MindoraTheme {
                BreathingExerciseScreen(
                    uiState = currentState,
                    onBack = {},
                    onStart = {},
                    onPause = {},
                    onResume = {},
                    onRequestEnd = {},
                    onDismissEnd = {},
                    onConfirmEnd = {},
                    onRetrySave = {},
                    onDiscard = {},
                    onDone = {},
                )
            }
        }

        val containerBoundsMax = composeRule.onNodeWithTag(BreathingTestTags.CONTAINER).getUnclippedBoundsInRoot()
        val phaseTextBoundsMax = composeRule.onNodeWithTag(BreathingTestTags.PHASE_TEXT).getUnclippedBoundsInRoot()
        val countdownBoundsMax = composeRule.onNodeWithTag(BreathingTestTags.COUNTDOWN_TEXT).getUnclippedBoundsInRoot()
        val primaryButtonBoundsMax = composeRule.onNodeWithTag(BreathingTestTags.PRIMARY_BUTTON).getUnclippedBoundsInRoot()
        val endButtonBoundsMax = composeRule.onNodeWithTag(BreathingTestTags.END_BUTTON).getUnclippedBoundsInRoot()

        val scaleMax = composeRule.onNodeWithTag(BreathingTestTags.CIRCLE)
            .fetchSemanticsNode()
            .config[VisualScaleSemanticsKey]

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
        composeRule.setContent {
            MindoraTheme {
                BreathingExerciseScreen(
                    uiState = createRunningState(phase = BreathingPhase.INHALE, phaseProgress = 0.8f),
                    onBack = {},
                    onStart = {},
                    onPause = {},
                    onResume = {},
                    onRequestEnd = {},
                    onDismissEnd = {},
                    onConfirmEnd = {},
                    onRetrySave = {},
                    onDiscard = {},
                    onDone = {},
                )
            }
        }
        val inhaleScale = composeRule.onNodeWithTag(BreathingTestTags.CIRCLE)
            .fetchSemanticsNode()
            .config[VisualScaleSemanticsKey]

        composeRule.setContent {
            MindoraTheme {
                BreathingExerciseScreen(
                    uiState = createRunningState(phase = BreathingPhase.EXHALE, phaseProgress = 0.8f),
                    onBack = {},
                    onStart = {},
                    onPause = {},
                    onResume = {},
                    onRequestEnd = {},
                    onDismissEnd = {},
                    onConfirmEnd = {},
                    onRetrySave = {},
                    onDiscard = {},
                    onDone = {},
                )
            }
        }
        val exhaleScale = composeRule.onNodeWithTag(BreathingTestTags.CIRCLE)
            .fetchSemanticsNode()
            .config[VisualScaleSemanticsKey]

        assertNotEquals("Inhale and exhale at same progress produce different visual scale", inhaleScale, exhaleScale)
    }

    @Test
    fun circleIsCenteredInFixedContainer() {
        composeRule.setContent {
            MindoraTheme {
                BreathingExerciseScreen(
                    uiState = createRunningState(phaseProgress = 0.5f),
                    onBack = {},
                    onStart = {},
                    onPause = {},
                    onResume = {},
                    onRequestEnd = {},
                    onDismissEnd = {},
                    onConfirmEnd = {},
                    onRetrySave = {},
                    onDiscard = {},
                    onDone = {},
                )
            }
        }

        val containerBounds = composeRule.onNodeWithTag(BreathingTestTags.CONTAINER).getUnclippedBoundsInRoot()
        val circleBounds = composeRule.onNodeWithTag(BreathingTestTags.CIRCLE).getUnclippedBoundsInRoot()

        val containerCenterX = (containerBounds.left + containerBounds.right) / 2
        val containerCenterY = (containerBounds.top + containerBounds.bottom) / 2

        val circleCenterX = (circleBounds.left + circleBounds.right) / 2
        val circleCenterY = (circleBounds.top + circleBounds.bottom) / 2

        assertEquals("Circle center X matches container center X", containerCenterX.value, circleCenterX.value, 0.5f)
        assertEquals("Circle center Y matches container center Y", containerCenterY.value, circleCenterY.value, 0.5f)
    }

    @Test
    fun pauseResumeAndEndActionsWorkAsExpected() {
        var pauseCalled = false
        var endCalled = false

        composeRule.setContent {
            MindoraTheme {
                BreathingExerciseScreen(
                    uiState = createRunningState(),
                    onBack = {},
                    onStart = {},
                    onPause = { pauseCalled = true },
                    onResume = {},
                    onRequestEnd = { endCalled = true },
                    onDismissEnd = {},
                    onConfirmEnd = {},
                    onRetrySave = {},
                    onDiscard = {},
                    onDone = {},
                )
            }
        }

        composeRule.onNodeWithTag(BreathingTestTags.PRIMARY_BUTTON).performClick()
        assertEquals(true, pauseCalled)

        composeRule.onNodeWithTag(BreathingTestTags.END_BUTTON).performClick()
        assertEquals(true, endCalled)
    }

    @Test
    fun pausedStateDisplaysResumeButtonInSameBounds() {
        composeRule.setContent {
            MindoraTheme {
                BreathingExerciseScreen(
                    uiState = createRunningState(),
                    onBack = {},
                    onStart = {},
                    onPause = {},
                    onResume = {},
                    onRequestEnd = {},
                    onDismissEnd = {},
                    onConfirmEnd = {},
                    onRetrySave = {},
                    onDiscard = {},
                    onDone = {},
                )
            }
        }
        val runningButtonBounds = composeRule.onNodeWithTag(BreathingTestTags.PRIMARY_BUTTON).getUnclippedBoundsInRoot()

        composeRule.setContent {
            MindoraTheme {
                BreathingExerciseScreen(
                    uiState = createPausedState(),
                    onBack = {},
                    onStart = {},
                    onPause = {},
                    onResume = {},
                    onRequestEnd = {},
                    onDismissEnd = {},
                    onConfirmEnd = {},
                    onRetrySave = {},
                    onDiscard = {},
                    onDone = {},
                )
            }
        }
        val pausedButtonBounds = composeRule.onNodeWithTag(BreathingTestTags.PRIMARY_BUTTON).getUnclippedBoundsInRoot()

        assertEquals("Primary action button bounds remain stable between running and paused states", runningButtonBounds, pausedButtonBounds)
        composeRule.onNodeWithText("Resume").assertIsDisplayed()
    }
}
