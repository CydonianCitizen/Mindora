package com.cydoniancitizen.mindora.core.content.model

sealed interface MindfulnessStep {
    val id: String
    val title: String
    val description: String
}

data class GuidedMeditationStep(
    override val id: String,
    override val title: String,
    override val description: String,
    val durationSeconds: Int,
    val audioAsset: String,
) : MindfulnessStep

data class FreeMeditationStep(
    override val id: String,
    override val title: String,
    override val description: String,
    val suggestedDurationSeconds: Int,
) : MindfulnessStep

data class BreathingExerciseStep(
    override val id: String,
    override val title: String,
    override val description: String,
    val inhaleSeconds: Int,
    val holdAfterInhaleSeconds: Int,
    val exhaleSeconds: Int,
    val holdAfterExhaleSeconds: Int,
    val cycles: Int,
) : MindfulnessStep
