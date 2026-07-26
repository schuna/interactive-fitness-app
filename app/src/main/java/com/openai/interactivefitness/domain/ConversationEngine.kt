package com.openai.interactivefitness.domain

sealed interface ConversationIntent {
    data object ShowMenu : ConversationIntent
    data object RecommendToday : ConversationIntent
    data object StartRecommendation : ConversationIntent
    data object ShowDashboard : ConversationIntent
    data object ShowHistory : ConversationIntent
    data class QuickLog(val type: WorkoutType) : ConversationIntent
    data class Unknown(val originalText: String) : ConversationIntent
}

data class ConversationResult(
    val intent: ConversationIntent,
    val reply: String,
)

class ConversationEngine {
    fun interpret(text: String): ConversationResult {
        val normalized = text.trim().lowercase()
        if (normalized.isBlank()) {
            return ConversationResult(
                ConversationIntent.ShowMenu,
                "원하는 기능을 입력하거나 아래 메뉴를 선택해 주세요.",
            )
        }

        return when {
            normalized in setOf("메뉴", "도움말", "도움", "뭘 할 수 있어?", "뭘 할 수 있어") ->
                ConversationResult(
                    ConversationIntent.ShowMenu,
                    "오늘 운동 추천, 빠른 운동 기록, 기록 조회, 대시보드를 이용할 수 있어요.",
                )
            listOf("추천 운동 시작", "추천 시작", "운동 시작").any(normalized::contains) ->
                ConversationResult(
                    ConversationIntent.StartRecommendation,
                    "오늘의 추천 운동을 시작할게요.",
                )
            listOf("추천", "오늘 운동", "뭐 하지", "뭘 하지").any(normalized::contains) ->
                ConversationResult(
                    ConversationIntent.RecommendToday,
                    "오늘의 컨디션과 최근 기록을 반영한 추천을 보여드릴게요.",
                )
            listOf("대시보드", "분석", "통계", "주간").any(normalized::contains) ->
                ConversationResult(
                    ConversationIntent.ShowDashboard,
                    "최근 활동과 주간 목표를 확인해 보세요.",
                )
            listOf(
                "기록 조회",
                "기록 보기",
                "운동 기록",
                "히스토리",
                "최근 운동",
            ).any(normalized::contains) ->
                ConversationResult(
                    ConversationIntent.ShowHistory,
                    "운동 기록 화면으로 이동할게요.",
                )
            listOf("근력", "웨이트").any(normalized::contains) &&
                listOf("기록", "추가", "완료").any(normalized::contains) ->
                quickLog(WorkoutType.STRENGTH)
            listOf("달리기", "러닝").any(normalized::contains) &&
                listOf("기록", "추가", "완료").any(normalized::contains) ->
                quickLog(WorkoutType.RUNNING)
            listOf("사이클", "자전거").any(normalized::contains) &&
                listOf("기록", "추가", "완료").any(normalized::contains) ->
                quickLog(WorkoutType.CYCLING)
            else -> ConversationResult(
                ConversationIntent.Unknown(text.trim()),
                "이 문장은 안전하게 실행할 수 있는 명령으로 확인되지 않았어요. ‘메뉴’를 입력해 지원 기능을 확인해 주세요.",
            )
        }
    }

    private fun quickLog(type: WorkoutType) = ConversationResult(
        ConversationIntent.QuickLog(type),
        "${type.label} 운동을 기본값으로 기록할게요.",
    )
}
