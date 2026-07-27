package com.openai.interactivefitness.domain

sealed interface ConversationIntent {
    data object ShowMenu : ConversationIntent
    data object RecommendToday : ConversationIntent
    data object StartRecommendation : ConversationIntent
    data object ShowDashboard : ConversationIntent
    data object ShowHistory : ConversationIntent
    data object UpdateCondition : ConversationIntent
    data object ShowAccountSettings : ConversationIntent
    data object ManualLog : ConversationIntent
    data object CustomWorkoutPlan : ConversationIntent
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
                    "추천 운동, 커스텀 운동 계획, 수동 기록 저장, 기록 보기, 주간 분석을 이용할 수 있어요.",
                )
            normalized.contains("컨디션 업데이트") ||
                normalized.contains("컨디션 갱신") ||
                normalized.contains("컨디션 다시") ->
                ConversationResult(
                    ConversationIntent.UpdateCondition,
                    "오늘의 컨디션을 다시 입력할까요?",
                )
            listOf(
                "구글",
                "계정",
                "로그인",
                "로그아웃",
                "저장 방식",
                "데이터 저장",
            ).any(normalized::contains) ->
                ConversationResult(
                    ConversationIntent.ShowAccountSettings,
                    "계정 및 데이터 저장 방식을 설정에서 관리할까요?",
                )
            listOf("추천 운동 시작", "추천 시작", "운동 시작").any(normalized::contains) ->
                ConversationResult(
                    ConversationIntent.StartRecommendation,
                    "오늘의 추천 운동을 시작할까요?",
                )
            listOf("커스텀", "나만의 계획", "운동 계획 만들기", "루틴 만들기").any(normalized::contains) ->
                ConversationResult(
                    ConversationIntent.CustomWorkoutPlan,
                    "커스텀 운동 계획을 만들거나 저장된 계획을 실행할까요?",
                )
            listOf("추천", "오늘 운동", "뭐 하지", "뭘 하지").any(normalized::contains) ->
                ConversationResult(
                    ConversationIntent.RecommendToday,
                    "오늘의 컨디션과 최근 기록을 반영한 추천 운동을 확인할까요?",
                )
            listOf("대시보드", "분석", "통계", "주간").any(normalized::contains) ->
                ConversationResult(
                    ConversationIntent.ShowDashboard,
                    "최근 활동과 주간 목표 분석을 확인할까요?",
                )
            !listOf("저장", "추가", "입력").any(normalized::contains) && (normalized in setOf(
                "기록",
                "내 기록",
                "기록 보여줘",
                "운동 내역",
                "내역",
            ) || listOf(
                "기록 조회",
                "기록 보기",
                "운동 기록",
                "히스토리",
                "최근 운동",
            ).any(normalized::contains)) ->
                ConversationResult(
                    ConversationIntent.ShowHistory,
                    "운동 기록을 확인할까요?",
                )
            listOf("수동 기록", "기록 저장", "운동 입력", "직접 입력", "기록 추가")
                .any(normalized::contains) ||
                (normalized.endsWith("기록") && normalized != "기록") ->
                ConversationResult(
                    ConversationIntent.ManualLog,
                    "운동 내용을 직접 입력해 저장할까요?",
                )
            else -> ConversationResult(
                ConversationIntent.Unknown(text.trim()),
                "이 문장은 안전하게 실행할 수 있는 명령으로 확인되지 않았어요. ‘메뉴’를 입력해 지원 기능을 확인해 주세요.",
            )
        }
    }

}
