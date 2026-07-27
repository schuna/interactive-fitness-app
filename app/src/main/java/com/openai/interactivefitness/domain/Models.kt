package com.openai.interactivefitness.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

enum class WorkoutType(val label: String) {
    STRENGTH("웨이트"),
    RUNNING("달리기"),
    CYCLING("사이클"),
    RECOVERY("회복"),
}

enum class WorkoutDataSource {
    LOCAL,
    HEALTH_CONNECT,
}

enum class HealthConnectSyncState {
    NOT_SYNCED,
    IMPORTED,
    EXPORTED,
}

data class WorkoutSession(
    val id: String = UUID.randomUUID().toString(),
    val type: WorkoutType,
    val title: String,
    val startedAt: LocalDateTime,
    val durationMinutes: Int,
    val rpe: Int,
    val detail: String,
    val strengthSets: List<StrengthSet> = emptyList(),
    val intervals: List<WorkoutInterval> = emptyList(),
    val sourceRecommendationId: String? = null,
    val recommendationDate: LocalDate? = null,
    val dataSource: WorkoutDataSource = WorkoutDataSource.LOCAL,
    val externalRecordId: String? = null,
    val sourceModifiedAt: LocalDateTime? = null,
    val healthConnectSyncState: HealthConnectSyncState = HealthConnectSyncState.NOT_SYNCED,
    val lastSyncedAt: LocalDateTime? = null,
)

data class StrengthSet(
    val id: String = UUID.randomUUID().toString(),
    val exercise: String,
    val weightKg: Double,
    val reps: Int,
    val rpe: Int,
)

data class WorkoutInterval(
    val id: String = UUID.randomUUID().toString(),
    val durationSeconds: Int,
    val distanceMeters: Int,
    val note: String = "",
)

data class ActiveWorkout(
    val recommendation: Recommendation,
    val startedAt: LocalDateTime,
    val steps: List<String>,
    val currentStepIndex: Int = 0,
    val completedSteps: Int = 0,
    val restSecondsRemaining: Int = 0,
    val isResting: Boolean = false,
    val restEndsAtEpochMillis: Long? = null,
) {
    val isLastStep: Boolean
        get() = currentStepIndex >= steps.lastIndex

    val progress: Float
        get() = if (steps.isEmpty()) 0f else completedSteps.toFloat() / steps.size

    fun completeCurrentStep(
        defaultRestSeconds: Int = 60,
        nowEpochMillis: Long = System.currentTimeMillis(),
    ): ActiveWorkout =
        if (steps.isEmpty()) {
            this
        } else {
            copy(
                completedSteps = (completedSteps + 1).coerceAtMost(steps.size),
                isResting = !isLastStep,
                restSecondsRemaining = if (isLastStep) 0 else defaultRestSeconds,
                restEndsAtEpochMillis = if (isLastStep) {
                    null
                } else {
                    nowEpochMillis + defaultRestSeconds * 1_000L
                },
            )
        }

    fun moveToNextStep(): ActiveWorkout =
        copy(
            currentStepIndex = (currentStepIndex + 1).coerceAtMost(steps.lastIndex),
            isResting = false,
            restSecondsRemaining = 0,
            restEndsAtEpochMillis = null,
        )

    fun restoredAt(nowEpochMillis: Long = System.currentTimeMillis()): ActiveWorkout {
        if (!isResting) return this
        val remaining = restEndsAtEpochMillis
            ?.minus(nowEpochMillis)
            ?.let { ((it + 999L) / 1_000L).toInt().coerceAtLeast(0) }
            ?: restSecondsRemaining
        return if (remaining <= 0) moveToNextStep()
        else copy(restSecondsRemaining = remaining)
    }
}

enum class ErrorCategory {
    DATABASE,
    FIREBASE,
    HEALTH_CONNECT,
    RECOMMENDATION,
    STATE_RESTORE,
    UNKNOWN,
}

data class AppError(
    val code: String,
    val category: ErrorCategory,
    val userMessage: String,
    val operation: String,
    val occurredAt: LocalDateTime = LocalDateTime.now(),
    val isRecoverable: Boolean = true,
)

data class DailyCondition(
    val fatigue: Int = 2,
    val soreness: Int = 1,
    val hasPain: Boolean = false,
    val availableMinutes: Int = 45,
)

data class Recommendation(
    val id: String,
    val date: LocalDate,
    val type: WorkoutType,
    val title: String,
    val reason: String,
    val durationMinutes: Int,
    val difficulty: String,
    val exercises: List<String>,
    val safetyNotice: String? = null,
)

data class CustomWorkoutPlan(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val type: WorkoutType,
    val durationMinutes: Int,
    val exercises: List<String>,
    val plannedExercises: List<PlannedExercise> = emptyList(),
) {
    fun executionSteps(): List<String> =
        if (plannedExercises.isEmpty()) {
            exercises
        } else {
            plannedExercises.flatMap { exercise ->
                exercise.sets.mapIndexed { index, set ->
                    buildString {
                        append(exercise.exerciseName)
                        append(" · ${index + 1}세트 · ")
                        if (set.weightKg > 0) {
                            append(
                                if (set.weightKg % 1.0 == 0.0) {
                                    "${set.weightKg.toInt()}kg"
                                } else {
                                    "${set.weightKg}kg"
                                },
                            )
                            append(" × ")
                        }
                        append("${set.reps}회")
                    }
                }
            }
        }
}

data class PlannedExercise(
    val id: String = UUID.randomUUID().toString(),
    val exerciseId: String,
    val exerciseName: String,
    val sets: List<PlannedSet>,
    val restSeconds: Int = 60,
) {
    val totalVolumeKg: Double
        get() = sets.sumOf { it.weightKg * it.reps }
}

data class PlannedSet(
    val id: String = UUID.randomUUID().toString(),
    val weightKg: Double = 0.0,
    val reps: Int = 10,
)

data class WeeklySummary(
    val sessions: Int,
    val totalMinutes: Int,
    val strengthSessions: Int,
    val cardioSessions: Int,
    val goalProgress: Float,
    val activeDays: Int = 0,
    val currentStreakDays: Int = 0,
)

data class WorkoutDraft(
    val id: String? = null,
    val type: WorkoutType = WorkoutType.STRENGTH,
    val title: String = "",
    val durationMinutes: String = "30",
    val rpe: String = "6",
    val detail: String = "",
    val strengthSets: List<StrengthSet> = emptyList(),
    val intervals: List<WorkoutInterval> = emptyList(),
) {
    fun validate(): Map<String, String> = buildMap {
        if (title.isBlank()) put("title", "운동 제목을 입력하세요.")
        val duration = durationMinutes.toIntOrNull()
        if (duration == null || duration !in 1..1_440) {
            put("durationMinutes", "운동 시간은 1~1440분 사이여야 합니다.")
        }
        val parsedRpe = rpe.toIntOrNull()
        if (parsedRpe == null || parsedRpe !in 1..10) {
            put("rpe", "RPE는 1~10 사이여야 합니다.")
        }
        if (strengthSets.any {
                it.exercise.isBlank() || it.weightKg < 0 || it.reps < 1 || it.rpe !in 1..10
            }
        ) {
            put("strengthSets", "세트의 종목, 중량, 반복 수와 RPE를 확인하세요.")
        }
        if (intervals.any { it.durationSeconds < 1 || it.distanceMeters < 0 }) {
            put("intervals", "인터벌의 시간과 거리를 확인하세요.")
        }
    }

    fun toSession(
        existing: WorkoutSession? = null,
        now: LocalDateTime = LocalDateTime.now(),
    ): WorkoutSession {
        require(validate().isEmpty()) { "Invalid workout draft" }
        return WorkoutSession(
            id = existing?.id ?: id ?: UUID.randomUUID().toString(),
            type = type,
            title = title.trim(),
            startedAt = existing?.startedAt ?: now.minusMinutes(durationMinutes.toLong()),
            durationMinutes = durationMinutes.toInt(),
            rpe = rpe.toInt(),
            detail = detail.trim(),
            strengthSets = strengthSets,
            intervals = intervals,
        )
    }
}
