package com.openai.interactivefitness.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.openai.interactivefitness.domain.WorkoutSession
import com.openai.interactivefitness.domain.WorkoutType
import java.time.LocalDateTime

@Entity(tableName = "workout_sessions")
data class WorkoutEntity(
    @PrimaryKey val id: String,
    val type: String,
    val title: String,
    val startedAt: String,
    val durationMinutes: Int,
    val rpe: Int,
    val detail: String,
    val sourceRecommendationId: String?,
    val recommendationDate: String?,
)

fun WorkoutEntity.toDomain(): WorkoutSession = WorkoutSession(
    id = id,
    type = WorkoutType.valueOf(type),
    title = title,
    startedAt = LocalDateTime.parse(startedAt),
    durationMinutes = durationMinutes,
    rpe = rpe,
    detail = detail,
    sourceRecommendationId = sourceRecommendationId,
    recommendationDate = recommendationDate?.let(java.time.LocalDate::parse),
)

fun WorkoutSession.toEntity(): WorkoutEntity = WorkoutEntity(
    id = id,
    type = type.name,
    title = title,
    startedAt = startedAt.toString(),
    durationMinutes = durationMinutes,
    rpe = rpe,
    detail = detail,
    sourceRecommendationId = sourceRecommendationId,
    recommendationDate = recommendationDate?.toString(),
)

@Entity(
    tableName = "strength_sets",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("workoutId")],
)
data class StrengthSetEntity(
    @PrimaryKey val id: String,
    val workoutId: String,
    val position: Int,
    val exercise: String,
    val weightKg: Double,
    val reps: Int,
    val rpe: Int,
)

@Entity(
    tableName = "workout_intervals",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("workoutId")],
)
data class WorkoutIntervalEntity(
    @PrimaryKey val id: String,
    val workoutId: String,
    val position: Int,
    val durationSeconds: Int,
    val distanceMeters: Int,
    val note: String,
)
