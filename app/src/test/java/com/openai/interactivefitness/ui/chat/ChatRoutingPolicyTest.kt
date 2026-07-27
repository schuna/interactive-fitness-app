package com.openai.interactivefitness.ui.chat

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatRoutingPolicyTest {
    @Test
    fun signedInUserUsesAiFirstWhenRouterIsAvailable() {
        assertTrue(shouldUseAiFirst(isGoogleSignedIn = true, hasGeminiIntentRouter = true))
    }

    @Test
    fun signedOutUserAndMissingRouterDoNotUseAiFirst() {
        assertFalse(shouldUseAiFirst(isGoogleSignedIn = false, hasGeminiIntentRouter = true))
        assertFalse(shouldUseAiFirst(isGoogleSignedIn = true, hasGeminiIntentRouter = false))
    }
}
