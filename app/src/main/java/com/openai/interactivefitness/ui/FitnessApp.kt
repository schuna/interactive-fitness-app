package com.openai.interactivefitness.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.openai.interactivefitness.FitnessApplication
import com.openai.interactivefitness.domain.Recommendation
import com.openai.interactivefitness.domain.ActiveWorkout
import com.openai.interactivefitness.domain.StrengthSet
import com.openai.interactivefitness.domain.WeeklySummary
import com.openai.interactivefitness.domain.WorkoutDraft
import com.openai.interactivefitness.domain.WorkoutInterval
import com.openai.interactivefitness.domain.WorkoutSession
import com.openai.interactivefitness.domain.WorkoutType
import java.time.format.DateTimeFormatter

private enum class Destination(
    val label: String,
    val icon: ImageVector,
) {
    TODAY("오늘", Icons.Outlined.Home),
    CHAT("대화", Icons.Outlined.ChatBubbleOutline),
    DASHBOARD("대시보드", Icons.Outlined.AutoGraph),
    HISTORY("기록", Icons.AutoMirrored.Outlined.EventNote),
}

@Composable
fun FitnessApp() {
    val application = LocalContext.current.applicationContext as FitnessApplication
    val viewModel: FitnessViewModel = viewModel(
        factory = FitnessViewModel.factory(
            application.workoutRepository,
            application.activeWorkoutStore,
        ),
    )
    val state by viewModel.uiState.collectAsState()
    val selected = androidx.compose.runtime.saveable.rememberSaveable {
        androidx.compose.runtime.mutableStateOf(Destination.TODAY)
    }

    state.error?.let { error ->
        AlertDialog(
            onDismissRequest = viewModel::dismissError,
            title = { Text("작업을 완료하지 못했습니다") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(error.userMessage)
                    Text(
                        "오류 코드: ${error.code}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::dismissError) { Text("확인") }
            },
        )
    }

    state.activeWorkout?.let { activeWorkout ->
        WorkoutProgressScreen(
            activeWorkout = activeWorkout,
            onCompleteStep = viewModel::completeCurrentStep,
            onSkipRest = viewModel::skipRest,
            onAdjustRest = viewModel::adjustRest,
            onFinish = viewModel::finishActiveWorkout,
            onCancel = viewModel::cancelActiveWorkout,
        )
        return
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                Destination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = selected.value == destination,
                        onClick = { selected.value = destination },
                        icon = { Icon(destination.icon, contentDescription = null) },
                        label = { Text(destination.label) },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (selected.value) {
                Destination.TODAY -> TodayScreen(
                    state = state,
                    onFatigueChanged = { viewModel.updateCondition(fatigue = it) },
                    onSorenessChanged = { viewModel.updateCondition(soreness = it) },
                    onPainChanged = { viewModel.updateCondition(hasPain = it) },
                    onStart = viewModel::startRecommendation,
                )
                Destination.CHAT -> ChatScreen(
                    recommendation = state.recommendation,
                    onQuickWorkout = viewModel::addQuickWorkout,
                    onStart = viewModel::startRecommendation,
                )
                Destination.DASHBOARD -> DashboardScreen(state.weeklySummary)
                Destination.HISTORY -> HistoryScreen(
                    workouts = state.workouts,
                    onSave = viewModel::saveWorkout,
                    onDelete = viewModel::deleteWorkout,
                )
            }
        }
    }
}

@Composable
private fun TodayScreen(
    state: FitnessUiState,
    onFatigueChanged: (Int) -> Unit,
    onSorenessChanged: (Int) -> Unit,
    onPainChanged: (Boolean) -> Unit,
    onStart: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Spacer(Modifier.height(12.dp))
            Text("오늘의 운동", style = MaterialTheme.typography.headlineMedium)
            Text("기록과 컨디션을 바탕으로 준비했어요.")
        }
        item {
            ConditionCard(
                fatigue = state.condition.fatigue,
                soreness = state.condition.soreness,
                hasPain = state.condition.hasPain,
                onFatigueChanged = onFatigueChanged,
                onSorenessChanged = onSorenessChanged,
                onPainChanged = onPainChanged,
            )
        }
        item {
            state.recommendation?.let {
                RecommendationCard(it, onStart)
            } ?: CircularProgressIndicator()
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun ConditionCard(
    fatigue: Int,
    soreness: Int,
    hasPain: Boolean,
    onFatigueChanged: (Int) -> Unit,
    onSorenessChanged: (Int) -> Unit,
    onPainChanged: (Boolean) -> Unit,
) {
    Card {
        Column(Modifier.padding(16.dp)) {
            Text("오늘의 컨디션", style = MaterialTheme.typography.titleMedium)
            Text("피로도 $fatigue / 5")
            Slider(
                value = fatigue.toFloat(),
                onValueChange = { onFatigueChanged(it.toInt().coerceIn(1, 5)) },
                valueRange = 1f..5f,
                steps = 3,
            )
            Text("근육통 $soreness / 5")
            Slider(
                value = soreness.toFloat(),
                onValueChange = { onSorenessChanged(it.toInt().coerceIn(1, 5)) },
                valueRange = 1f..5f,
                steps = 3,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = hasPain, onCheckedChange = onPainChanged)
                Text("운동에 영향을 줄 수 있는 통증이 있어요")
            }
        }
    }
}

@Composable
private fun RecommendationCard(recommendation: Recommendation, onStart: () -> Unit) {
    Card {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(recommendation.title, style = MaterialTheme.typography.headlineSmall)
            Text("${recommendation.durationMinutes}분 · ${recommendation.difficulty}")
            Text(recommendation.reason)
            HorizontalDivider()
            recommendation.exercises.forEach { Text("• $it") }
            recommendation.safetyNotice?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
                Text("운동 시작")
            }
        }
    }
}

@Composable
private fun ChatScreen(
    recommendation: Recommendation?,
    onQuickWorkout: (WorkoutType) -> Unit,
    onStart: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text("무엇을 도와드릴까요?", style = MaterialTheme.typography.headlineMedium) }
        item {
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(recommendation?.reason ?: "추천을 준비하고 있어요.")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(onClick = onStart, label = { Text("추천 운동 시작") })
                    }
                }
            }
        }
        item { Text("빠른 기록", style = MaterialTheme.typography.titleMedium) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(WorkoutType.STRENGTH, WorkoutType.RUNNING, WorkoutType.CYCLING).forEach {
                    AssistChip(onClick = { onQuickWorkout(it) }, label = { Text(it.label) })
                }
            }
        }
        item {
            Text(
                "현재는 검증 가능한 메뉴 명령만 제공합니다. 자유 텍스트 해석은 서버 계층 연결 후 추가합니다.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun DashboardScreen(summary: WeeklySummary) {
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("이번 주", style = MaterialTheme.typography.headlineMedium)
        Card {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${summary.sessions}회 · ${summary.totalMinutes}분")
                LinearProgressIndicator(
                    progress = { summary.goalProgress },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("주 4회 목표 ${(summary.goalProgress * 100).toInt()}%")
            }
        }
        Card {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("운동 균형", style = MaterialTheme.typography.titleMedium)
                Text("근력 ${summary.strengthSessions}회")
                Text("유산소 ${summary.cardioSessions}회")
            }
        }
    }
}

@Composable
private fun WorkoutProgressScreen(
    activeWorkout: ActiveWorkout,
    onCompleteStep: () -> Unit,
    onSkipRest: () -> Unit,
    onAdjustRest: (Int) -> Unit,
    onFinish: () -> Unit,
    onCancel: () -> Unit,
) {
    var confirmCancel by remember { mutableStateOf(false) }

    if (confirmCancel) {
        AlertDialog(
            onDismissRequest = { confirmCancel = false },
            title = { Text("운동을 취소할까요?") },
            text = { Text("현재 진행 상황은 기록되지 않습니다.") },
            confirmButton = {
                TextButton(onClick = onCancel) {
                    Text("운동 취소", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmCancel = false }) { Text("계속하기") }
            },
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("운동 진행", style = MaterialTheme.typography.labelLarge)
                Text(
                    activeWorkout.recommendation.title,
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
            TextButton(onClick = { confirmCancel = true }) { Text("취소") }
        }

        LinearProgressIndicator(
            progress = { activeWorkout.progress },
            modifier = Modifier.fillMaxWidth(),
        )
        Text("${activeWorkout.completedSteps}/${activeWorkout.steps.size}단계 완료")

        if (activeWorkout.completedSteps >= activeWorkout.steps.size) {
            Card {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("운동을 모두 마쳤습니다!", style = MaterialTheme.typography.titleLarge)
                    Text("완료 기록을 저장하고 대시보드를 갱신하세요.")
                    Button(onClick = onFinish, modifier = Modifier.fillMaxWidth()) {
                        Text("운동 종료 및 저장")
                    }
                }
            }
        } else if (activeWorkout.isResting) {
            Card {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text("휴식", style = MaterialTheme.typography.titleLarge)
                    Text(
                        formatTimer(activeWorkout.restSecondsRemaining),
                        style = MaterialTheme.typography.displayMedium,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AssistChip(
                            onClick = { onAdjustRest(-15) },
                            label = { Text("-15초") },
                        )
                        AssistChip(
                            onClick = { onAdjustRest(15) },
                            label = { Text("+15초") },
                        )
                    }
                    Button(onClick = onSkipRest, modifier = Modifier.fillMaxWidth()) {
                        Text("휴식 건너뛰기")
                    }
                }
            }
        } else {
            val currentStep = activeWorkout.steps.getOrNull(activeWorkout.currentStepIndex)
            Card {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(
                        "현재 단계 ${activeWorkout.currentStepIndex + 1}",
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                        currentStep.orEmpty(),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Button(onClick = onCompleteStep, modifier = Modifier.fillMaxWidth()) {
                        Text(if (activeWorkout.isLastStep) "마지막 단계 완료" else "운동 시작")
                    }
                }
            }
        }

        Text("전체 계획", style = MaterialTheme.typography.titleMedium)
        activeWorkout.steps.forEachIndexed { index, step ->
            val marker = when {
                index < activeWorkout.completedSteps -> "✓"
                index == activeWorkout.currentStepIndex -> "●"
                else -> "○"
            }
            Text("$marker $step")
        }
    }
}

private fun formatTimer(totalSeconds: Int): String =
    "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)

@Composable
private fun HistoryScreen(
    workouts: List<WorkoutSession>,
    onSave: (WorkoutDraft, WorkoutSession?) -> Unit,
    onDelete: (String) -> Unit,
) {
    var pendingDelete by remember { mutableStateOf<WorkoutSession?>(null) }
    var editingWorkout by remember { mutableStateOf<WorkoutSession?>(null) }
    var editorDraft by remember { mutableStateOf<WorkoutDraft?>(null) }

    editorDraft?.let { draft ->
        WorkoutEditorDialog(
            draft = draft,
            isEditing = editingWorkout != null,
            onDraftChange = { editorDraft = it },
            onDismiss = {
                editorDraft = null
                editingWorkout = null
            },
            onSave = {
                onSave(it, editingWorkout)
                editorDraft = null
                editingWorkout = null
            },
        )
    }

    pendingDelete?.let { workout ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("운동 기록을 삭제할까요?") },
            text = { Text("${workout.title} 기록은 삭제 후 복구할 수 없습니다.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(workout.id)
                        pendingDelete = null
                    },
                ) {
                    Text("삭제", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("취소")
                }
            },
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Spacer(Modifier.height(12.dp))
            Text("운동 기록", style = MaterialTheme.typography.headlineMedium)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(WorkoutType.STRENGTH, WorkoutType.RUNNING, WorkoutType.CYCLING).forEach {
                    AssistChip(
                        onClick = {
                            editingWorkout = null
                            editorDraft = WorkoutDraft(
                                type = it,
                                title = "${it.label} 운동",
                            )
                        },
                        label = { Text("+ ${it.label}") },
                    )
                }
            }
        }
        if (workouts.isEmpty()) {
            item {
                Card {
                    Text(
                        "아직 기록이 없습니다. 위 버튼으로 첫 운동을 추가해보세요.",
                        modifier = Modifier.padding(18.dp),
                    )
                }
            }
        }
        items(workouts, key = WorkoutSession::id) { workout ->
            Card {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(workout.title, style = MaterialTheme.typography.titleMedium)
                    Text("${workout.type.label} · ${workout.durationMinutes}분 · RPE ${workout.rpe}")
                    Text(workout.startedAt.format(DateTimeFormatter.ofPattern("M월 d일 HH:mm")))
                    Text(workout.detail, style = MaterialTheme.typography.bodySmall)
                    if (workout.strengthSets.isNotEmpty()) {
                        Text(
                            "${workout.strengthSets.size}세트 · " +
                                "${workout.strengthSets.sumOf { it.weightKg * it.reps }.toInt()}kg 볼륨",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    if (workout.intervals.isNotEmpty()) {
                        Text(
                            "${workout.intervals.size}인터벌 · " +
                                "${workout.intervals.sumOf { it.distanceMeters }}m",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Row(
                        modifier = Modifier.align(Alignment.End),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        TextButton(
                            onClick = {
                                editingWorkout = workout
                                editorDraft = WorkoutDraft(
                                    id = workout.id,
                                    type = workout.type,
                                    title = workout.title,
                                    durationMinutes = workout.durationMinutes.toString(),
                                    rpe = workout.rpe.toString(),
                                    detail = workout.detail,
                                    strengthSets = workout.strengthSets,
                                    intervals = workout.intervals,
                                )
                            },
                        ) {
                            Text("수정")
                        }
                        TextButton(onClick = { pendingDelete = workout }) {
                            Text("삭제")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkoutEditorDialog(
    draft: WorkoutDraft,
    isEditing: Boolean,
    onDraftChange: (WorkoutDraft) -> Unit,
    onDismiss: () -> Unit,
    onSave: (WorkoutDraft) -> Unit,
) {
    var showErrors by remember(draft.id) { mutableStateOf(false) }
    val errors = if (showErrors) draft.validate() else emptyMap()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "운동 기록 수정" else "새 운동 기록") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            WorkoutType.STRENGTH,
                            WorkoutType.RUNNING,
                            WorkoutType.CYCLING,
                            WorkoutType.RECOVERY,
                        ).forEach { type ->
                            FilterChip(
                                selected = draft.type == type,
                                onClick = { onDraftChange(draft.copy(type = type)) },
                                label = { Text(type.label) },
                            )
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = draft.title,
                        onValueChange = { onDraftChange(draft.copy(title = it)) },
                        label = { Text("운동 제목") },
                        isError = "title" in errors,
                        supportingText = { errors["title"]?.let { Text(it) } },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = draft.durationMinutes,
                            onValueChange = { onDraftChange(draft.copy(durationMinutes = it)) },
                            label = { Text("시간(분)") },
                            isError = "durationMinutes" in errors,
                            supportingText = { errors["durationMinutes"]?.let { Text(it) } },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = draft.rpe,
                            onValueChange = { onDraftChange(draft.copy(rpe = it)) },
                            label = { Text("RPE") },
                            isError = "rpe" in errors,
                            supportingText = { errors["rpe"]?.let { Text(it) } },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = draft.detail,
                        onValueChange = { onDraftChange(draft.copy(detail = it)) },
                        label = { Text("상세 내용") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (draft.type == WorkoutType.STRENGTH) {
                    item {
                        Text("웨이트 세트", style = MaterialTheme.typography.titleMedium)
                        errors["strengthSets"]?.let {
                            Text(it, color = MaterialTheme.colorScheme.error)
                        }
                    }
                    items(draft.strengthSets.size) { index ->
                        val set = draft.strengthSets[index]
                        StrengthSetEditor(
                            index = index,
                            set = set,
                            onChange = { changed ->
                                onDraftChange(
                                    draft.copy(
                                        strengthSets = draft.strengthSets.toMutableList().apply {
                                            this[index] = changed
                                        },
                                    ),
                                )
                            },
                            onDelete = {
                                onDraftChange(
                                    draft.copy(
                                        strengthSets = draft.strengthSets.filterIndexed { i, _ ->
                                            i != index
                                        },
                                    ),
                                )
                            },
                        )
                    }
                    item {
                        TextButton(
                            onClick = {
                                onDraftChange(
                                    draft.copy(
                                        strengthSets = draft.strengthSets + StrengthSet(
                                            exercise = draft.title.ifBlank { "운동" },
                                            weightKg = 20.0,
                                            reps = 10,
                                            rpe = 6,
                                        ),
                                    ),
                                )
                            },
                        ) {
                            Text("+ 세트 추가")
                        }
                    }
                } else if (
                    draft.type == WorkoutType.RUNNING ||
                    draft.type == WorkoutType.CYCLING
                ) {
                    item {
                        Text("인터벌", style = MaterialTheme.typography.titleMedium)
                        errors["intervals"]?.let {
                            Text(it, color = MaterialTheme.colorScheme.error)
                        }
                    }
                    items(draft.intervals.size) { index ->
                        val interval = draft.intervals[index]
                        IntervalEditor(
                            index = index,
                            interval = interval,
                            onChange = { changed ->
                                onDraftChange(
                                    draft.copy(
                                        intervals = draft.intervals.toMutableList().apply {
                                            this[index] = changed
                                        },
                                    ),
                                )
                            },
                            onDelete = {
                                onDraftChange(
                                    draft.copy(
                                        intervals = draft.intervals.filterIndexed { i, _ ->
                                            i != index
                                        },
                                    ),
                                )
                            },
                        )
                    }
                    item {
                        TextButton(
                            onClick = {
                                onDraftChange(
                                    draft.copy(
                                        intervals = draft.intervals + WorkoutInterval(
                                            durationSeconds = 300,
                                            distanceMeters = 1_000,
                                        ),
                                    ),
                                )
                            },
                        ) {
                            Text("+ 인터벌 추가")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    showErrors = true
                    if (draft.validate().isEmpty()) onSave(draft)
                },
            ) {
                Text("저장")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        },
    )
}

@Composable
private fun StrengthSetEditor(
    index: Int,
    set: StrengthSet,
    onChange: (StrengthSet) -> Unit,
    onDelete: () -> Unit,
) {
    Card {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("세트 ${index + 1}", modifier = Modifier.weight(1f))
                TextButton(onClick = onDelete) { Text("제거") }
            }
            OutlinedTextField(
                value = set.exercise,
                onValueChange = { onChange(set.copy(exercise = it)) },
                label = { Text("운동 종목") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                NumericField(
                    value = set.weightKg.toDisplayString(),
                    label = "중량(kg)",
                    decimal = true,
                    modifier = Modifier.weight(1f),
                    onValueChange = { value ->
                        value.toDoubleOrNull()?.let { onChange(set.copy(weightKg = it)) }
                    },
                )
                NumericField(
                    value = set.reps.toString(),
                    label = "반복",
                    modifier = Modifier.weight(1f),
                    onValueChange = { value ->
                        value.toIntOrNull()?.let { onChange(set.copy(reps = it.coerceAtLeast(1))) }
                    },
                )
                NumericField(
                    value = set.rpe.toString(),
                    label = "RPE",
                    modifier = Modifier.weight(1f),
                    onValueChange = { value ->
                        value.toIntOrNull()?.let { onChange(set.copy(rpe = it.coerceIn(1, 10))) }
                    },
                )
            }
        }
    }
}

@Composable
private fun IntervalEditor(
    index: Int,
    interval: WorkoutInterval,
    onChange: (WorkoutInterval) -> Unit,
    onDelete: () -> Unit,
) {
    Card {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("인터벌 ${index + 1}", modifier = Modifier.weight(1f))
                TextButton(onClick = onDelete) { Text("제거") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                NumericField(
                    value = interval.durationSeconds.toString(),
                    label = "시간(초)",
                    modifier = Modifier.weight(1f),
                    onValueChange = { value ->
                        value.toIntOrNull()?.let {
                            onChange(interval.copy(durationSeconds = it.coerceAtLeast(1)))
                        }
                    },
                )
                NumericField(
                    value = interval.distanceMeters.toString(),
                    label = "거리(m)",
                    modifier = Modifier.weight(1f),
                    onValueChange = { value ->
                        value.toIntOrNull()?.let {
                            onChange(interval.copy(distanceMeters = it.coerceAtLeast(0)))
                        }
                    },
                )
            }
            OutlinedTextField(
                value = interval.note,
                onValueChange = { onChange(interval.copy(note = it)) },
                label = { Text("메모") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun NumericField(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    decimal: Boolean = false,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(
            keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number,
        ),
        singleLine = true,
        modifier = modifier,
    )
}

private fun Double.toDisplayString(): String =
    if (this % 1.0 == 0.0) toInt().toString() else toString()
