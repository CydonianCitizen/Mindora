package com.cydoniancitizen.mindora.app

import com.cydoniancitizen.mindora.navigation.BreathingExerciseDestination
import com.cydoniancitizen.mindora.navigation.FreeMeditationDestination
import com.cydoniancitizen.mindora.navigation.GuidedMeditationDestination
import com.cydoniancitizen.mindora.navigation.MindoraDestination
import com.cydoniancitizen.mindora.navigation.PathDetailDestination
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MindoraAppTest {
    @Test
    fun `bottom bar is visible only on top-level destinations`() {
        MindoraDestination.entries.forEach { destination ->
            assertTrue(shouldShowBottomBar(destination.route))
        }
        assertFalse(shouldShowBottomBar(null))
        assertFalse(shouldShowBottomBar(PathDetailDestination.route))
        assertFalse(shouldShowBottomBar(FreeMeditationDestination.route))
        assertFalse(shouldShowBottomBar(FreeMeditationDestination.linkedRoute))
        assertFalse(shouldShowBottomBar(BreathingExerciseDestination.route))
        assertFalse(shouldShowBottomBar(BreathingExerciseDestination.linkedRoute))
        assertFalse(shouldShowBottomBar(GuidedMeditationDestination.route))
    }
}
