package com.openai.interactivefitness.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Relation
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    @Transaction
    @Query("SELECT * FROM workout_sessions ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<WorkoutWithDetails>>

    @Upsert
    suspend fun upsert(workout: WorkoutEntity)

    @Upsert
    suspend fun upsertStrengthSets(sets: List<StrengthSetEntity>)

    @Upsert
    suspend fun upsertIntervals(intervals: List<WorkoutIntervalEntity>)

    @Query("DELETE FROM strength_sets WHERE workoutId = :workoutId")
    suspend fun deleteStrengthSets(workoutId: String)

    @Query("DELETE FROM workout_intervals WHERE workoutId = :workoutId")
    suspend fun deleteIntervals(workoutId: String)

    @Transaction
    suspend fun replaceWorkout(
        workout: WorkoutEntity,
        sets: List<StrengthSetEntity>,
        intervals: List<WorkoutIntervalEntity>,
    ) {
        upsert(workout)
        deleteStrengthSets(workout.id)
        deleteIntervals(workout.id)
        if (sets.isNotEmpty()) upsertStrengthSets(sets)
        if (intervals.isNotEmpty()) upsertIntervals(intervals)
    }

    @Query("DELETE FROM workout_sessions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM workout_sessions")
    suspend fun count(): Int
}

data class WorkoutWithDetails(
    @Embedded val workout: WorkoutEntity,
    @Relation(parentColumn = "id", entityColumn = "workoutId")
    val strengthSets: List<StrengthSetEntity>,
    @Relation(parentColumn = "id", entityColumn = "workoutId")
    val intervals: List<WorkoutIntervalEntity>,
)
