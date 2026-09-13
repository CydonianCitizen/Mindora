package com.cydoniancitizen.mindora.feature.history

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSession
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSessionStatus
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSessionType
import com.cydoniancitizen.mindora.ui.theme.MindoraTheme
import java.time.Duration
import java.time.Instant
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HistoryScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun emptyHistoryStateDisplaysTitleAndTriggersStartPractice() {
        var startPracticeClicked = false
        composeRule.setContent {
            MindoraTheme {
                HistoryScreen(
                    uiState = HistoryUiState.Empty,
                    onFilterSelected = {},
                    onStartPracticeClick = { startPracticeClicked = true },
                )
            }
        }

        composeRule.onNodeWithText("History").assertIsDisplayed()
        composeRule.onNodeWithText("Your journey starts here").assertIsDisplayed()
        composeRule.onNodeWithText("Start a practice").assertIsDisplayed().performClick()
        assertTrue(startPracticeClicked)
    }

    @Test
    fun contentStateRendersMonthGroupUppercaseAndSessionsAcrossMultipleMonths() {
        val session1 = MindfulnessSession(
            id = "s1",
            type = MindfulnessSessionType.FREE_MEDITATION,
            status = MindfulnessSessionStatus.COMPLETED,
            sourcePathId = null,
            sourceStepId = null,
            startedAt = Instant.parse("2026-02-10T14:30:00Z"),
            activeDuration = Duration.ofMinutes(15),
            plannedDuration = null,
        )
        val session2 = MindfulnessSession(
            id = "s2",
            type = MindfulnessSessionType.BREATHING_EXERCISE,
            status = MindfulnessSessionStatus.COMPLETED,
            sourcePathId = null,
            sourceStepId = null,
            startedAt = Instant.parse("2026-01-15T09:00:00Z"),
            activeDuration = Duration.ofMinutes(5),
            plannedDuration = null,
        )

        composeRule.setContent {
            MindoraTheme {
                HistoryScreen(
                    uiState = HistoryUiState.Content(
                        selectedFilter = HistoryFilter.ALL,
                        monthGroups = listOf(
                            HistoryMonthGroup(
                                yearMonth = YearMonth.of(2026, 2),
                                sessions = listOf(
                                    HistorySessionDisplayItem(
                                        session = session1,
                                        catalogueTitle = null,
                                    ),
                                ),
                            ),
                            HistoryMonthGroup(
                                yearMonth = YearMonth.of(2026, 1),
                                sessions = listOf(
                                    HistorySessionDisplayItem(
                                        session = session2,
                                        catalogueTitle = null,
                                    ),
                                ),
                            ),
                        ),
                    ),
                    onFilterSelected = {},
                    onStartPracticeClick = {},
                )
            }
        }

        composeRule.onNodeWithText("FEBRUARY 2026").assertIsDisplayed()
        composeRule.onNodeWithText("JANUARY 2026").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("February 2026").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("January 2026").assertIsDisplayed()
        composeRule.onAllNodesWithText("Free meditation").onFirst().assertIsDisplayed()
        composeRule.onNodeWithText("Free meditation • 15:00").assertIsDisplayed()
        composeRule.onNodeWithText("All activity").assertIsDisplayed()
    }

    @Test
    fun filterSelectionTriggersFilterCallback() {
        var selectedFilter: HistoryFilter? = null

        composeRule.setContent {
            MindoraTheme {
                HistoryScreen(
                    uiState = HistoryUiState.Content(
                        selectedFilter = HistoryFilter.ALL,
                        monthGroups = emptyList(),
                    ),
                    onFilterSelected = { selectedFilter = it },
                    onStartPracticeClick = {},
                )
            }
        }

        composeRule.onNodeWithText("Breathing exercise")
            .performScrollTo()
            .performClick()
        assertEquals(HistoryFilter.BREATHING_EXERCISE, selectedFilter)
    }

    @Test
    fun filteredEmptyStateDisplaysClearFilterButton() {
        var cleared = false

        composeRule.setContent {
            MindoraTheme {
                HistoryScreen(
                    uiState = HistoryUiState.Content(
                        selectedFilter = HistoryFilter.BREATHING_EXERCISE,
                        monthGroups = emptyList(),
                    ),
                    onFilterSelected = { if (it == HistoryFilter.ALL) cleared = true },
                    onStartPracticeClick = {},
                )
            }
        }

        composeRule.onNodeWithText("No sessions found").assertIsDisplayed()
        composeRule.onNodeWithText("Show all activity").assertIsDisplayed().performClick()
        assertTrue(cleared)
    }
}
