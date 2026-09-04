package com.cydoniancitizen.mindora.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class MindoraColorSchemeTest {

    @Test
    fun `standard contrast uses the base schemes`() {
        assertEquals(primaryLight, mindoraColorScheme(darkTheme = false, contrast = 0f).primary)
        assertEquals(primaryDark, mindoraColorScheme(darkTheme = true, contrast = 0f).primary)
    }

    @Test
    fun `negative contrast is treated as standard`() {
        assertEquals(primaryLight, mindoraColorScheme(darkTheme = false, contrast = -1f).primary)
    }

    @Test
    fun `medium contrast starts at one third`() {
        assertEquals(
            primaryLightMediumContrast,
            mindoraColorScheme(darkTheme = false, contrast = 1f / 3f).primary,
        )
        assertEquals(
            primaryDarkMediumContrast,
            mindoraColorScheme(darkTheme = true, contrast = 0.5f).primary,
        )
    }

    @Test
    fun `high contrast starts at two thirds`() {
        assertEquals(
            primaryLightHighContrast,
            mindoraColorScheme(darkTheme = false, contrast = 2f / 3f).primary,
        )
        assertEquals(
            primaryDarkHighContrast,
            mindoraColorScheme(darkTheme = true, contrast = 1f).primary,
        )
    }

    @Test
    fun `each contrast step changes the scheme`() {
        val standard = mindoraColorScheme(darkTheme = false, contrast = 0f)
        val medium = mindoraColorScheme(darkTheme = false, contrast = 0.5f)
        val high = mindoraColorScheme(darkTheme = false, contrast = 1f)
        assertNotEquals(standard.primary, medium.primary)
        assertNotEquals(medium.primary, high.primary)
    }
}
