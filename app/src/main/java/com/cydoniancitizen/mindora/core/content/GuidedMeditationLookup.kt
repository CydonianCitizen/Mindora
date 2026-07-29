package com.cydoniancitizen.mindora.core.content

import com.cydoniancitizen.mindora.core.content.model.GuidedMeditationStep

data class ResolvedGuidedMeditation(
    val pathId: String,
    val step: GuidedMeditationStep,
)

suspend fun MindfulnessContentRepository.findGuidedMeditation(
    stepId: String,
): ResolvedGuidedMeditation? {
    getPaths().forEach { path ->
        val step = path.steps.firstOrNull { it.id == stepId } ?: return@forEach
        return (step as? GuidedMeditationStep)?.let {
            ResolvedGuidedMeditation(pathId = path.id, step = it)
        }
    }
    return null
}
