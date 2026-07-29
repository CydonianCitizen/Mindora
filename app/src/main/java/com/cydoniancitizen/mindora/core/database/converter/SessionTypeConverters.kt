package com.cydoniancitizen.mindora.core.database.converter

import androidx.room.TypeConverter
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSessionStatus
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSessionType

class SessionTypeConverters {
    @TypeConverter
    fun sessionTypeToString(type: MindfulnessSessionType): String = when (type) {
        MindfulnessSessionType.GUIDED_MEDITATION -> "GUIDED_MEDITATION"
        MindfulnessSessionType.FREE_MEDITATION -> "FREE_MEDITATION"
        MindfulnessSessionType.BREATHING_EXERCISE -> "BREATHING_EXERCISE"
    }

    @TypeConverter
    fun stringToSessionType(value: String): MindfulnessSessionType = when (value) {
        "GUIDED_MEDITATION" -> MindfulnessSessionType.GUIDED_MEDITATION
        "FREE_MEDITATION" -> MindfulnessSessionType.FREE_MEDITATION
        "BREATHING_EXERCISE" -> MindfulnessSessionType.BREATHING_EXERCISE
        else -> throw IllegalArgumentException("Unknown mindfulness session type: $value")
    }

    @TypeConverter
    fun sessionStatusToString(status: MindfulnessSessionStatus): String = when (status) {
        MindfulnessSessionStatus.COMPLETED -> "COMPLETED"
        MindfulnessSessionStatus.INTERRUPTED -> "INTERRUPTED"
    }

    @TypeConverter
    fun stringToSessionStatus(value: String): MindfulnessSessionStatus = when (value) {
        "COMPLETED" -> MindfulnessSessionStatus.COMPLETED
        "INTERRUPTED" -> MindfulnessSessionStatus.INTERRUPTED
        else -> throw IllegalArgumentException("Unknown mindfulness session status: $value")
    }
}
