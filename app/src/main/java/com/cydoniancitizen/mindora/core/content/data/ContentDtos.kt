package com.cydoniancitizen.mindora.core.content.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ContentCatalogueDto(
    val schemaVersion: Int,
    val paths: List<MindfulnessPathDto>,
    val meditations: List<LibraryMeditationDto> = emptyList(),
)

/**
 * Text the reader sees, keyed by language tag. Assets carry no locale qualifier of their own, so
 * the catalogue holds every translation and the parser resolves one.
 */
internal typealias LocalizedTextDto = Map<String, String>

@Serializable
internal data class LibraryMeditationDto(
    val id: String,
    val title: LocalizedTextDto,
    val description: LocalizedTextDto,
    val technique: MeditationTermDto,
    val category: MeditationTermDto,
    val goal: MeditationTermDto,
    val durationMinutes: Int,
    val level: String,
    val steps: List<LibraryMeditationStepDto>,
    val safetyNotes: LocalizedTextDto? = null,
    val tags: List<LocalizedTextDto> = emptyList(),
    val audioAsset: String? = null,
)

@Serializable
internal data class LibraryMeditationStepDto(
    val text: LocalizedTextDto,
    val durationSeconds: Int? = null,
)

@Serializable
internal data class MeditationTermDto(
    val id: String,
    val label: LocalizedTextDto,
)

@Serializable
internal data class MindfulnessPathDto(
    val id: String,
    val title: String,
    val description: String,
    val steps: List<MindfulnessStepDto>,
)

@Serializable
internal sealed class MindfulnessStepDto {
    abstract val id: String
    abstract val title: String
    abstract val description: String
}

@Serializable
@SerialName("guided_meditation")
internal data class GuidedMeditationStepDto(
    override val id: String,
    override val title: String,
    override val description: String,
    val durationSeconds: Int,
    val audioAsset: String,
) : MindfulnessStepDto()

@Serializable
@SerialName("free_meditation")
internal data class FreeMeditationStepDto(
    override val id: String,
    override val title: String,
    override val description: String,
    val suggestedDurationSeconds: Int,
) : MindfulnessStepDto()

@Serializable
@SerialName("breathing_exercise")
internal data class BreathingExerciseStepDto(
    override val id: String,
    override val title: String,
    override val description: String,
    val inhaleSeconds: Int,
    val holdAfterInhaleSeconds: Int,
    val exhaleSeconds: Int,
    val holdAfterExhaleSeconds: Int,
    val cycles: Int,
) : MindfulnessStepDto()
