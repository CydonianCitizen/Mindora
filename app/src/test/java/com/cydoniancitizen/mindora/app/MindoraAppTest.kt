package com.cydoniancitizen.mindora.app

import com.cydoniancitizen.mindora.navigation.BreathingExerciseDestination
import com.cydoniancitizen.mindora.navigation.FreeMeditationDestination
import com.cydoniancitizen.mindora.navigation.GuidedMeditationDestination
import com.cydoniancitizen.mindora.navigation.LibraryDestination
import com.cydoniancitizen.mindora.navigation.MeditationDetailDestination
import com.cydoniancitizen.mindora.navigation.MindoraDestination
import com.cydoniancitizen.mindora.navigation.PathDetailDestination
import com.cydoniancitizen.mindora.navigation.WhiteNoiseDestination
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
        assertFalse(shouldShowBottomBar(LibraryDestination.route))
        assertFalse(shouldShowBottomBar(MeditationDetailDestination.route))
        assertFalse(shouldShowBottomBar(FreeMeditationDestination.route))
        assertFalse(shouldShowBottomBar(FreeMeditationDestination.linkedRoute))
        assertFalse(shouldShowBottomBar(WhiteNoiseDestination.route))
        assertFalse(shouldShowBottomBar(BreathingExerciseDestination.route))
        assertFalse(shouldShowBottomBar(BreathingExerciseDestination.linkedRoute))
        assertFalse(shouldShowBottomBar(GuidedMeditationDestination.route))
    }
}
