package com.openai.interactivefitness.data

import com.openai.interactivefitness.domain.WorkoutSession
import com.openai.interactivefitness.domain.StrengthSet
import com.openai.interactivefitness.domain.WorkoutInterval
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomWorkoutRepository(
    private val dao: WorkoutDao,
) : WorkoutRepository {
    override val workouts: Flow<List<WorkoutSession>> =
        dao.observeAll().map { rows ->
            rows.map { row ->
                row.workout.toDomain().copy(
                    strengthSets = row.strengthSets
                        .sortedBy(StrengthSetEntity::position)
                        .map {
                            StrengthSet(
                                id = it.id,
                                exercise = it.exercise,
                                weightKg = it.weightKg,
                                reps = it.reps,
                                rpe = it.rpe,
                            )
                        },
                    intervals = row.intervals
                        .sortedBy(WorkoutIntervalEntity::position)
                        .map {
                            WorkoutInterval(
                                id = it.id,
                                durationSeconds = it.durationSeconds,
                                distanceMeters = it.distanceMeters,
                                note = it.note,
                            )
                        },
                )
            }
        }

    override suspend fun save(workout: WorkoutSession) {
        dao.replaceWorkout(
            workout = workout.toEntity(),
            sets = workout.strengthSets.mapIndexed { index, set ->
                StrengthSetEntity(
                    id = set.id,
                    workoutId = workout.id,
                    position = index,
                    exercise = set.exercise,
                    weightKg = set.weightKg,
                    reps = set.reps,
                    rpe = set.rpe,
                )
            },
            intervals = workout.intervals.mapIndexed { index, interval ->
                WorkoutIntervalEntity(
                    id = interval.id,
                    workoutId = workout.id,
                    position = index,
                    durationSeconds = interval.durationSeconds,
                    distanceMeters = interval.distanceMeters,
                    note = interval.note,
                )
            },
        )
    }

    override suspend fun delete(id: String) {
        dao.deleteById(id)
    }
}
