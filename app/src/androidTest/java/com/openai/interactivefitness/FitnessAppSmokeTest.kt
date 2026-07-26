package com.openai.interactivefitness

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test

class FitnessAppSmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun appStartsInChatAndMenuContainsPrimaryDestinations() {
        composeRule.onNodeWithText("메시지를 입력하세요").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("전체 메뉴 열기").performClick()

        composeRule.onNodeWithText("대시보드").assertIsDisplayed()
        composeRule.onNodeWithText("기록").assertIsDisplayed()
        composeRule.onNodeWithText("오늘").assertIsDisplayed()
    }

    @Test
    fun matchingCommandIsHighlightedWhileTyping() {
        composeRule.onNodeWithText("메시지를 입력하세요")
            .performClick()
            .performTextInput("이번 주 분석해줘")

        composeRule.onNodeWithText("이번 주 분석").assertIsSelected()
    }
}
