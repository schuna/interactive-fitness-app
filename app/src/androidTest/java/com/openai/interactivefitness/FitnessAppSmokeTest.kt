package com.openai.interactivefitness

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class FitnessAppSmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun appStartsAndPrimaryNavigationIsVisible() {
        composeRule.onNodeWithText("오늘").assertIsDisplayed()
        composeRule.onNodeWithText("대화").assertIsDisplayed()
        composeRule.onNodeWithText("대시보드").assertIsDisplayed()
        composeRule.onNodeWithText("기록").assertIsDisplayed()
    }
}
