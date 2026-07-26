package com.openai.interactivefitness.data

import androidx.room.Entity
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
)

fun WorkoutEntity.toDomain(): WorkoutSession = WorkoutSession(
    id = id,
    type = WorkoutType.valueOf(type),
    title = title,
    startedAt = LocalDateTime.parse(startedAt),
    durationMinutes = durationMinutes,
    rpe = rpe,
    detail = detail,
)

fun WorkoutSession.toEntity(): WorkoutEntity = WorkoutEntity(
    id = id,
    type = type.name,
    title = title,
    startedAt = startedAt.toString(),
    durationMinutes = durationMinutes,
    rpe = rpe,
    detail = detail,
)
