package com.cydoniancitizen.mindora.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.cydoniancitizen.mindora.core.database.converter.SessionTypeConverters
import com.cydoniancitizen.mindora.core.database.dao.MindfulnessSessionDao
import com.cydoniancitizen.mindora.core.database.entity.MindfulnessSessionEntity

@Database(
    entities = [MindfulnessSessionEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(SessionTypeConverters::class)
abstract class MindoraDatabase : RoomDatabase() {
    abstract fun mindfulnessSessionDao(): MindfulnessSessionDao
}
