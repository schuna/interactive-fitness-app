package com.openai.interactivefitness.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AiActionParametersTest {
    @Test
    fun convertsGeminiParametersToCatalogPrefill() {
        val result = mapOf(
            "muscleGroup" to "BACK",
            "equipment" to "케이블",
            "durationMinutes" to "40",
        ).customPlanPrefill()

        assertEquals(MuscleGroup.BACK, result.muscleGroup)
        assertEquals(ExerciseEquipment.CABLE, result.equipment)
        assertEquals(40, result.durationMinutes)
    }

    @Test
    fun rejectsUnknownWorkoutTypeAndInvalidDuration() {
        assertNull(mapOf("workoutType" to "SWIMMING").workoutTypeOrNull())
        assertNull(mapOf("durationMinutes" to "-1").customPlanPrefill().durationMinutes)
    }
}
