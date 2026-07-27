package com.openai.interactivefitness.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MenuSuggestionEngineTest {
    private val candidates = listOf(
        MenuCandidate("추천 운동", setOf("추천", "오늘", "운동")),
        MenuCandidate("수동 기록 저장", setOf("수동", "기록", "저장", "입력")),
    )

    @Test
    fun ranksTheClosestMenuWithoutExecutingIt() {
        assertEquals("수동 기록 저장", MenuSuggestionEngine().suggest("운동 기록 입력", candidates))
    }

    @Test
    fun unrelatedInputHasNoSuggestion() {
        assertNull(MenuSuggestionEngine().suggest("안녕하세요", candidates))
    }
}
