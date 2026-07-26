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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.openai.interactivefitness.R
import com.openai.interactivefitness.domain.ConversationEngine
import com.openai.interactivefitness.domain.ConversationIntent
import com.openai.interactivefitness.domain.DailyCondition
import com.openai.interactivefitness.domain.Recommendation
import com.openai.interactivefitness.domain.WorkoutType
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val time: String = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")),
)

private data class ChatQuickAction(
    val label: String,
    val command: String,
    val icon: ImageVector,
    val keywords: List<String>,
)

@Composable
fun ChatScreen(
    condition: DailyCondition,
    recommendation: Recommendation?,
    onFatigueChanged: (Int) -> Unit,
    onSorenessChanged: (Int) -> Unit,
    onPainChanged: (Boolean) -> Unit,
    onQuickWorkout: (WorkoutType) -> Unit,
    onStart: () -> Unit,
    isCompleted: Boolean,
    onOpenDashboard: () -> Unit,
    onOpenHistory: () -> Unit,
) {
    val conversationEngine = remember { ConversationEngine() }
    var input by remember { mutableStateOf("") }
    val greeting = stringResource(R.string.chat_greeting)
    val messages = remember(greeting) {
        mutableStateListOf(ChatMessage(text = greeting, isUser = false))
    }
    val listState = rememberLazyListState()
    val conditionAppliedMessage = stringResource(R.string.condition_applied)
    val quickActions = listOf(
        ChatQuickAction(
            stringResource(R.string.chat_action_today),
            "오늘 운동 추천",
            Icons.Outlined.Spa,
            listOf("오늘", "추천", "운동"),
        ),
        ChatQuickAction(
            stringResource(R.string.chat_action_weight),
            "웨이트 기록",
            Icons.Outlined.FitnessCenter,
            listOf("웨이트", "근력", "기록"),
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

    fun submit(command: String = input) {
        if (command.isBlank()) return
        val result = conversationEngine.interpret(command)
        messages += ChatMessage(command.trim(), isUser = true)
        messages += ChatMessage(result.reply, isUser = false)
        when (val intent = result.intent) {
            ConversationIntent.RecommendToday -> Unit
            ConversationIntent.StartRecommendation -> onStart()
            ConversationIntent.ShowDashboard -> onOpenDashboard()
            ConversationIntent.ShowHistory -> onOpenHistory()
            is ConversationIntent.QuickLog -> onQuickWorkout(intent.type)
            ConversationIntent.ShowMenu,
            is ConversationIntent.Unknown,
            -> Unit
        }
        input = ""
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(messages) { ChatMessageBubble(it) }
            item {
                ChatConditionCard(
                    condition = condition,
                    onFatigueChanged = onFatigueChanged,
                    onSorenessChanged = onSorenessChanged,
                    onPainChanged = onPainChanged,
                    onApply = {
                        messages += ChatMessage(conditionAppliedMessage, isUser = false)
                    },
                )
            }
            item {
                ChatMessageBubble(
                    ChatMessage(
                        recommendation?.reason
                            ?: stringResource(R.string.chat_recommendation_ready),
                        isUser = false,
                    ),
                    actionLabel = if (isCompleted) {
                        stringResource(R.string.chat_recommendation_completed)
                    } else {
                        stringResource(R.string.chat_recommendation_start)
                    },
                    actionEnabled = !isCompleted,
                    onAction = onStart,
                )
            }
        }

        Surface(
            tonalElevation = 2.dp,
            shadowElevation = 8.dp,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth().imePadding(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
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
                        val highlighted = input.isNotBlank() &&
                            action.keywords.any { input.contains(it, ignoreCase = true) }
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
            modifier = Modifier.widthIn(max = 340.dp),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = if (message.isUser) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
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
