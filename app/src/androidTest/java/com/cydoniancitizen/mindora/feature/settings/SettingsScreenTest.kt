package com.cydoniancitizen.mindora.feature.settings

import android.text.format.DateFormat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.platform.app.InstrumentationRegistry
import com.cydoniancitizen.mindora.BuildConfig
import com.cydoniancitizen.mindora.core.preferences.model.MindoraPreferences
import com.cydoniancitizen.mindora.ui.theme.MindoraTheme
import java.time.LocalTime
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun realWeeklyGoalRendersAndStepperUsesSupportedIncrements() {
        var preferences by mutableStateOf(MindoraPreferences(weeklyGoalMinutes = 60))
        composeRule.setContent {
            MindoraTheme {
                SettingsScreenUnderTest(
                    preferences = preferences,
                    onWeeklyGoalSelected = {
                        preferences = preferences.copy(weeklyGoalMinutes = it)
                    },
                )
            }
        }

        composeRule.onNodeWithText("60 minutes").assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            "Increase weekly goal, current value: 60 minutes",
        ).performClick()
        composeRule.onNodeWithText("90 minutes").assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            "Decrease weekly goal, current value: 90 minutes",
        ).performClick()
        composeRule.onNodeWithText("60 minutes").assertIsDisplayed()
    }

    @Test
    fun weeklyGoalStepperDisablesControlsAtMinuteBoundaries() {
        var goal by mutableStateOf<Int?>(30)
        composeRule.setContent {
            MindoraTheme {
                SettingsScreenUnderTest(
                    preferences = MindoraPreferences(weeklyGoalMinutes = goal),
                    onWeeklyGoalSelected = { goal = it },
                )
            }
        }

        composeRule.onNodeWithContentDescription(
            "Decrease weekly goal, current value: 30 minutes",
        ).assertIsNotEnabled()

        composeRule.runOnIdle { goal = 120 }
        composeRule.onNodeWithContentDescription(
            "Increase weekly goal, current value: 120 minutes",
        ).assertIsNotEnabled()
    }

    @Test
    fun offGoalCanBeEnabledAndExistingGoalCanBeTurnedOff() {
        var goal by mutableStateOf<Int?>(null)
        composeRule.setContent {
            MindoraTheme {
                SettingsScreenUnderTest(
                    preferences = MindoraPreferences(weeklyGoalMinutes = goal),
                    onWeeklyGoalSelected = { goal = it },
                )
            }
        }

        composeRule.onNodeWithText("Off").assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            "Increase weekly goal, current value: Off",
        ).performClick()
        assertEquals(30, goal)
        composeRule.onNodeWithText("Turn off weekly goal").performClick()
        assertEquals(null, goal)
    }

    @Test
    fun disabledReminderShowsLocalizedTimeAndIsNotActionable() {
        val time = LocalTime.of(7, 35)
        val displayTime = localizedTime(time)
        composeRule.setContent {
            MindoraTheme {
                SettingsScreenUnderTest(
                    preferences = MindoraPreferences(
                        dailyReminderEnabled = false,
                        dailyReminderTime = time,
                    ),
                )
            }
        }

        composeRule.onNodeWithContentDescription("Daily reminder").assertIsOff()
        composeRule.onNodeWithText(displayTime).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Reminder time, $displayTime")
            .assertIsNotEnabled()
    }

    @Test
    fun enabledReminderToggleEmitsActionAndTimeRowIsEnabled() {
        var toggleValue: Boolean? = null
        val time = LocalTime.of(20, 0)
        val displayTime = localizedTime(time)
        composeRule.setContent {
            MindoraTheme {
                SettingsScreenUnderTest(
                    preferences = MindoraPreferences(
                        dailyReminderEnabled = true,
                        dailyReminderTime = time,
                    ),
                    onReminderToggle = { toggleValue = it },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Daily reminder")
            .assertIsOn()
            .performClick()
        assertFalse(toggleValue ?: true)
        composeRule.onNodeWithContentDescription("Reminder time, $displayTime")
            .assertIsEnabled()
            .assertHasClickAction()
    }

    @Test
    fun aboutUsesBuildConfigAndUnsupportedHtmlControlsAreAbsent() {
        val version = "Version ${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})"
        composeRule.setContent {
            MindoraTheme { SettingsScreenUnderTest(preferences = MindoraPreferences()) }
        }

        // About sits below the language and data sections now, and a lazy list does not compose
        // what is far off screen, so the node has to be scrolled into existence before it exists
        // to be found.
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText(version))
        composeRule.onNodeWithText(version).assertIsDisplayed()
        composeRule.onNodeWithText("Privacy first").assertIsDisplayed()
        composeRule.onNodeWithText("Preferred session length").assertDoesNotExist()
        composeRule.onNodeWithText("Reset all data").assertDoesNotExist()
    }

    @Test
    fun pageSectionsAndGoalActionsExposeAccessibilitySemantics() {
        val heading = SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)
        composeRule.setContent {
            MindoraTheme {
                SettingsScreenUnderTest(
                    preferences = MindoraPreferences(weeklyGoalMinutes = 60),
                )
            }
        }

        composeRule.onNodeWithText("Settings").assert(heading)
        composeRule.onNodeWithText("Practice goals").assert(heading)
        composeRule.onNodeWithText("Reminders").assert(heading)
        composeRule.onNodeWithContentDescription(
            "Decrease weekly goal, current value: 60 minutes",
        ).assertIsEnabled()
        composeRule.onNodeWithContentDescription("Daily reminder").assertIsOff()

        // Asserted last, and after a scroll: reaching the sections at the bottom drops the ones at
        // the top out of composition.
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Vibration"))
        composeRule.onNodeWithText("Vibration").assert(heading)
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Language"))
        composeRule.onNodeWithText("Language").assert(heading)
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Your data"))
        composeRule.onNodeWithText("Your data").assert(heading)
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("About"))
        composeRule.onNodeWithText("About").assert(heading)
    }

    @Test
    fun blockedNotificationsShowSystemExplanation() {
        composeRule.setContent {
            MindoraTheme {
                SettingsScreenUnderTest(
                    preferences = MindoraPreferences(dailyReminderEnabled = true),
                    notificationsAllowed = false,
                )
            }
        }

        composeRule.onNodeWithText(
            "Daily reminder is enabled, but notifications are blocked by system settings.",
        ).assertIsDisplayed()
        composeRule.onNodeWithText("Open notification settings").assertIsDisplayed()
    }

    @androidx.compose.runtime.Composable
    private fun SettingsScreenUnderTest(
        preferences: MindoraPreferences,
        notificationsAllowed: Boolean = true,
        onWeeklyGoalSelected: (Int?) -> Unit = {},
        onReminderToggle: (Boolean) -> Unit = {},
        onExportClick: () -> Unit = {},
        onLanguageSelected: (AppLanguage) -> Unit = {},
    ) {
        SettingsScreen(
            uiState = SettingsUiState.Content(preferences),
            actions = SettingsActions(
                onWeeklyGoalSelected = onWeeklyGoalSelected,
                onReminderToggle = onReminderToggle,
                onLanguageSelected = onLanguageSelected,
                onExportClick = onExportClick,
            ),
            notificationsAllowed = notificationsAllowed,
            permissionDenied = false,
        )
    }

    private fun localizedTime(time: LocalTime): String {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, time.hour)
            set(Calendar.MINUTE, time.minute)
        }
        return DateFormat.getTimeFormat(context).format(calendar.time)
    }

    @Test
    fun languageSelectorShowsTheChoiceAndOpensTheOthers() {
        var chosen: AppLanguage? = null
        composeRule.setContent {
            MindoraTheme {
                SettingsScreenUnderTest(
                    preferences = MindoraPreferences(),
                    onLanguageSelected = { chosen = it },
                )
            }
        }

        // Collapsed, the box shows only the current choice; the others exist once it is opened.
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("System default"))
        composeRule.onNodeWithText("English").assertDoesNotExist()
        composeRule.onNodeWithText("System default").performClick()

        composeRule.onNodeWithText("Italiano").assertIsDisplayed().performClick()
        assertEquals(AppLanguage.ITALIAN, chosen)
    }

    @Test
    fun exportSectionOffersTheActionAndReportsTheResult() {
        var exportRequested = false
        composeRule.setContent {
            MindoraTheme {
                SettingsScreenUnderTest(
                    preferences = MindoraPreferences(),
                    onExportClick = { exportRequested = true },
                )
            }
        }

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Export data"))
        composeRule.onNodeWithText("Export data").assertIsEnabled().performClick()
        assertEquals(true, exportRequested)
    }
}
