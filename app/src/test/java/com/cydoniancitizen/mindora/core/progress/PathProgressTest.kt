package com.cydoniancitizen.mindora.core.progress

import com.cydoniancitizen.mindora.core.session.model.MindfulnessSessionStatus
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSessionType
import com.cydoniancitizen.mindora.testsupport.TestMindfulnessCatalogue
import com.cydoniancitizen.mindora.testsupport.testSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PathProgressTest {
    private val path = TestMindfulnessCatalogue.firstPath

    @Test
    fun `no sessions completes no current steps`() {
        val progress = calculatePathProgress(path, emptyList())

        assertTrue(progress.completedStepIds.isEmpty())
        assertEquals(0, progress.completedCount)
        assertEquals(3, progress.totalCount)
        assertEquals(0f, progress.fraction)
    }

    @Test
    fun `one exact completed session completes one step`() {
        val progress = calculatePathProgress(path, listOf(testSession("matching")))

        assertEquals(setOf("guided-step"), progress.completedStepIds)
        assertEquals(1, progress.completedCount)
        assertEquals(1f / 3f, progress.fraction)
    }

    @Test
    fun `interrupted standalone and mismatched references do not count`() {
        val sessions = listOf(
            testSession("interrupted", status = MindfulnessSessionStatus.INTERRUPTED),
            testSession("standalone", pathId = null, stepId = null),
            testSession("wrong-path", pathId = "other-path"),
            testSession("wrong-step", stepId = "other-step"),
            testSession("missing-step-id", stepId = null),
        )

        assertEquals(0, calculatePathProgress(path, sessions).completedCount)
    }

    @Test
    fun `duplicates and removed historical steps count at most once`() {
        val sessions = listOf(
            testSession("first"),
            testSession("duplicate"),
            testSession("removed", stepId = "removed-step"),
        )

        val progress = calculatePathProgress(path, sessions)

        assertEquals(setOf("guided-step"), progress.completedStepIds)
        assertEquals(1, progress.completedCount)
        assertTrue(progress.fraction in 0f..1f)
    }

    @Test
    fun `completed sessions count across all current step types`() {
        val sessions = listOf(
            testSession("guided"),
            testSession(
                id = "free",
                stepId = "free-step",
                type = MindfulnessSessionType.FREE_MEDITATION,
            ),
            testSession(
                id = "breathing",
                stepId = "breathing-step",
                type = MindfulnessSessionType.BREATHING_EXERCISE,
            ),
        )

        val progress = calculatePathProgress(path, sessions)

        assertEquals(setOf("guided-step", "free-step", "breathing-step"), progress.completedStepIds)
        assertEquals(3, progress.completedCount)
        assertEquals(1f, progress.fraction)
        assertTrue(progress.fraction in 0f..1f)
    }
}
