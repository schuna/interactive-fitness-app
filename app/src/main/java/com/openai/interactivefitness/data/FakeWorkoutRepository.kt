package com.openai.interactivefitness.data

import com.openai.interactivefitness.domain.WorkoutSession
import com.openai.interactivefitness.domain.WorkoutType
import java.time.LocalDateTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class FakeWorkoutRepository : WorkoutRepository {
    private val mutableWorkouts = MutableStateFlow(
        listOf(
            WorkoutSession(
                id = "sample-run",
                type = WorkoutType.RUNNING,
                title = "가벼운 달리기",
                startedAt = LocalDateTime.now().minusDays(1),
                durationMinutes = 32,
                rpe = 6,
                detail = "5.1 km",
            ),
            WorkoutSession(
                id = "sample-strength",
                type = WorkoutType.STRENGTH,
                title = "상체 근력",
                startedAt = LocalDateTime.now().minusDays(3),
                durationMinutes = 45,
                rpe = 7,
                detail = "12세트",
            ),
        ),
    )

    override val workouts: StateFlow<List<WorkoutSession>> = mutableWorkouts.asStateFlow()

    override suspend fun save(workout: WorkoutSession) {
        mutableWorkouts.update { current ->
            (current.filterNot { it.id == workout.id } + workout)
                .sortedByDescending(WorkoutSession::startedAt)
        }
    }

    override suspend fun delete(id: String) {
        mutableWorkouts.update { current -> current.filterNot { it.id == id } }
    }
}
