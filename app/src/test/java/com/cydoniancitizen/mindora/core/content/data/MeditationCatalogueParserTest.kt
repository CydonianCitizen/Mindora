package com.cydoniancitizen.mindora.core.content.data

import com.cydoniancitizen.mindora.core.content.model.MeditationLevel
import com.cydoniancitizen.mindora.core.content.model.MeditationStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class MeditationCatalogueParserTest {
    @Test
    fun `a meditation is mapped with its taxonomy, steps and optional fields`() {
        val meditation = ContentCatalogueParser.parse(validCatalogue).meditations.single()

        assertEquals("meditation-calm", meditation.id)
        assertEquals("breath-awareness", meditation.technique.id)
        assertEquals("Breath", meditation.category.label)
        assertEquals("calm", meditation.goal.id)
        assertEquals(5, meditation.durationMinutes)
        assertEquals(MeditationLevel.BEGINNER, meditation.level)
        assertEquals(
            listOf(MeditationStep("First step."), MeditationStep("Second step.")),
            meditation.steps,
        )
        assertEquals(listOf("breath"), meditation.tags)
        assertNull(meditation.safetyNotes)
        assertNull(meditation.audioAsset)
    }

    @Test
    fun `every text is resolved in the requested language`() {
        val meditation = ContentCatalogueParser.parse(validCatalogue, "it").meditations.single()

        assertEquals("Respiro per ritrovare calma", meditation.title)
        assertEquals("Una pratica breve.", meditation.description)
        assertEquals("Respiro", meditation.category.label)
        assertEquals(
            listOf(MeditationStep("Primo passo."), MeditationStep("Secondo passo.")),
            meditation.steps,
        )
        assertEquals(listOf("respiro"), meditation.tags)
    }

    @Test
    fun `an untranslated language falls back to the default one`() {
        val meditation = ContentCatalogueParser.parse(validCatalogue, "de").meditations.single()

        assertEquals("Breathing to find calm", meditation.title)
        assertEquals("Breath", meditation.category.label)
    }

    @Test
    fun `a text without the default translation is rejected`() {
        assertInvalid(
            validCatalogue.replace("\"en\": \"Breathing to find calm\", ", ""),
            "must provide the 'en' translation",
        )
    }

    @Test
    fun `a catalogue without meditations still parses`() {
        val catalogue = ContentCatalogueParser.parse("""{"schemaVersion":1,"paths":[]}""")

        assertTrue(catalogue.meditations.isEmpty())
    }

    @Test
    fun `duplicate ids are rejected`() {
        assertInvalid(
            validCatalogue.replace("\"meditations\": [", "\"meditations\": [$meditation,"),
            "Duplicate meditation ID",
        )
    }

    @Test
    fun `an unsupported level is rejected`() {
        assertInvalid(
            validCatalogue.replace("\"level\": \"beginner\"", "\"level\": \"expert\""),
            "level 'expert' is not supported",
        )
    }

    @Test
    fun `a term id used with two labels is rejected`() {
        val second = meditation
            .replace("meditation-calm", "meditation-focus")
            .replace("\"en\": \"Breath\"", "\"en\": \"Breathing\"")

        assertInvalid(
            validCatalogue.replace("\"meditations\": [", "\"meditations\": [$second,"),
            "labelled both",
        )
    }

    @Test
    fun `an empty step list is rejected`() {
        assertInvalid(validCatalogue.replace(steps, "[]"), "at least one step")
    }

    @Test
    fun `a step keeps the length the catalogue states for it`() {
        val meditation = ContentCatalogueParser.parse(
            validCatalogue.replace(
                """{"text": {"en": "First step.", "it": "Primo passo."}}""",
                """{"text": {"en": "First step.", "it": "Primo passo."}, """ +
                    """"durationSeconds": 60}""",
            ),
        ).meditations.single()

        assertEquals(
            listOf(MeditationStep("First step.", 60), MeditationStep("Second step.")),
            meditation.steps,
        )
    }

    @Test
    fun `stated step lengths that fill the whole meditation leave no room for the others`() {
        assertInvalid(
            validCatalogue.replace(
                """{"text": {"en": "First step.", "it": "Primo passo."}}""",
                """{"text": {"en": "First step.", "it": "Primo passo."}, """ +
                    """"durationSeconds": 300}""",
            ),
            "leave no time for the steps without one",
        )
    }

    @Test
    fun `when every step states a length they must add up to the meditation`() {
        assertInvalid(
            validCatalogue
                .replace(
                    """{"text": {"en": "First step.", "it": "Primo passo."}}""",
                    """{"text": {"en": "First step.", "it": "Primo passo."}, """ +
                        """"durationSeconds": 100}""",
                )
                .replace(
                    """{"text": {"en": "Second step.", "it": "Secondo passo."}}""",
                    """{"text": {"en": "Second step.", "it": "Secondo passo."}, """ +
                        """"durationSeconds": 100}""",
                ),
            "add up to 200 seconds but the meditation lasts 300",
        )
    }

    @Test
    fun `an unsafe audio path is rejected even before audio is bundled`() {
        assertInvalid(
            validCatalogue.replace("\"audioAsset\": null", "\"audioAsset\": \"../guided.mp3\""),
            "safe relative bundled path",
        )
    }

    private fun assertInvalid(source: String, expectedReason: String) {
        val error = assertThrows(InvalidContentCatalogueException::class.java) {
            ContentCatalogueParser.parse(source)
        }

        assertTrue(
            "Expected '$expectedReason' in '${error.message}'",
            error.message.orEmpty().contains(expectedReason),
        )
    }

    private companion object {
        // One line, so interpolating it into the fixture leaves it byte-for-byte replaceable.
        const val steps =
            """[{"text": {"en": "First step.", "it": "Primo passo."}}, """ +
                """{"text": {"en": "Second step.", "it": "Secondo passo."}}]"""

        val meditation = """
            {
              "id": "meditation-calm",
              "title": {"en": "Breathing to find calm", "it": "Respiro per ritrovare calma"},
              "description": {"en": "A short practice.", "it": "Una pratica breve."},
              "technique": {
                "id": "breath-awareness",
                "label": {"en": "Breath Awareness", "it": "Breath Awareness"}
              },
              "category": {"id": "breath", "label": {"en": "Breath", "it": "Respiro"}},
              "goal": {"id": "calm", "label": {"en": "Calm", "it": "Calma"}},
              "durationMinutes": 5,
              "level": "beginner",
              "steps": $steps,
              "tags": [{"en": "breath", "it": "respiro"}],
              "audioAsset": null
            }
        """.trimIndent()

        val validCatalogue = """
            {
              "schemaVersion": 1,
              "paths": [],
              "meditations": [$meditation]
            }
        """.trimIndent()
    }
}
