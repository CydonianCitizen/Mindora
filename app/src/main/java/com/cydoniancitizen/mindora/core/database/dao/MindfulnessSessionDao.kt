package com.cydoniancitizen.mindora.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cydoniancitizen.mindora.core.database.entity.MindfulnessSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MindfulnessSessionDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(session: MindfulnessSessionEntity)

    @Query(
        """
        SELECT * FROM mindfulness_sessions
        ORDER BY started_at_epoch_millis DESC, id DESC
        """,
    )
    fun observeAll(): Flow<List<MindfulnessSessionEntity>>
}
