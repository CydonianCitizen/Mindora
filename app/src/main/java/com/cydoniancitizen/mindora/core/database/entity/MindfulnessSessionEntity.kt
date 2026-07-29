package com.cydoniancitizen.mindora.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSession
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSessionStatus
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSessionType
import java.time.Duration
import java.time.Instant

@Entity(
    tableName = "mindfulness_sessions",
    indices = [
        Index(
            value = ["started_at_epoch_millis", "id"],
            name = "index_mindfulness_sessions_started_at_epoch_millis_id",
        ),
    ],
)
data class MindfulnessSessionEntity(
    @PrimaryKey
    val id: String,
    val type: MindfulnessSessionType,
    val status: MindfulnessSessionStatus,
    @ColumnInfo(name = "source_path_id")
    val sourcePathId: String?,
    @ColumnInfo(name = "source_step_id")
    val sourceStepId: String?,
    @ColumnInfo(name = "started_at_epoch_millis")
    val startedAtEpochMillis: Long,
    @ColumnInfo(name = "active_duration_millis")
    val activeDurationMillis: Long,
    @ColumnInfo(name = "planned_duration_millis")
    val plannedDurationMillis: Long?,
)

internal fun MindfulnessSession.toEntity() = MindfulnessSessionEntity(
    id = id,
    type = type,
    status = status,
    sourcePathId = sourcePathId,
    sourceStepId = sourceStepId,
    startedAtEpochMillis = startedAt.toEpochMilli(),
    activeDurationMillis = activeDuration.toMillis(),
    plannedDurationMillis = plannedDuration?.toMillis(),
)

internal fun MindfulnessSessionEntity.toDomain() = MindfulnessSession(
    id = id,
    type = type,
    status = status,
    sourcePathId = sourcePathId,
    sourceStepId = sourceStepId,
    startedAt = Instant.ofEpochMilli(startedAtEpochMillis),
    activeDuration = Duration.ofMillis(activeDurationMillis),
    plannedDuration = plannedDurationMillis?.let(Duration::ofMillis),
)
