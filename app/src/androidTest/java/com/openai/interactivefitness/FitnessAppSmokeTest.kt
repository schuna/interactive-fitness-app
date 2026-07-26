package com.openai.interactivefitness

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class FitnessAppSmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun appStartsInChatAndDrawerContainsSettingsAndDiagnostics() {
        composeRule.onNodeWithText("메시지를 입력하세요").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("설정 열기").performClick()

        composeRule.onAllNodesWithText("설정").assertCountEquals(2)
        composeRule.onNodeWithText("진단 및 동기화").assertIsDisplayed()
    }

    @Test
    fun settingsScreenProvidesThemeAndAppInformation() {
        composeRule.onNodeWithContentDescription("설정 열기").performClick()
        composeRule.onNode(hasText("설정") and hasClickAction()).performClick()

        composeRule.onNodeWithText("테마").assertIsDisplayed()
        composeRule.onNodeWithText("시스템").assertIsDisplayed()
        composeRule.onNodeWithText("라이트").assertIsDisplayed()
        composeRule.onNodeWithText("다크").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("앱 정보").assertIsDisplayed()
        composeRule.onNodeWithText("버전 0.1.0 (1)").assertIsDisplayed()
    }

    @Test
    fun matchingCommandIsHighlightedWhileTyping() {
        composeRule.onNodeWithText("메시지를 입력하세요")
            .performClick()
            .performTextInput("이번 주 분석해줘")

        composeRule.onNodeWithText("이번 주 분석").assertIsSelected()
    }

    @Test
    fun sendButtonClearsFocusAndShowsLatestReply() {
        val input = composeRule.onNodeWithText("메시지를 입력하세요")
        input.performClick().performTextInput("메뉴")

        composeRule.onNodeWithContentDescription("메시지 보내기").performClick()

        input.assertIsNotFocused()
        composeRule.onNodeWithText(
            "오늘 운동 추천, 빠른 운동 기록, 기록 조회, 대시보드를 이용할 수 있어요.",
        ).assertIsDisplayed()
    }

    @Test
    fun repeatedCommandCreatesANewConversationExchange() {
        val input = composeRule.onNodeWithText("메시지를 입력하세요")
        repeat(2) {
            input.performClick().performTextInput("메뉴")
            composeRule.onNodeWithContentDescription("메시지 보내기").performClick()
        }

        composeRule.onAllNodesWithText(
            "오늘 운동 추천, 빠른 운동 기록, 기록 조회, 대시보드를 이용할 수 있어요.",
        ).assertCountEquals(2)
    }

    @Test
    fun backFromDashboardReturnsToChat() {
        composeRule.onNodeWithText("이번 주 분석").performClick()
        composeRule.onNodeWithText("이번 주 분석 보기").performClick()
        composeRule.onNodeWithText("이번 주").assertIsDisplayed()

        composeRule.runOnIdle {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }

        composeRule.onNodeWithText("메시지를 입력하세요").assertIsDisplayed()
    }

    @Test
    fun recommendationReplyProvidesNavigationButton() {
        composeRule.onNodeWithText("오늘 운동").performClick()
        composeRule.onNodeWithText("추천 운동 보기").assertIsDisplayed().performClick()

        composeRule.onNodeWithText("오늘의 운동").assertIsDisplayed()
    }

    @Test
    fun historyPhraseShowsConfirmationAndActionBeforeNavigation() {
        composeRule.onNodeWithText("메시지를 입력하세요")
            .performClick()
            .performTextInput("운동 기록 보여줘")
        composeRule.onNodeWithContentDescription("메시지 보내기").performClick()

        composeRule.onNodeWithText("운동 기록을 확인할까요?").assertIsDisplayed()
        composeRule.onNodeWithText("운동 기록 보기").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("운동 기록").assertIsDisplayed()
    }

    @Test
    fun runningRecordCommandOpensPrefilledEditorInsteadOfSavingImmediately() {
        composeRule.onNodeWithText("메시지를 입력하세요")
            .performClick()
            .performTextInput("러닝 기록")
        composeRule.onNodeWithContentDescription("메시지 보내기").performClick()

        composeRule.onNodeWithText("달리기 운동 기록을 직접 입력할까요?").assertIsDisplayed()
        composeRule.onNodeWithText("운동 기록 입력").performClick()

        composeRule.onNodeWithText("새 운동 기록").assertIsDisplayed()
        composeRule.onNodeWithText("달리기 운동").assertIsDisplayed()
    }

    @Test
    fun accountPhraseShowsSettingsActionBeforeNavigation() {
        composeRule.onNodeWithText("메시지를 입력하세요")
            .performClick()
            .performTextInput("구글 계정 변경")
        composeRule.onNodeWithContentDescription("메시지 보내기").performClick()

        composeRule.onNodeWithText(
            "계정 및 데이터 저장 방식을 설정에서 관리할까요?",
        ).assertIsDisplayed()
        composeRule.onNodeWithText("계정 설정 열기").performClick()
        composeRule.onNodeWithText("계정 및 저장 방식").assertIsDisplayed()
    }

    @Test
    fun submittedConditionIsHiddenUntilUpdateIsRequested() {
        composeRule.activity.getSharedPreferences("daily_condition", 0)
            .edit()
            .putString("date", LocalDate.now().toString())
            .putInt("fatigue", 2)
            .putInt("soreness", 1)
            .putBoolean("has_pain", false)
            .commit()
        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("오늘의 컨디션을 알려주세요").assertDoesNotExist()
        composeRule.onNodeWithText("메시지를 입력하세요")
            .performClick()
            .performTextInput("컨디션 업데이트해줘")
        composeRule.onNodeWithContentDescription("메시지 보내기").performClick()
        composeRule.onNodeWithText("컨디션 다시 입력").performClick()

        composeRule.onNodeWithText("오늘의 컨디션을 알려주세요").assertIsDisplayed()
    }

    @Test
    fun backFromActiveRecommendationReturnsToChat() {
        composeRule.onNodeWithText("메시지를 입력하세요")
            .performClick()
            .performTextInput("추천 운동 시작")
        composeRule.onNodeWithContentDescription("메시지 보내기").performClick()
        composeRule.onNodeWithText("운동 시작하기").performClick()
        composeRule.onNodeWithText("운동 진행").assertIsDisplayed()

        composeRule.runOnIdle {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }

        composeRule.onNodeWithText("메시지를 입력하세요").assertIsDisplayed()
    }
}
