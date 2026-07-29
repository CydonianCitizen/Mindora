package com.cydoniancitizen.mindora.core.content.data

import com.cydoniancitizen.mindora.core.content.model.BreathingExerciseStep
import com.cydoniancitizen.mindora.core.content.model.FreeMeditationStep
import com.cydoniancitizen.mindora.core.content.model.GuidedMeditationStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentCatalogueParserTest {
    private val validCatalogue by lazy {
        val resource = requireNotNull(
            javaClass.getResource("/valid_mindfulness_catalog.json"),
        )
        resource.readText()
    }

    @Test
    fun `valid catalogue parses every step type and preserves array order`() {
        val paths = ContentCatalogueParser.parse(validCatalogue)

        assertEquals(listOf("first-path", "second-path"), paths.map { it.id })
        assertEquals(
            listOf("guided-1", "free-1", "breathing-1"),
            paths.first().steps.map { it.id },
        )
        assertTrue(paths.first().steps[0] is GuidedMeditationStep)
        assertTrue(paths.first().steps[1] is FreeMeditationStep)
        assertTrue(paths.first().steps[2] is BreathingExerciseStep)

        val guided = paths.first().steps[0] as GuidedMeditationStep
        assertEquals(300, guided.durationSeconds)
        assertEquals("audio/guided_test.mp3", guided.audioAsset)

        val free = paths.first().steps[1] as FreeMeditationStep
        assertEquals(600, free.suggestedDurationSeconds)

        val breathing = paths.first().steps[2] as BreathingExerciseStep
        assertEquals(4, breathing.inhaleSeconds)
        assertEquals(0, breathing.holdAfterInhaleSeconds)
        assertEquals(6, breathing.exhaleSeconds)
        assertEquals(1, breathing.holdAfterExhaleSeconds)
        assertEquals(5, breathing.cycles)
    }

    @Test
    fun `empty catalogue is valid`() {
        assertTrue(
            ContentCatalogueParser.parse("""{"schemaVersion":1,"paths":[]}""").isEmpty(),
        )
    }

    @Test
    fun `unsupported schema version is rejected explicitly`() {
        val error = assertThrows(UnsupportedContentSchemaException::class.java) {
            ContentCatalogueParser.parse("""{"schemaVersion":2}""")
        }

        assertEquals(2, error.schemaVersion)
    }

    @Test
    fun `duplicate path IDs are rejected`() {
        assertInvalid(
            validCatalogue.replace("\"id\": \"second-path\"", "\"id\": \"first-path\""),
            "Duplicate path ID",
        )
    }

    @Test
    fun `duplicate step IDs are rejected globally`() {
        assertInvalid(
            validCatalogue.replace("\"id\": \"second-free\"", "\"id\": \"guided-1\""),
            "Duplicate step ID",
        )
    }

    @Test
    fun `missing required field is rejected`() {
        assertThrows(MalformedContentCatalogueException::class.java) {
            ContentCatalogueParser.parse(
                validCatalogue.replace("          \"durationSeconds\": 300,\n", ""),
            )
        }
    }

    @Test
    fun `invalid guided duration is rejected`() {
        assertInvalid(
            validCatalogue.replace("\"durationSeconds\": 300", "\"durationSeconds\": 0"),
            "durationSeconds",
        )
    }

    @Test
    fun `unsafe guided audio paths are rejected`() {
        listOf(
            "../guided.mp3",
            "/audio/guided.mp3",
            "https://example.com/guided.mp3",
            "audio\\\\guided.mp3",
            "audio/../guided.mp3",
        ).forEach { unsafePath ->
            assertInvalid(
                validCatalogue.replace("audio/guided_test.mp3", unsafePath),
                "safe relative bundled path",
            )
        }
    }

    @Test
    fun `invalid free meditation duration is rejected`() {
        assertInvalid(
            validCatalogue.replace(
                "\"suggestedDurationSeconds\": 600",
                "\"suggestedDurationSeconds\": 0",
            ),
            "suggestedDurationSeconds",
        )
    }

    @Test
    fun `invalid breathing values are rejected`() {
        listOf(
            "\"inhaleSeconds\": 4" to "\"inhaleSeconds\": 0",
            "\"holdAfterInhaleSeconds\": 0" to "\"holdAfterInhaleSeconds\": -1",
            "\"exhaleSeconds\": 6" to "\"exhaleSeconds\": 0",
            "\"holdAfterExhaleSeconds\": 1" to "\"holdAfterExhaleSeconds\": -1",
            "\"cycles\": 5" to "\"cycles\": 0",
        ).forEach { (validValue, invalidValue) ->
            assertInvalid(
                validCatalogue.replace(validValue, invalidValue),
                invalidValue.substringAfter('"').substringBefore('"'),
            )
        }
    }

    @Test
    fun `unknown step type is rejected`() {
        assertThrows(MalformedContentCatalogueException::class.java) {
            ContentCatalogueParser.parse(
                validCatalogue.replace("\"guided_meditation\"", "\"unknown_step\""),
            )
        }
    }

    @Test
    fun `unknown JSON key is rejected`() {
        assertThrows(MalformedContentCatalogueException::class.java) {
            ContentCatalogueParser.parse(
                validCatalogue.replace(
                    "\"title\": \"First path\",",
                    "\"title\": \"First path\", \"unexpected\": true,",
                ),
            )
        }
    }

    @Test
    fun `malformed JSON is rejected`() {
        assertThrows(MalformedContentCatalogueException::class.java) {
            ContentCatalogueParser.parse("""{"schemaVersion":1,"paths":[""")
        }
    }

    private fun assertInvalid(source: String, expectedReason: String) {
        val error = assertThrows(InvalidContentCatalogueException::class.java) {
            ContentCatalogueParser.parse(source)
        }
        assertTrue(error.message.orEmpty().contains(expectedReason))
    }
}
