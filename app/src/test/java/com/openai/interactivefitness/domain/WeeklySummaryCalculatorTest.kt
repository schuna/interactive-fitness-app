package com.openai.interactivefitness.domain

import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class WeeklySummaryCalculatorTest {
    private val today = LocalDate.of(2026, 7, 26)
    private val calculator = WeeklySummaryCalculator()

    @Test
    fun includesExactlySevenCalendarDaysAndExcludesFuture() {
        val result = calculator.calculate(
            listOf(
                workout(today.minusDays(6), WorkoutType.STRENGTH),
                workout(today.minusDays(7), WorkoutType.RUNNING),
                workout(today.plusDays(1), WorkoutType.CYCLING),
            ),
            today,
        )

        assertEquals(1, result.sessions)
        assertEquals(1, result.strengthSessions)
    }

    @Test
    fun multipleSessionsOnOneDayCountAsOneActiveDay() {
        val result = calculator.calculate(
            listOf(
                workout(today, WorkoutType.STRENGTH),
                workout(today, WorkoutType.RUNNING),
            ),
            today,
        )

        assertEquals(2, result.sessions)
        assertEquals(1, result.activeDays)
        assertEquals(0.5f, result.goalProgress)
    }

    @Test
    fun streakMayAnchorYesterdayButStopsAtGap() {
        val result = calculator.calculate(
            listOf(
                workout(today.minusDays(1), WorkoutType.RUNNING),
                workout(today.minusDays(2), WorkoutType.STRENGTH),
                workout(today.minusDays(4), WorkoutType.STRENGTH),
            ),
            today,
        )

        assertEquals(2, result.currentStreakDays)
    }

    private fun workout(date: LocalDate, type: WorkoutType) = WorkoutSession(
        type = type,
        title = "테스트",
        startedAt = LocalDateTime.of(date, java.time.LocalTime.NOON),
        durationMinutes = 30,
        rpe = 5,
        detail = "",
    )
}
