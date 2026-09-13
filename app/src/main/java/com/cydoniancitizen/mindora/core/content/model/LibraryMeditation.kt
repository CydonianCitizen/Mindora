package com.cydoniancitizen.mindora.core.content.model

/**
 * A meditation of the bundled library: one practice, described by its own text.
 *
 * The library is deliberately flat. A [MindfulnessPath] is an ordered course the user works
 * through; a library meditation stands on its own and is found by what it is for, which is why
 * technique, category and goal are separate fields rather than one label.
 */
data class LibraryMeditation(
    val id: String,
    val title: String,
    val description: String,
    val technique: MeditationTerm,
    val category: MeditationTerm,
    val goal: MeditationTerm,
    val durationMinutes: Int,
    val level: MeditationLevel,
    val steps: List<MeditationStep>,
    val safetyNotes: String?,
    val tags: List<String>,
    /**
     * Null for every meditation today: no approved audio is bundled yet. When a recording lands it
     * is added to the catalogue entry, and the guided player reads it from here.
     */
    val audioAsset: String?,
)

/**
 * One instruction of a library meditation, and how long it is meant to hold the screen.
 *
 * [durationSeconds] is null wherever the catalogue leaves the pacing open. A step with a stated
 * length keeps exactly that length; the rest share whatever the stated ones leave, so writing
 * "give the first three minutes to the breath" is enough to make the practice follow its own text.
 */
data class MeditationStep(
    val text: String,
    val durationSeconds: Int? = null,
)

/**
 * A taxonomy value: a stable id the catalogue groups by, and the label the interface shows.
 *
 * Terms live in the catalogue rather than in an enum so a new technique, category or goal is a
 * content change, not a code change.
 */
data class MeditationTerm(
    val id: String,
    val label: String,
)

/** A closed, ordered scale, so it is a type rather than a catalogue term. */
enum class MeditationLevel {
    BEGINNER,
    INTERMEDIATE,
    ADVANCED,
}
