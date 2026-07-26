package com.openai.interactivefitness.domain

import java.time.LocalDate

class WeeklySummaryCalculator(
    private val weeklyGoal: Int = 4,
) {
    fun calculate(
        workouts: List<WorkoutSession>,
        today: LocalDate = LocalDate.now(),
    ): WeeklySummary {
        val firstDay = today.minusDays(6)
        val recent = workouts.filter {
            val date = it.startedAt.toLocalDate()
            !date.isBefore(firstDay) && !date.isAfter(today)
        }
        val workoutDates = workouts
            .map { it.startedAt.toLocalDate() }
            .filterNot { it.isAfter(today) }
            .toSet()
        val streakAnchor = when {
            today in workoutDates -> today
            today.minusDays(1) in workoutDates -> today.minusDays(1)
            else -> null
        }
        val streak = generateSequence(streakAnchor) { it.minusDays(1) }
            .takeWhile { it in workoutDates }
            .count()

        return WeeklySummary(
            sessions = recent.size,
            totalMinutes = recent.sumOf(WorkoutSession::durationMinutes),
            strengthSessions = recent.count { it.type == WorkoutType.STRENGTH },
            cardioSessions = recent.count {
                it.type == WorkoutType.RUNNING || it.type == WorkoutType.CYCLING
            },
            goalProgress = (recent.size / weeklyGoal.toFloat()).coerceIn(0f, 1f),
            activeDays = recent.map { it.startedAt.toLocalDate() }.distinct().size,
            currentStreakDays = streak,
        )
    }
}
