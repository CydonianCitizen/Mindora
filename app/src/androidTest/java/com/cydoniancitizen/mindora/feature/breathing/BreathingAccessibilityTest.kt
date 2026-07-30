package com.cydoniancitizen.mindora.feature.breathing

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import com.cydoniancitizen.mindora.ui.theme.MindoraTheme
import java.time.Duration
import java.time.Instant
import org.junit.Rule
import org.junit.Test

class BreathingAccessibilityTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun phaseSummaryIsDiscoverableWithoutRapidLiveRegionAnnouncements() {
        composeRule.setContent {
            MindoraTheme {
                BreathingExerciseScreen(
                    uiState = BreathingExerciseUiState.Running(
                        startedAt = Instant.EPOCH,
                        accumulatedActiveDuration = Duration.ZERO,
                        resumedAtElapsedRealtimeMillis = 0L,
                        currentPhase = BreathingPhase.INHALE,
                        currentCycle = 1,
                        totalCycles = 5,
                        phaseRemainingDuration = Duration.ofSeconds(4),
                        totalRemainingDuration = Duration.ofSeconds(50),
                        activeDuration = Duration.ZERO,
                        phaseProgress = 0f,
                    ),
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

        val phaseSummary = composeRule.onNodeWithContentDescription(
            "Inhale, Cycle 1 of 5, Phase remaining: 00:04",
        ).assertIsDisplayed().fetchSemanticsNode()
        check(!phaseSummary.config.contains(SemanticsProperties.LiveRegion))
    }
}
