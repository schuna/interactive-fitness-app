package com.openai.interactivefitness.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomWorkoutPlanDao {
    @Transaction
    @Query("SELECT * FROM custom_workout_plans ORDER BY title")
    fun observeAll(): Flow<List<CustomWorkoutPlanWithDetails>>

    @Query("SELECT COUNT(*) FROM custom_workout_plans")
    suspend fun count(): Int

    @Upsert
    suspend fun upsertPlan(plan: CustomWorkoutPlanEntity)

    @Upsert
    suspend fun upsertExercises(exercises: List<CustomPlanExerciseEntity>)

    @Upsert
    suspend fun upsertSets(sets: List<CustomPlanSetEntity>)

    @Query("DELETE FROM custom_plan_exercises WHERE planId = :planId")
    suspend fun deleteExercises(planId: String)

    @Query("DELETE FROM custom_workout_plans WHERE id = :id")
    suspend fun deleteById(id: String)

    @Transaction
    suspend fun replace(
        plan: CustomWorkoutPlanEntity,
        exercises: List<CustomPlanExerciseEntity>,
        sets: List<CustomPlanSetEntity>,
    ) {
        upsertPlan(plan)
        deleteExercises(plan.id)
        if (exercises.isNotEmpty()) upsertExercises(exercises)
        if (sets.isNotEmpty()) upsertSets(sets)
    }
}
