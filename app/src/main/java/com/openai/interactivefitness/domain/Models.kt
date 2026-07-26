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

data class WorkoutSession(
    val id: String = UUID.randomUUID().toString(),
    val type: WorkoutType,
    val title: String,
    val startedAt: LocalDateTime,
    val durationMinutes: Int,
    val rpe: Int,
    val detail: String,
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

data class WeeklySummary(
    val sessions: Int,
    val totalMinutes: Int,
    val strengthSessions: Int,
    val cardioSessions: Int,
    val goalProgress: Float,
)

data class WorkoutDraft(
    val id: String? = null,
    val type: WorkoutType = WorkoutType.STRENGTH,
    val title: String = "",
    val durationMinutes: String = "30",
    val rpe: String = "6",
    val detail: String = "",
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
        )
    }
}
