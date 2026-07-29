package com.cydoniancitizen.mindora.core.content

import com.cydoniancitizen.mindora.core.content.model.MindfulnessPath

interface MindfulnessContentRepository {
    suspend fun getPaths(): List<MindfulnessPath>
}
