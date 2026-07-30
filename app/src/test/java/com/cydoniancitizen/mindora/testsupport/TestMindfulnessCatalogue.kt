package com.cydoniancitizen.mindora.testsupport

import com.cydoniancitizen.mindora.core.content.model.BreathingExerciseStep
import com.cydoniancitizen.mindora.core.content.model.FreeMeditationStep
import com.cydoniancitizen.mindora.core.content.model.GuidedMeditationStep
import com.cydoniancitizen.mindora.core.content.model.MindfulnessPath
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSession
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSessionStatus
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSessionType
import java.time.Duration
import java.time.Instant

object TestMindfulnessCatalogue {
    val guidedStep = GuidedMeditationStep(
        id = "guided-step",
        title = "Guided step",
        description = "Guided description.",
        durationSeconds = 180,
        audioAsset = "audio/test.mp3",
    )
    val freeStep = FreeMeditationStep(
        id = "free-step",
        title = "Free step",
        description = "Free description.",
        suggestedDurationSeconds = 120,
    )
    val breathingStep = BreathingExerciseStep(
        id = "breathing-step",
        title = "Breathing step",
        description = "Breathing description.",
        inhaleSeconds = 3,
        holdAfterInhaleSeconds = 1,
        exhaleSeconds = 5,
        holdAfterExhaleSeconds = 1,
        cycles = 2,
    )
    val firstPath = MindfulnessPath(
        id = "first-path",
        title = "First path",
        description = "First path description.",
        steps = listOf(guidedStep, freeStep, breathingStep),
    )
    val secondPath = MindfulnessPath(
        id = "second-path",
        title = "Second path",
        description = "Second path description.",
        steps = listOf(
            FreeMeditationStep(
                id = "second-free-step",
                title = "Free step",
                description = "Second free description.",
                suggestedDurationSeconds = 300,
            ),
        ),
    )
    val paths = listOf(firstPath, secondPath)
}

fun testSession(
    id: String,
    status: MindfulnessSessionStatus = MindfulnessSessionStatus.COMPLETED,
    pathId: String? = TestMindfulnessCatalogue.firstPath.id,
    stepId: String? = TestMindfulnessCatalogue.guidedStep.id,
    type: MindfulnessSessionType = MindfulnessSessionType.GUIDED_MEDITATION,
) = MindfulnessSession(
    id = id,
    type = type,
    status = status,
    sourcePathId = pathId,
    sourceStepId = stepId,
    startedAt = Instant.parse("2026-07-29T08:00:00Z"),
    activeDuration = Duration.ofSeconds(30),
    plannedDuration = Duration.ofMinutes(3),
)
