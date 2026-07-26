package com.openai.interactivefitness.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workout_sessions ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<WorkoutEntity>>

    @Upsert
    suspend fun upsert(workout: WorkoutEntity)

    @Query("DELETE FROM workout_sessions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM workout_sessions")
    suspend fun count(): Int
}
