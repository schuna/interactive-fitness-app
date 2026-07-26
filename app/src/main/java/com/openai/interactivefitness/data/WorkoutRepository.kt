package com.openai.interactivefitness.data

import com.openai.interactivefitness.domain.WorkoutSession
import kotlinx.coroutines.flow.Flow

interface WorkoutRepository {
    val workouts: Flow<List<WorkoutSession>>
    suspend fun save(workout: WorkoutSession)
    suspend fun delete(id: String)
}
