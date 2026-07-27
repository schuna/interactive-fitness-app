package com.openai.interactivefitness.data

import com.openai.interactivefitness.domain.ConversationIntent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GeminiIntentRouterTest {
    @Test
    fun mapsSupportedFunctionAndPreservesParameters() {
        val result = mapGeminiFunction(
            "show_trend_analysis",
            mapOf("period" to "MONTH", "workoutType" to "RUNNING"),
        )

        assertEquals(ConversationIntent.ShowDashboard, result?.intent)
        assertEquals("MONTH", result?.parameters?.get("period"))
    }

    @Test
    fun rejectsUnknownFunction() {
        assertNull(mapGeminiFunction("delete_all_workouts"))
    }
}
