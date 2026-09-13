package com.cydoniancitizen.mindora.core.content.data

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Parses the catalogue that actually ships, so a content edit is caught by the ordinary test run
 * rather than only on a device. The asset is read from the module rather than from the classpath:
 * unit tests run with the module directory as their working directory.
 */
class BundledCatalogueAssetTest {
    @Test
    fun productionCatalogueParsesAndValidates() {
        val catalogue = ContentCatalogueParser.parse(asset.readText())

        assertEquals(15, catalogue.meditations.size)
        assertEquals(5, catalogue.meditations.map { it.technique.id }.distinct().size)
        assertTrue(catalogue.meditations.all { it.steps.size >= 7 })
        // Every technique carries the three variants the library was planned around.
        assertTrue(
            catalogue.meditations.groupBy { it.technique.id }.all { (_, group) -> group.size == 3 },
        )
    }

    @Test
    fun everyMeditationReadsInBothShippedLanguages() {
        val source = asset.readText()

        val english = ContentCatalogueParser.parse(source, "en").meditations
        val italian = ContentCatalogueParser.parse(source, "it").meditations

        assertEquals(english.map { it.id }, italian.map { it.id })
        // Same catalogue, different words: a text left untranslated would fall back and match.
        assertTrue(english.zip(italian).all { (left, right) -> left.title != right.title })
        assertTrue(english.zip(italian).all { (left, right) -> left.steps != right.steps })
        assertTrue(italian.all { it.tags.isNotEmpty() })
    }

    @Test
    fun noMeditationClaimsAudioThatIsNotBundledYet() {
        val catalogue = ContentCatalogueParser.parse(asset.readText())

        assertTrue(catalogue.meditations.all { it.audioAsset == null })
    }

    private val asset: File
        get() = File("src/main/assets/content/mindfulness_catalog.json")
            .takeIf(File::exists)
            ?: File("app/src/main/assets/content/mindfulness_catalog.json")
}
