package com.openai.interactivefitness.data

import com.openai.interactivefitness.domain.WorkoutSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomWorkoutRepository(
    private val dao: WorkoutDao,
) : WorkoutRepository {
    override val workouts: Flow<List<WorkoutSession>> =
        dao.observeAll().map { rows -> rows.map(WorkoutEntity::toDomain) }

    override suspend fun save(workout: WorkoutSession) {
        dao.upsert(workout.toEntity())
    }

    override suspend fun delete(id: String) {
        dao.deleteById(id)
    }
}
