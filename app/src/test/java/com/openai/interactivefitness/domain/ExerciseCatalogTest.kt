package com.openai.interactivefitness.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseCatalogTest {
    @Test
    fun filtersByMuscleAndEquipmentTogether() {
        val result = ExerciseCatalog.filter(
            muscleGroup = MuscleGroup.BACK,
            equipment = ExerciseEquipment.CABLE,
        )

        assertTrue(result.isNotEmpty())
        assertTrue(result.all {
            MuscleGroup.BACK in it.muscleGroups &&
                it.equipment == ExerciseEquipment.CABLE
        })
    }

    @Test
    fun searchesByExerciseNameAndMetadata() {
        assertEquals("랫 풀다운", ExerciseCatalog.filter("랫 풀").single().name)
        assertTrue(ExerciseCatalog.filter("맨몸").all {
            it.equipment == ExerciseEquipment.BODYWEIGHT
        })
    }
}
