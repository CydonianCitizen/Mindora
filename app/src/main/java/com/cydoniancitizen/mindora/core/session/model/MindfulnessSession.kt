package com.cydoniancitizen.mindora.core.session.model

import java.time.Duration
import java.time.Instant

data class MindfulnessSession(
    val id: String,
    val type: MindfulnessSessionType,
    val status: MindfulnessSessionStatus,
    /**
     * The bundled path this session ran inside, or null when it did not run inside one: a plain
     * timer, or a library meditation, which stands on its own rather than in a path.
     */
    val sourcePathId: String?,
    /**
     * The bundled content the session ran: a path step or a standalone library meditation. Null
     * only when the session came from no catalogue entry at all, such as a plain timer.
     */
    val sourceStepId: String?,
    val startedAt: Instant,
    val activeDuration: Duration,
    val plannedDuration: Duration?,
)

enum class MindfulnessSessionType {
    GUIDED_MEDITATION,
    FREE_MEDITATION,
    BREATHING_EXERCISE,
    WHITE_NOISE,
}

enum class MindfulnessSessionStatus {
    COMPLETED,
    INTERRUPTED,
}
