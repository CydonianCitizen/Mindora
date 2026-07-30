package com.cydoniancitizen.mindora.core.content

import com.cydoniancitizen.mindora.core.content.model.GuidedMeditationStep

data class ResolvedGuidedMeditation(
    val pathId: String,
    val step: GuidedMeditationStep,
)

suspend fun MindfulnessContentRepository.findGuidedMeditation(
    stepId: String,
): ResolvedGuidedMeditation? = getPaths()
    .findStep(stepId)
    ?.let { located ->
        (located.step as? GuidedMeditationStep)?.let { step ->
            ResolvedGuidedMeditation(pathId = located.path.id, step = step)
        }
    }
