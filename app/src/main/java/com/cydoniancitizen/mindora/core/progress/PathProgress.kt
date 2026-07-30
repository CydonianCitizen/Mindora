package com.cydoniancitizen.mindora.core.progress

import com.cydoniancitizen.mindora.core.content.model.MindfulnessPath
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSession
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSessionStatus

data class PathProgress(
    val completedStepIds: Set<String>,
    val completedCount: Int,
    val totalCount: Int,
    val fraction: Float,
)

fun calculatePathProgress(
    path: MindfulnessPath,
    sessions: List<MindfulnessSession>,
): PathProgress {
    val currentStepIds = path.steps.mapTo(linkedSetOf()) { it.id }
    val completedStepIds = sessions.asSequence()
        .filter { session ->
            session.status == MindfulnessSessionStatus.COMPLETED &&
                session.sourcePathId == path.id &&
                session.sourceStepId in currentStepIds
        }
        .mapNotNull(MindfulnessSession::sourceStepId)
        .toSet()
    val totalCount = currentStepIds.size
    val fraction = if (totalCount == 0) {
        0f
    } else {
        completedStepIds.size.toFloat() / totalCount
    }.coerceIn(0f, 1f)

    return PathProgress(
        completedStepIds = completedStepIds,
        completedCount = completedStepIds.size,
        totalCount = totalCount,
        fraction = fraction,
    )
}
