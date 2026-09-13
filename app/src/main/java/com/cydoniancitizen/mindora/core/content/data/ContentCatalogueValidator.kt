package com.cydoniancitizen.mindora.core.content.data

import com.cydoniancitizen.mindora.core.content.model.BreathingExerciseStep
import com.cydoniancitizen.mindora.core.content.model.FreeMeditationStep
import com.cydoniancitizen.mindora.core.content.model.GuidedMeditationStep
import com.cydoniancitizen.mindora.core.content.model.LibraryMeditation
import com.cydoniancitizen.mindora.core.content.model.MeditationLevel
import com.cydoniancitizen.mindora.core.content.model.MeditationStep
import com.cydoniancitizen.mindora.core.content.model.MeditationTerm
import com.cydoniancitizen.mindora.core.content.model.MindfulnessCatalogue
import com.cydoniancitizen.mindora.core.content.model.MindfulnessPath
import com.cydoniancitizen.mindora.core.content.model.MindfulnessStep

/** The language every catalogue text must provide, and the one anything else falls back to. */
internal const val DEFAULT_CONTENT_LANGUAGE = "en"

internal object ContentCatalogueValidator {
    private val uriScheme = Regex("^[A-Za-z][A-Za-z0-9+.-]*:")

    fun validateAndMap(
        catalogue: ContentCatalogueDto,
        language: String,
    ): MindfulnessCatalogue = MindfulnessCatalogue(
        paths = validatePaths(catalogue),
        meditations = validateMeditations(catalogue, language),
    )

    private fun validatePaths(catalogue: ContentCatalogueDto): List<MindfulnessPath> {
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

    private fun validateMeditations(
        catalogue: ContentCatalogueDto,
        language: String,
    ): List<LibraryMeditation> {
        val meditationIds = mutableSetOf<String>()
        // One id must always carry the same label, or the same filter would appear twice.
        val termLabels = mutableMapOf<String, String>()

        return catalogue.meditations.map { meditation ->
            validateId(meditation.id, "Meditation")
            valid(meditationIds.add(meditation.id), "Duplicate meditation ID '${meditation.id}'.")
            val title = text(meditation.title, language, "Meditation '${meditation.id}' title")
            val description = text(
                meditation.description,
                language,
                "Meditation '${meditation.id}' description",
            )
            valid(
                meditation.durationMinutes > 0,
                "Meditation '${meditation.id}' durationMinutes must be greater than zero.",
            )
            valid(
                meditation.steps.isNotEmpty(),
                "Meditation '${meditation.id}' must contain at least one step.",
            )
            val steps = meditation.steps.mapIndexed { index, step ->
                val label = "Meditation '${meditation.id}' step ${index + 1}"
                step.durationSeconds?.let {
                    valid(it > 0, "$label durationSeconds must be greater than zero.")
                }
                MeditationStep(
                    text = text(step.text, language, label),
                    durationSeconds = step.durationSeconds,
                )
            }
            validateStepPacing(meditation.id, meditation.durationMinutes, steps)
            val tags = meditation.tags.mapIndexed { index, tag ->
                text(tag, language, "Meditation '${meditation.id}' tag ${index + 1}")
            }
            val safetyNotes = meditation.safetyNotes?.let {
                text(it, language, "Meditation '${meditation.id}' safetyNotes")
            }
            meditation.audioAsset?.let { validateAudioAsset(meditation.id, it) }

            LibraryMeditation(
                id = meditation.id,
                title = title,
                description = description,
                technique = term(meditation.technique, meditation.id, "technique", language, termLabels),
                category = term(meditation.category, meditation.id, "category", language, termLabels),
                goal = term(meditation.goal, meditation.id, "goal", language, termLabels),
                durationMinutes = meditation.durationMinutes,
                level = level(meditation.level, meditation.id),
                steps = steps,
                safetyNotes = safetyNotes,
                tags = tags,
                audioAsset = meditation.audioAsset,
            )
        }
    }

    /**
     * A meditation's stated step lengths have to fit inside the meditation.
     *
     * When every step states one they must add up exactly, so a duration and its steps can never
     * drift apart. When only some do, the stated ones must leave time over, or the steps that say
     * nothing would be given no moment of their own and would never be shown.
     */
    private fun validateStepPacing(
        meditationId: String,
        durationMinutes: Int,
        steps: List<MeditationStep>,
    ) {
        val statedSeconds = steps.sumOf { it.durationSeconds ?: 0 }
        val meditationSeconds = durationMinutes * SECONDS_PER_MINUTE
        if (steps.all { it.durationSeconds != null }) {
            valid(
                statedSeconds == meditationSeconds,
                "Meditation '$meditationId' step durations add up to $statedSeconds seconds " +
                    "but the meditation lasts $meditationSeconds.",
            )
        } else {
            valid(
                statedSeconds < meditationSeconds,
                "Meditation '$meditationId' step durations leave no time for the steps " +
                    "without one.",
            )
        }
    }

    private fun term(
        term: MeditationTermDto,
        meditationId: String,
        field: String,
        language: String,
        termLabels: MutableMap<String, String>,
    ): MeditationTerm {
        validateId(term.id, "Meditation '$meditationId' $field ID")
        val label = text(term.label, language, "Meditation '$meditationId' $field label")
        val known = termLabels.putIfAbsent(term.id, label)
        valid(
            known == null || known == label,
            "Term '${term.id}' is labelled both '$known' and '$label'.",
        )
        return MeditationTerm(id = term.id, label = label)
    }

    /**
     * Falls back to [DEFAULT_CONTENT_LANGUAGE], which every text must provide: a catalogue that
     * has not been translated yet still reads, in one language, rather than showing a blank.
     */
    private fun text(values: LocalizedTextDto, language: String, label: String): String {
        val fallback = values[DEFAULT_CONTENT_LANGUAGE]
        valid(
            fallback != null,
            "$label must provide the '$DEFAULT_CONTENT_LANGUAGE' translation.",
        )
        val resolved = values[language] ?: fallback.orEmpty()
        validateText(resolved, label)
        return resolved
    }

    private fun level(value: String, meditationId: String): MeditationLevel =
        when (value) {
            "beginner" -> MeditationLevel.BEGINNER
            "intermediate" -> MeditationLevel.INTERMEDIATE
            "advanced" -> MeditationLevel.ADVANCED
            else -> throw InvalidContentCatalogueException(
                "Meditation '$meditationId' level '$value' is not supported.",
            )
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

    private fun validateAudioAsset(id: String, audioAsset: String) {
        valid(audioAsset.isNotBlank(), "'$id' audioAsset must not be blank.")
        val isRelativeBundledPath = !audioAsset.startsWith("/") &&
            !audioAsset.contains('\\') &&
            !uriScheme.containsMatchIn(audioAsset) &&
            audioAsset.split('/').none { segment -> segment == ".." }
        valid(
            isRelativeBundledPath,
            "'$id' audioAsset must be a safe relative bundled path.",
        )
    }

    private const val SECONDS_PER_MINUTE = 60

    private fun valid(condition: Boolean, reason: String) {
        if (!condition) {
            throw InvalidContentCatalogueException(reason)
        }
    }
}
