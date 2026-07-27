package com.openai.interactivefitness.domain

import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class RecommendationEngineTest {
    private val engine = RecommendationEngine()
    private val today = LocalDate.of(2026, 7, 26)

    @Test
    fun painStopsNormalRecommendation() {
        val result = engine.recommend(
            history = emptyList(),
            condition = DailyCondition(hasPain = true),
            today = today,
        )

        assertEquals(WorkoutType.RECOVERY, result.type)
        assertNotNull(result.safetyNotice)
    }

    @Test
    fun missingStrengthProducesStrengthRecommendation() {
        val run = WorkoutSession(
            type = WorkoutType.RUNNING,
            title = "달리기",
            startedAt = LocalDateTime.of(2026, 7, 25, 8, 0),
            durationMinutes = 30,
            rpe = 6,
            detail = "5 km",
        )

        val result = engine.recommend(listOf(run), DailyCondition(), today = today)

        assertEquals(WorkoutType.STRENGTH, result.type)
    }

    @Test
    fun matchingSavedCustomPlanIsUsedInRecommendation() {
        val plan = CustomWorkoutPlan(
            id = "plan-1",
            title = "내 전신 루틴",
            type = WorkoutType.STRENGTH,
            durationMinutes = 30,
            exercises = listOf("스쿼트 3×8", "푸시업 3×10"),
        )

        val result = engine.recommend(
            history = emptyList(),
            condition = DailyCondition(availableMinutes = 45),
            customPlans = listOf(plan),
            today = today,
        )

        assertEquals("내 전신 루틴", result.title)
        assertEquals(plan.exercises, result.exercises)
    }

    @Test
    fun highFatigueProducesRecoveryRecommendation() {
        val result = engine.recommend(
            history = emptyList(),
            condition = DailyCondition(fatigue = 5),
            today = today,
        )

        assertEquals(WorkoutType.RECOVERY, result.type)
    }
}
