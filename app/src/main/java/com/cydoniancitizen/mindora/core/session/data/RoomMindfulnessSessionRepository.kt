package com.cydoniancitizen.mindora.core.session.data

import com.cydoniancitizen.mindora.core.database.dao.MindfulnessSessionDao
import com.cydoniancitizen.mindora.core.database.entity.toDomain
import com.cydoniancitizen.mindora.core.database.entity.toEntity
import com.cydoniancitizen.mindora.core.session.MindfulnessSessionRepository
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSession
import java.time.Duration
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomMindfulnessSessionRepository @Inject constructor(
    private val sessionDao: MindfulnessSessionDao,
) : MindfulnessSessionRepository {
    override fun observeSessions(): Flow<List<MindfulnessSession>> =
        sessionDao.observeAll().map { sessions ->
            sessions.map { it.toDomain() }
        }

    override suspend fun addSession(session: MindfulnessSession) {
        validateForPersistence(session)
        sessionDao.insert(session.toEntity())
    }
}

internal fun validateForPersistence(session: MindfulnessSession) {
    require(session.id.isNotBlank()) { "Session ID must not be blank." }
    require(session.id == session.id.trim()) {
        "Session ID must not have leading or trailing whitespace."
    }
    require(session.activeDuration > Duration.ZERO) {
        "Active duration must be greater than zero."
    }
    session.plannedDuration?.let {
        require(it > Duration.ZERO) {
            "Planned duration must be greater than zero."
        }
    }
    validateOptionalId(session.sourcePathId, "Source path ID")
    validateOptionalId(session.sourceStepId, "Source step ID")
    require(session.sourceStepId == null || session.sourcePathId != null) {
        "Source step ID requires a source path ID."
    }
}

private fun validateOptionalId(value: String?, name: String) {
    value ?: return
    require(value.isNotBlank()) { "$name must not be blank." }
    require(value == value.trim()) {
        "$name must not have leading or trailing whitespace."
    }
}
