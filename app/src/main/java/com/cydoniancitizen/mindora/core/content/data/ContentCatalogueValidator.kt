package com.cydoniancitizen.mindora.core.content.data

import com.cydoniancitizen.mindora.core.content.model.BreathingExerciseStep
import com.cydoniancitizen.mindora.core.content.model.FreeMeditationStep
import com.cydoniancitizen.mindora.core.content.model.GuidedMeditationStep
import com.cydoniancitizen.mindora.core.content.model.MindfulnessPath
import com.cydoniancitizen.mindora.core.content.model.MindfulnessStep

internal object ContentCatalogueValidator {
    private val uriScheme = Regex("^[A-Za-z][A-Za-z0-9+.-]*:")

    fun validateAndMap(catalogue: ContentCatalogueDto): List<MindfulnessPath> {
        val pathIds = mutableSetOf<String>()
        val stepIds = mutableSetOf<String>()

        return catalogue.paths.map { path ->
            validateId(path.id, "Path")
            valid(pathIds.add(path.id), "Duplicate path ID '${path.id}'.")
            validateText(path.title, "Path '${path.id}' title")
            validateText(path.description, "Path '${path.id}' description")
            valid(path.steps.isNotEmpty(), "Path '${path.id}' must contain at least one step.")

            MindfulnessPath(
                id = path.id,
                title = path.title,
                description = path.description,
                steps = path.steps.map { step ->
                    validateStep(step, stepIds)
                },
            )
        }
    }

    private fun validateStep(
        step: MindfulnessStepDto,
        stepIds: MutableSet<String>,
    ): MindfulnessStep {
        validateId(step.id, "Step")
        valid(stepIds.add(step.id), "Duplicate step ID '${step.id}'.")
        validateText(step.title, "Step '${step.id}' title")
        validateText(step.description, "Step '${step.id}' description")

        return when (step) {
            is GuidedMeditationStepDto -> {
                valid(
                    step.durationSeconds > 0,
                    "Guided meditation '${step.id}' durationSeconds must be greater than zero.",
                )
                validateAudioAsset(step.id, step.audioAsset)
                GuidedMeditationStep(
                    id = step.id,
                    title = step.title,
                    description = step.description,
                    durationSeconds = step.durationSeconds,
                    audioAsset = step.audioAsset,
                )
            }

            is FreeMeditationStepDto -> {
                valid(
                    step.suggestedDurationSeconds > 0,
                    "Free meditation '${step.id}' suggestedDurationSeconds must be greater than zero.",
                )
                FreeMeditationStep(
                    id = step.id,
                    title = step.title,
                    description = step.description,
                    suggestedDurationSeconds = step.suggestedDurationSeconds,
                )
            }

            is BreathingExerciseStepDto -> {
                valid(
                    step.inhaleSeconds > 0,
                    "Breathing exercise '${step.id}' inhaleSeconds must be greater than zero.",
                )
                valid(
                    step.holdAfterInhaleSeconds >= 0,
                    "Breathing exercise '${step.id}' holdAfterInhaleSeconds must not be negative.",
                )
                valid(
                    step.exhaleSeconds > 0,
                    "Breathing exercise '${step.id}' exhaleSeconds must be greater than zero.",
                )
                valid(
                    step.holdAfterExhaleSeconds >= 0,
                    "Breathing exercise '${step.id}' holdAfterExhaleSeconds must not be negative.",
                )
                valid(
                    step.cycles > 0,
                    "Breathing exercise '${step.id}' cycles must be greater than zero.",
                )
                BreathingExerciseStep(
                    id = step.id,
                    title = step.title,
                    description = step.description,
                    inhaleSeconds = step.inhaleSeconds,
                    holdAfterInhaleSeconds = step.holdAfterInhaleSeconds,
                    exhaleSeconds = step.exhaleSeconds,
                    holdAfterExhaleSeconds = step.holdAfterExhaleSeconds,
                    cycles = step.cycles,
                )
            }
        }
    }

    private fun validateId(id: String, label: String) {
        valid(id.isNotBlank(), "$label ID must not be blank.")
        valid(id == id.trim(), "$label ID '$id' must not have surrounding whitespace.")
    }

    private fun validateText(value: String, label: String) {
        valid(value.isNotBlank(), "$label must not be blank.")
    }

    private fun validateAudioAsset(stepId: String, audioAsset: String) {
        valid(audioAsset.isNotBlank(), "Guided meditation '$stepId' audioAsset must not be blank.")
        val isRelativeBundledPath = !audioAsset.startsWith("/") &&
            !audioAsset.contains('\\') &&
            !uriScheme.containsMatchIn(audioAsset) &&
            audioAsset.split('/').none { segment -> segment == ".." }
        valid(
            isRelativeBundledPath,
            "Guided meditation '$stepId' audioAsset must be a safe relative bundled path.",
        )
    }

    private fun valid(condition: Boolean, reason: String) {
        if (!condition) {
            throw InvalidContentCatalogueException(reason)
        }
    }
}
