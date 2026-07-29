package com.cydoniancitizen.mindora.core.session

import com.cydoniancitizen.mindora.core.session.model.MindfulnessSession
import kotlinx.coroutines.flow.Flow

interface MindfulnessSessionRepository {
    fun observeSessions(): Flow<List<MindfulnessSession>>

    suspend fun addSession(session: MindfulnessSession)
}
