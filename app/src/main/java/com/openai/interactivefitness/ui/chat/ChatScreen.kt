package com.openai.interactivefitness.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.openai.interactivefitness.R
import com.openai.interactivefitness.domain.ConversationEngine
import com.openai.interactivefitness.domain.ConversationIntent
import com.openai.interactivefitness.domain.ConversationResult
import com.openai.interactivefitness.domain.DailyCondition
import com.openai.interactivefitness.domain.MenuCandidate
import com.openai.interactivefitness.domain.MenuSuggestionEngine
import com.openai.interactivefitness.domain.CustomPlanPrefill
import com.openai.interactivefitness.domain.customPlanPrefill
import com.openai.interactivefitness.domain.workoutTypeOrNull
import com.openai.interactivefitness.data.GeminiIntentRouter
import com.openai.interactivefitness.domain.WorkoutType
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlinx.coroutines.launch

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val time: String = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")),
    val action: ChatMessageAction? = null,
    val parameters: Map<String, String> = emptyMap(),
)

enum class ChatMessageAction {
    SHOW_RECOMMENDATION,
    START_RECOMMENDATION,
    SHOW_DASHBOARD,
    SHOW_HISTORY,
    SHOW_COMPLETED_WORKOUT,
    UPDATE_CONDITION,
    SHOW_ACCOUNT_SETTINGS,
    MANUAL_LOG,
    SAVED_CUSTOM_PLANS,
    CUSTOM_PLAN,
}

@Stable
class ChatConversationState {
    val messages = mutableStateListOf<ChatMessage>()

    fun addExchange(
        userText: String,
        coachText: String,
        action: ChatMessageAction? = null,
        parameters: Map<String, String> = emptyMap(),
    ) {
        messages += ChatMessage(text = userText, isUser = true)
        messages += ChatMessage(
            text = coachText,
            isUser = false,
            action = action,
            parameters = parameters,
        )
    }

    fun addCoachMessage(text: String) {
        messages += ChatMessage(text = text, isUser = false)
    }
}

@Composable
fun rememberChatConversationState(): ChatConversationState =
    remember { ChatConversationState() }

private data class ChatQuickAction(
    val label: String,
    val command: String,
    val icon: ImageVector,
    val keywords: List<String>,
)

@Composable
fun ChatScreen(
    conversationState: ChatConversationState,
    geminiIntentRouter: GeminiIntentRouter?,
    isGoogleSignedIn: Boolean,
    condition: DailyCondition,
    onFatigueChanged: (Int) -> Unit,
    onSorenessChanged: (Int) -> Unit,
    onPainChanged: (Boolean) -> Unit,
    isConditionSubmittedToday: Boolean,
    onConditionSubmitted: (DailyCondition) -> Unit,
    onManualLog: (WorkoutType?) -> Unit,
    onOpenSavedCustomPlans: () -> Unit,
    onCreateCustomPlan: (CustomPlanPrefill) -> Unit,
    onStart: () -> Unit,
    isCompleted: Boolean,
    onOpenRecommendation: () -> Unit,
    onOpenDashboard: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val conversationEngine = remember { ConversationEngine() }
    val menuSuggestionEngine = remember { MenuSuggestionEngine() }
    val coroutineScope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    var showConditionEditor by remember {
        mutableStateOf(false)
    }
    val greeting = stringResource(R.string.chat_greeting)
    val messages = conversationState.messages
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val conditionAppliedMessage = stringResource(R.string.condition_applied)
    val compactScreen = LocalConfiguration.current.screenWidthDp <= 360
    val quickActions = listOf(
        ChatQuickAction(
            "추천 운동",
            "오늘 운동 추천",
            Icons.Outlined.Spa,
            listOf("오늘", "추천", "운동"),
        ),
        ChatQuickAction(
            "저장된 운동 계획",
            "저장된 운동 계획",
            Icons.Outlined.FitnessCenter,
            listOf("저장", "계획", "목록", "루틴"),
        ),
        ChatQuickAction(
            "새 운동 계획 만들기",
            "새 운동 계획 만들기",
            Icons.Outlined.FitnessCenter,
            listOf("새", "만들기", "계획", "루틴"),
        ),
        ChatQuickAction(
            "수동 기록 저장",
            "수동 기록 저장",
            Icons.Outlined.FitnessCenter,
            listOf("수동", "기록", "저장", "입력"),
        ),
        ChatQuickAction(
            stringResource(R.string.chat_action_weekly),
            "이번 주 분석",
            Icons.Outlined.AutoGraph,
            listOf("이번 주", "주간", "분석", "통계"),
        ),
        ChatQuickAction(
            stringResource(R.string.chat_action_history),
            "기록 보기",
            Icons.Outlined.History,
            listOf("기록", "최근", "지난"),
        ),
    )
    val suggestedCommand = menuSuggestionEngine.suggest(
        input,
        quickActions.map { MenuCandidate(it.command, it.keywords.toSet()) },
    )

    fun showResult(
        command: String,
        result: ConversationResult,
        parameters: Map<String, String> = emptyMap(),
    ) {
        val action = when (result.intent) {
            ConversationIntent.RecommendToday ->
                if (isCompleted) {
                    ChatMessageAction.SHOW_COMPLETED_WORKOUT
                } else {
                    ChatMessageAction.SHOW_RECOMMENDATION
                }
            ConversationIntent.StartRecommendation ->
                if (isCompleted) {
                    ChatMessageAction.SHOW_COMPLETED_WORKOUT
                } else {
                    ChatMessageAction.START_RECOMMENDATION
                }
            ConversationIntent.ShowDashboard -> ChatMessageAction.SHOW_DASHBOARD
            ConversationIntent.ShowHistory -> ChatMessageAction.SHOW_HISTORY
            ConversationIntent.UpdateCondition -> ChatMessageAction.UPDATE_CONDITION
            ConversationIntent.ShowAccountSettings -> ChatMessageAction.SHOW_ACCOUNT_SETTINGS
            ConversationIntent.ManualLog -> ChatMessageAction.MANUAL_LOG
            ConversationIntent.ShowCustomWorkoutPlans -> ChatMessageAction.SAVED_CUSTOM_PLANS
            ConversationIntent.CustomWorkoutPlan -> ChatMessageAction.CUSTOM_PLAN
            ConversationIntent.ShowMenu,
            is ConversationIntent.Unknown,
            -> null
        }
        val reply = if (
            isCompleted &&
            result.intent in setOf(
                ConversationIntent.RecommendToday,
                ConversationIntent.StartRecommendation,
            )
        ) {
            "오늘의 추천 운동을 이미 완료했어요. 완료한 운동 기록을 확인할까요?"
        } else {
            result.reply
        }
        conversationState.addExchange(command.trim(), reply, action, parameters)
    }

    fun submit(command: String = input) {
        if (command.isBlank()) return
        val trimmed = command.trim()
        input = ""
        focusManager.clearFocus()
        keyboardController?.hide()
        val localResult = conversationEngine.interpret(trimmed)
        if (shouldUseAiFirst(isGoogleSignedIn, geminiIntentRouter != null)) {
            coroutineScope.launch {
                val aiResult = runCatching { geminiIntentRouter?.route(trimmed) }.getOrNull()
                if (aiResult == null) {
                    showResult(trimmed, localResult)
                } else {
                    showResult(
                        trimmed,
                        ConversationResult(intent = aiResult.intent, reply = aiResult.reply),
                        aiResult.parameters,
                    )
                }
            }
            return
        }
        if (localResult.intent !is ConversationIntent.Unknown || geminiIntentRouter == null) {
            showResult(trimmed, localResult)
            return
        }
        coroutineScope.launch {
            val aiResult = runCatching { geminiIntentRouter.route(trimmed) }.getOrNull()
            if (aiResult == null) {
                showResult(trimmed, localResult)
            } else {
                showResult(
                    trimmed,
                    ConversationResult(intent = aiResult.intent, reply = aiResult.reply),
                    aiResult.parameters,
                )
            }
        }
    }

    LaunchedEffect(messages.size, showConditionEditor) {
        if (showConditionEditor && isConditionSubmittedToday) {
            listState.animateScrollToItem(1)
        } else if (messages.isNotEmpty()) {
            val staticItemCount = if (showConditionEditor) 2 else 1
            listState.animateScrollToItem(staticItemCount + messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(if (compactScreen) 12.dp else 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                ChatMessageBubble(ChatMessage(text = greeting, isUser = false))
            }
            if (showConditionEditor) item {
                ChatConditionCard(
                    condition = condition,
                    onFatigueChanged = onFatigueChanged,
                    onSorenessChanged = onSorenessChanged,
                    onPainChanged = onPainChanged,
                    onApply = {
                        onConditionSubmitted(condition)
                        conversationState.addCoachMessage(conditionAppliedMessage)
                        showConditionEditor = false
                    },
                )
            }
            items(messages, key = ChatMessage::id) { message ->
                val actionLabel = when (message.action) {
                    ChatMessageAction.SHOW_RECOMMENDATION ->
                        stringResource(R.string.chat_action_open_recommendation)
                    ChatMessageAction.START_RECOMMENDATION ->
                        stringResource(R.string.chat_action_start_workout)
                    ChatMessageAction.SHOW_DASHBOARD ->
                        stringResource(R.string.chat_action_open_dashboard)
                    ChatMessageAction.SHOW_HISTORY ->
                        stringResource(R.string.chat_action_open_history)
                    ChatMessageAction.SHOW_COMPLETED_WORKOUT ->
                        stringResource(R.string.chat_action_open_completed_workout)
                    ChatMessageAction.UPDATE_CONDITION ->
                        stringResource(R.string.chat_action_update_condition)
                    ChatMessageAction.SHOW_ACCOUNT_SETTINGS ->
                        stringResource(R.string.chat_action_open_account_settings)
                    ChatMessageAction.MANUAL_LOG -> "수동 기록 입력"
                    ChatMessageAction.SAVED_CUSTOM_PLANS -> "저장된 계획 보기"
                    ChatMessageAction.CUSTOM_PLAN -> "새 계획 만들기"
                    null -> null
                }
                ChatMessageBubble(
                    message = message,
                    actionLabel = actionLabel,
                    onAction = {
                        when (message.action) {
                            ChatMessageAction.SHOW_RECOMMENDATION -> onOpenRecommendation()
                            ChatMessageAction.START_RECOMMENDATION -> onStart()
                            ChatMessageAction.SHOW_DASHBOARD -> onOpenDashboard()
                            ChatMessageAction.SHOW_HISTORY -> onOpenHistory()
                            ChatMessageAction.SHOW_COMPLETED_WORKOUT -> onOpenHistory()
                            ChatMessageAction.UPDATE_CONDITION -> {
                                showConditionEditor = true
                            }
                            ChatMessageAction.SHOW_ACCOUNT_SETTINGS -> onOpenSettings()
                            ChatMessageAction.MANUAL_LOG ->
                                onManualLog(message.parameters.workoutTypeOrNull())
                            ChatMessageAction.SAVED_CUSTOM_PLANS ->
                                onOpenSavedCustomPlans()
                            ChatMessageAction.CUSTOM_PLAN ->
                                onCreateCustomPlan(message.parameters.customPlanPrefill())
                            null -> Unit
                        }
                    },
                )
            }
        }

        Surface(
            tonalElevation = 2.dp,
            shadowElevation = 8.dp,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(
                        horizontal = if (compactScreen) 10.dp else 16.dp,
                        vertical = 10.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.chat_help_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { submit("메뉴") }) {
                        Text(stringResource(R.string.chat_more))
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    quickActions.forEach { action ->
                        val highlighted = action.command == suggestedCommand
                        FilterChip(
                            selected = highlighted,
                            onClick = { submit(action.command) },
                            leadingIcon = {
                                Icon(
                                    action.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                )
                            },
                            label = { Text(action.label) },
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        placeholder = { Text(stringResource(R.string.chat_message_placeholder)) },
                        modifier = Modifier.weight(1f),
                        minLines = 1,
                        maxLines = 6,
                        shape = MaterialTheme.shapes.large,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { submit() }),
                    )
                    Surface(
                        onClick = { submit() },
                        enabled = input.isNotBlank(),
                        shape = CircleShape,
                        color = if (input.isNotBlank()) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        contentColor = if (input.isNotBlank()) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(56.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.AutoMirrored.Outlined.Send,
                                contentDescription = stringResource(R.string.chat_send),
                            )
                        }
                    }
                }
            }
        }
    }
}

internal fun shouldUseAiFirst(
    isGoogleSignedIn: Boolean,
    hasGeminiIntentRouter: Boolean,
): Boolean = isGoogleSignedIn && hasGeminiIntentRouter

@Composable
private fun ChatMessageBubble(
    message: ChatMessage,
    actionLabel: String? = null,
    actionEnabled: Boolean = true,
    onAction: () -> Unit = {},
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top,
    ) {
        if (!message.isUser) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(36.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.Spa,
                        contentDescription = stringResource(R.string.coach_avatar_description),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            Spacer(Modifier.size(8.dp))
        }
        Card(
            modifier = Modifier
                .fillMaxWidth(if (LocalConfiguration.current.screenWidthDp <= 360) 0.9f else 0.86f)
                .widthIn(max = 340.dp),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = if (message.isUser) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                },
                contentColor = if (message.isUser) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer
                },
            ),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(message.text, style = MaterialTheme.typography.bodyLarge)
                if (actionLabel != null) {
                    AssistChip(
                        onClick = onAction,
                        enabled = actionEnabled,
                        label = { Text(actionLabel) },
                    )
                }
                Text(
                    message.time,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
