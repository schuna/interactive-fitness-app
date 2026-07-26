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
}
