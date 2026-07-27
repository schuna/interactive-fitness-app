package com.openai.interactivefitness.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class PlannedExerciseTest {
    @Test
    fun calculatesTotalVolumeAcrossSets() {
        val exercise = PlannedExercise(
            exerciseId = "lat-pulldown",
            exerciseName = "랫 풀다운",
            sets = listOf(
                PlannedSet(weightKg = 12.0, reps = 12),
                PlannedSet(weightKg = 18.0, reps = 12),
                PlannedSet(weightKg = 20.0, reps = 12),
            ),
        )

        assertEquals(600.0, exercise.totalVolumeKg, 0.0)
    }

    @Test
    fun customPlanExpandsConfiguredSetsIntoExecutionSteps() {
        val exercise = PlannedExercise(
            exerciseId = "lat-pulldown",
            exerciseName = "랫 풀다운",
            sets = listOf(
                PlannedSet(weightKg = 12.0, reps = 12),
                PlannedSet(weightKg = 18.5, reps = 10),
            ),
        )
        val plan = CustomWorkoutPlan(
            title = "등 운동",
            type = WorkoutType.STRENGTH,
            durationMinutes = 30,
            exercises = listOf("랫 풀다운"),
            plannedExercises = listOf(exercise),
        )

        assertEquals(
            listOf("랫 풀다운 · 1세트 · 12kg × 12회", "랫 풀다운 · 2세트 · 18.5kg × 10회"),
            plan.executionSteps(),
        )
    }
}
