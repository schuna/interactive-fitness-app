package com.openai.interactivefitness.domain

import java.time.LocalDate

class RecommendationEngine {
    fun recommend(
        history: List<WorkoutSession>,
        condition: DailyCondition,
        today: LocalDate = LocalDate.now(),
    ): Recommendation {
        if (condition.hasPain) {
            return Recommendation(
                id = "recovery-$today",
                date = today,
                type = WorkoutType.RECOVERY,
                title = "휴식과 가벼운 회복",
                reason = "통증 신호가 있어 일반 운동 추천을 중단했습니다.",
                durationMinutes = 15,
                difficulty = "매우 낮음",
                exercises = listOf("편안한 호흡", "통증 없는 범위의 가벼운 움직임"),
                safetyNotice = "통증이 지속되거나 심해지면 운동을 멈추고 전문가와 상담하세요.",
            )
        }

        val recent = history.filter { !it.startedAt.toLocalDate().isBefore(today.minusDays(6)) }
        val strengthCount = recent.count { it.type == WorkoutType.STRENGTH }
        val cardioCount = recent.count {
            it.type == WorkoutType.RUNNING || it.type == WorkoutType.CYCLING
        }

        if (condition.fatigue >= 4 || condition.soreness >= 4) {
            return Recommendation(
                id = "recovery-$today",
                date = today,
                type = WorkoutType.RECOVERY,
                title = "회복 중심 루틴",
                reason = "오늘 입력한 피로도와 근육통이 높습니다.",
                durationMinutes = minOf(condition.availableMinutes, 25),
                difficulty = "낮음",
                exercises = listOf("가벼운 걷기 10분", "전신 모빌리티 10분", "호흡 정리 5분"),
            )
        }

        return if (strengthCount <= cardioCount) {
            Recommendation(
                id = "strength-$today",
                date = today,
                type = WorkoutType.STRENGTH,
                title = "전신 근력 기초",
                reason = "최근 7일간 유산소에 비해 근력 운동 비중이 낮습니다.",
                durationMinutes = minOf(condition.availableMinutes, 45),
                difficulty = "중간",
                exercises = listOf("스쿼트 3×8", "푸시업 3×10", "로우 3×10", "플랭크 3×30초"),
            )
        } else {
            Recommendation(
                id = "cardio-$today",
                date = today,
                type = WorkoutType.RUNNING,
                title = "편안한 지속주",
                reason = "최근 근력 운동 비중이 높아 균형을 위한 유산소를 추천합니다.",
                durationMinutes = minOf(condition.availableMinutes, 35),
                difficulty = "중간",
                exercises = listOf("워밍업 걷기 5분", "편안한 달리기 25분", "정리 걷기 5분"),
            )
        }
    }
}
