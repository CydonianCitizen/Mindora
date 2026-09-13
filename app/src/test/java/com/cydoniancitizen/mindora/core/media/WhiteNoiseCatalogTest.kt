package com.cydoniancitizen.mindora.core.media

import com.cydoniancitizen.mindora.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class WhiteNoiseCatalogTest {
    @Test
    fun `white-noise id resolves to the bundled mp3`() {
        val sound = WhiteNoiseCatalog.find("white-noise")

        assertEquals("white-noise", sound?.id)
        assertEquals(R.string.white_noise, sound?.titleResId)
        assertEquals("audio/white_noise.mp3", sound?.audioAsset)
    }

    @Test
    fun `default is the first catalogue entry`() {
        assertSame(WhiteNoiseCatalog.sounds.first(), WhiteNoiseCatalog.default)
    }

    @Test
    fun `unknown ids resolve to null`() {
        assertNull(WhiteNoiseCatalog.find("brown-noise"))
        assertNull(WhiteNoiseCatalog.find(""))
    }

    @Test
    fun `every asset path is a safe relative bundled path`() {
        WhiteNoiseCatalog.sounds.forEach { sound ->
            assertTrue(sound.audioAsset.isNotBlank())
            assertTrue(sound.audioAsset.startsWith("audio/"))
            assertTrue("no scheme", !sound.audioAsset.contains("://"))
            assertTrue("no traversal", sound.audioAsset.split('/').none { it == ".." })
        }
    }
}
