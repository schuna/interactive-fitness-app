package com.openai.interactivefitness.domain

data class CustomPlanPrefill(
    val muscleGroup: MuscleGroup? = null,
    val equipment: ExerciseEquipment? = null,
    val durationMinutes: Int? = null,
)

fun Map<String, String>.workoutTypeOrNull(): WorkoutType? =
    this["workoutType"]?.let { value ->
        WorkoutType.entries.firstOrNull {
            it.name.equals(value, ignoreCase = true) ||
                it.label.equals(value, ignoreCase = true)
        }
    }

fun Map<String, String>.customPlanPrefill(): CustomPlanPrefill =
    CustomPlanPrefill(
        muscleGroup = this["muscleGroup"]?.let { value ->
            MuscleGroup.entries.firstOrNull {
                it.name.equals(value, ignoreCase = true) ||
                    it.label.equals(value, ignoreCase = true)
            }
        },
        equipment = this["equipment"]?.let { value ->
            ExerciseEquipment.entries.firstOrNull {
                it.name.equals(value, ignoreCase = true) ||
                    it.label.equals(value, ignoreCase = true)
            }
        },
        durationMinutes = this["durationMinutes"]
            ?.toIntOrNull()
            ?.takeIf { it in 1..1_440 },
    )
