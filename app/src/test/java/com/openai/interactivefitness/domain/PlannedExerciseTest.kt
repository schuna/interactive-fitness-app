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
}
