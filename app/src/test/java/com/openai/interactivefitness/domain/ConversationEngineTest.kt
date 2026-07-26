package com.openai.interactivefitness.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationEngineTest {
    private val engine = ConversationEngine()

    @Test
    fun menuPhraseShowsSupportedCommands() {
        assertEquals(ConversationIntent.ShowMenu, engine.interpret("도움말").intent)
    }

    @Test
    fun workoutTextBecomesValidatedQuickLogIntent() {
        assertEquals(
            ConversationIntent.QuickLog(WorkoutType.RUNNING),
            engine.interpret("러닝 기록 추가").intent,
        )
    }

    @Test
    fun ambiguousTextDoesNotExecuteAnAction() {
        assertTrue(engine.interpret("적당히 알아서 해줘").intent is ConversationIntent.Unknown)
    }

    @Test
    fun visibleChatCommandsMapToExecutableIntents() {
        assertEquals(
            ConversationIntent.StartRecommendation,
            engine.interpret("추천 운동 시작").intent,
        )
        assertEquals(
            ConversationIntent.ShowHistory,
            engine.interpret("기록 보기").intent,
        )
        assertEquals(
            ConversationIntent.ShowDashboard,
            engine.interpret("이번 주 분석").intent,
        )
    }

    @Test
    fun historyKeywordReturnsConfirmationBeforeNavigation() {
        val result = engine.interpret("운동 기록 보여줘")

        assertEquals(ConversationIntent.ShowHistory, result.intent)
        assertEquals("운동 기록을 확인할까요?", result.reply)
    }

    @Test
    fun shortHistoryPhrasesAlsoConnectToHistory() {
        listOf("기록", "내 기록", "기록 보여줘", "운동 내역").forEach { phrase ->
            assertEquals(
                "Failed phrase: $phrase",
                ConversationIntent.ShowHistory,
                engine.interpret(phrase).intent,
            )
        }
    }

    @Test
    fun supportedActionsUseConfirmationQuestions() {
        assertEquals(
            "오늘의 컨디션과 최근 기록을 반영한 추천 운동을 확인할까요?",
            engine.interpret("오늘 운동 추천해줘").reply,
        )
        assertEquals(
            "최근 활동과 주간 목표 분석을 확인할까요?",
            engine.interpret("이번 주 분석 보여줘").reply,
        )
        assertEquals(
            "달리기 운동 기록을 직접 입력할까요?",
            engine.interpret("러닝 기록 추가").reply,
        )
    }

    @Test
    fun conditionUpdatePhraseRequestsConditionEditor() {
        val result = engine.interpret("오늘 컨디션 업데이트하고 싶어")

        assertEquals(ConversationIntent.UpdateCondition, result.intent)
        assertEquals("오늘의 컨디션을 다시 입력할까요?", result.reply)
    }

    @Test
    fun accountPhrasesRequestSettingsNavigation() {
        listOf("구글 로그인", "계정 변경", "데이터 저장 방식").forEach { phrase ->
            assertEquals(
                ConversationIntent.ShowAccountSettings,
                engine.interpret(phrase).intent,
            )
        }
    }
}
