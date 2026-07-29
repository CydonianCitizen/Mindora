package com.cydoniancitizen.mindora.core.session.model

import java.time.Duration
import java.time.Instant

data class MindfulnessSession(
    val id: String,
    val type: MindfulnessSessionType,
    val status: MindfulnessSessionStatus,
    val sourcePathId: String?,
    val sourceStepId: String?,
    val startedAt: Instant,
    val activeDuration: Duration,
    val plannedDuration: Duration?,
)

enum class MindfulnessSessionType {
    GUIDED_MEDITATION,
    FREE_MEDITATION,
    BREATHING_EXERCISE,
}

enum class MindfulnessSessionStatus {
    COMPLETED,
    INTERRUPTED,
}
