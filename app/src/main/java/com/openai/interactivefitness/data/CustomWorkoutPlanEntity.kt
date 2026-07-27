package com.openai.interactivefitness.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "custom_workout_plans")
data class CustomWorkoutPlanEntity(
    @PrimaryKey val id: String,
    val title: String,
    val type: String,
    val durationMinutes: Int,
)

@Entity(
    tableName = "custom_plan_exercises",
    foreignKeys = [
        ForeignKey(
            entity = CustomWorkoutPlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("planId")],
)
data class CustomPlanExerciseEntity(
    @PrimaryKey val id: String,
    val planId: String,
    val exerciseId: String,
    val exerciseName: String,
    val position: Int,
    val restSeconds: Int,
)

@Entity(
    tableName = "custom_plan_sets",
    foreignKeys = [
        ForeignKey(
            entity = CustomPlanExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["planExerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("planExerciseId")],
)
data class CustomPlanSetEntity(
    @PrimaryKey val id: String,
    val planExerciseId: String,
    val position: Int,
    val weightKg: Double,
    val reps: Int,
)

data class CustomPlanExerciseWithSets(
    @Embedded val exercise: CustomPlanExerciseEntity,
    @Relation(parentColumn = "id", entityColumn = "planExerciseId")
    val sets: List<CustomPlanSetEntity>,
)

data class CustomWorkoutPlanWithDetails(
    @Embedded val plan: CustomWorkoutPlanEntity,
    @Relation(
        entity = CustomPlanExerciseEntity::class,
        parentColumn = "id",
        entityColumn = "planId",
    )
    val exercises: List<CustomPlanExerciseWithSets>,
)
