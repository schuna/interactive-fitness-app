package com.openai.interactivefitness.domain

import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActiveWorkoutTest {
    private val recommendation = Recommendation(
        id = "test",
        date = LocalDate.of(2026, 7, 26),
        type = WorkoutType.STRENGTH,
        title = "테스트 운동",
        reason = "테스트",
        durationMinutes = 30,
        difficulty = "중간",
        exercises = listOf("스쿼트", "푸시업"),
    )

    @Test
    fun completingStepStartsRestAndAdvancingClearsIt() {
        val active = ActiveWorkout(
            recommendation = recommendation,
            startedAt = LocalDateTime.of(2026, 7, 26, 9, 0),
            steps = recommendation.exercises,
        )

        val resting = active.completeCurrentStep(defaultRestSeconds = 60)
        assertTrue(resting.isResting)
        assertEquals(60, resting.restSecondsRemaining)
        assertEquals(1, resting.completedSteps)

        val next = resting.moveToNextStep()
        assertFalse(next.isResting)
        assertEquals(1, next.currentStepIndex)
    }

    @Test
    fun lastStepCompletesWithoutStartingRest() {
        val last = ActiveWorkout(
            recommendation = recommendation,
            startedAt = LocalDateTime.of(2026, 7, 26, 9, 0),
            steps = recommendation.exercises,
            currentStepIndex = 1,
            completedSteps = 1,
        ).completeCurrentStep()

        assertFalse(last.isResting)
        assertEquals(2, last.completedSteps)
        assertEquals(1f, last.progress)
    }

    @Test
    fun restoredRestUsesAbsoluteEndTime() {
        val resting = ActiveWorkout(
            recommendation = recommendation,
            startedAt = LocalDateTime.of(2026, 7, 26, 9, 0),
            steps = recommendation.exercises,
            completedSteps = 1,
            isResting = true,
            restSecondsRemaining = 60,
            restEndsAtEpochMillis = 130_000L,
        )

        val restored = resting.restoredAt(nowEpochMillis = 100_000L)

        assertTrue(restored.isResting)
        assertEquals(30, restored.restSecondsRemaining)
    }

    @Test
    fun expiredRestAdvancesToNextStep() {
        val resting = ActiveWorkout(
            recommendation = recommendation,
            startedAt = LocalDateTime.of(2026, 7, 26, 9, 0),
            steps = recommendation.exercises,
            completedSteps = 1,
            isResting = true,
            restSecondsRemaining = 60,
            restEndsAtEpochMillis = 90_000L,
        )

        val restored = resting.restoredAt(nowEpochMillis = 100_000L)

        assertFalse(restored.isResting)
        assertEquals(1, restored.currentStepIndex)
    }
}
