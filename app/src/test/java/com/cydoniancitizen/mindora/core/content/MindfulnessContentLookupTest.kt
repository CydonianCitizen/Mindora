package com.cydoniancitizen.mindora.core.content

import com.cydoniancitizen.mindora.core.content.model.GuidedMeditationStep
import com.cydoniancitizen.mindora.testsupport.TestMindfulnessCatalogue
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MindfulnessContentLookupTest {
    private val paths = TestMindfulnessCatalogue.paths

    @Test
    fun `path lookup requires exact ID`() {
        assertEquals("First path", paths.findPath("first-path")?.title)
        assertNull(paths.findPath("FIRST-PATH"))
        assertNull(paths.findPath("First path"))
    }

    @Test
    fun `global step lookup returns containing path deterministically`() {
        val located = paths.findStep("free-step")

        assertEquals("first-path", located?.path?.id)
        assertEquals("free-step", located?.step?.id)
        assertNull(paths.findStep("Free step"))
        assertNull(paths.findStep("missing"))
    }

    @Test
    fun `guided lookup remains compatible and rejects wrong type`() = runTest {
        val repository = object : MindfulnessContentRepository {
            override suspend fun getPaths() = paths
        }

        val guided = repository.findGuidedMeditation("guided-step")

        assertEquals("first-path", guided?.pathId)
        assertTrue(guided?.step is GuidedMeditationStep)
        assertNull(repository.findGuidedMeditation("free-step"))
    }
}
