package com.cydoniancitizen.mindora.app

import com.cydoniancitizen.mindora.navigation.GuidedMeditationDestination
import com.cydoniancitizen.mindora.navigation.MindoraDestination
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MindoraAppTest {
    @Test
    fun `bottom bar is visible only on top-level destinations`() {
        MindoraDestination.entries.forEach { destination ->
            assertTrue(shouldShowBottomBar(destination.route))
        }
        assertFalse(shouldShowBottomBar(GuidedMeditationDestination.route))
    }
}
