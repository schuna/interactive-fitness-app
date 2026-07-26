package com.openai.interactivefitness.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.openai.interactivefitness.domain.AppError
import com.openai.interactivefitness.domain.ErrorCategory
import com.openai.interactivefitness.data.WorkoutRepository
import com.openai.interactivefitness.data.ActiveWorkoutStore
import com.openai.interactivefitness.data.ErrorLogStore
import com.openai.interactivefitness.data.FirebaseSyncService
import com.openai.interactivefitness.data.FirebaseSyncStatus
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestoreException
import java.io.IOException
import com.openai.interactivefitness.domain.DailyCondition
import com.openai.interactivefitness.domain.ActiveWorkout
import com.openai.interactivefitness.domain.Recommendation
import com.openai.interactivefitness.domain.RecommendationEngine
import com.openai.interactivefitness.domain.WeeklySummary
import com.openai.interactivefitness.domain.WorkoutSession
import com.openai.interactivefitness.domain.WorkoutDraft
import com.openai.interactivefitness.domain.WorkoutType
import java.time.LocalDateTime
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

data class FitnessUiState(
    val workouts: List<WorkoutSession> = emptyList(),
    val condition: DailyCondition = DailyCondition(),
    val recommendation: Recommendation? = null,
    val weeklySummary: WeeklySummary = WeeklySummary(0, 0, 0, 0, 0f),
    val activeWorkout: ActiveWorkout? = null,
    val error: AppError? = null,
    val isTodayRecommendationCompleted: Boolean = false,
    val errorHistory: List<AppError> = emptyList(),
    val firebaseSyncStatus: FirebaseSyncStatus = FirebaseSyncStatus.UNCONFIGURED,
)

class FitnessViewModel(
    private val repository: WorkoutRepository,
    private val activeWorkoutStore: ActiveWorkoutStore,
    private val errorLogStore: ErrorLogStore,
    private val firebaseSyncService: FirebaseSyncService?,
    private val savedStateHandle: SavedStateHandle,
    private val recommendationEngine: RecommendationEngine = RecommendationEngine(),
) : ViewModel() {
    private val condition = MutableStateFlow(DailyCondition())
    private val lastError = MutableStateFlow<AppError?>(null)
    private val errorHistory = MutableStateFlow(errorLogStore.load())
    private val firebaseStatus = firebaseSyncService?.status
        ?: MutableStateFlow(FirebaseSyncStatus.UNCONFIGURED)
    private val activeWorkout = MutableStateFlow(
        restoreActiveWorkout() ?: activeWorkoutStore.load(),
    )
    private var timerJob: Job? = null

    val uiState: StateFlow<FitnessUiState> =
        combine(repository.workouts, condition, activeWorkout) {
                workouts,
                currentCondition,
                currentWorkout,
            ->
            val recent = workouts.filter {
                it.startedAt.isAfter(LocalDateTime.now().minusDays(7))
            }
            FitnessUiState(
                workouts = workouts,
                condition = currentCondition,
                recommendation = recommendationEngine.recommend(workouts, currentCondition),
                weeklySummary = WeeklySummary(
                    sessions = recent.size,
                    totalMinutes = recent.sumOf(WorkoutSession::durationMinutes),
                    strengthSessions = recent.count { it.type == WorkoutType.STRENGTH },
                    cardioSessions = recent.count {
                        it.type == WorkoutType.RUNNING || it.type == WorkoutType.CYCLING
                    },
                    goalProgress = (recent.size / 4f).coerceAtMost(1f),
                ),
                activeWorkout = currentWorkout,
                isTodayRecommendationCompleted = workouts.any {
                    it.recommendationDate == LocalDate.now()
                },
            )
        }.combine(lastError) { state, error ->
            state.copy(error = error)
        }.combine(errorHistory) { state, history ->
            state.copy(errorHistory = history)
        }.combine(firebaseStatus) { state, status ->
            state.copy(firebaseSyncStatus = status)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FitnessUiState())

    init {
        activeWorkout.value = activeWorkout.value?.restoredAt()
        persistActiveWorkout(activeWorkout.value)
        if (activeWorkout.value?.isResting == true) startRestTimer()
    }

    fun updateCondition(fatigue: Int? = null, soreness: Int? = null, hasPain: Boolean? = null) {
        condition.value = condition.value.copy(
            fatigue = fatigue ?: condition.value.fatigue,
            soreness = soreness ?: condition.value.soreness,
            hasPain = hasPain ?: condition.value.hasPain,
        )
    }

    fun startRecommendation() {
        if (uiState.value.isTodayRecommendationCompleted) {
            recordError(AppError(
                code = "RECOMMENDATION_ALREADY_COMPLETED",
                category = ErrorCategory.RECOMMENDATION,
                userMessage = "오늘의 추천 운동을 이미 완료했습니다.",
                operation = "startRecommendation",
                isRecoverable = false,
            ))
            return
        }
        val recommendation = uiState.value.recommendation ?: return
        timerJob?.cancel()
        setActiveWorkout(ActiveWorkout(
            recommendation = recommendation,
            startedAt = LocalDateTime.now(),
            steps = recommendation.exercises,
        ))
    }

    fun completeCurrentStep() {
        val current = activeWorkout.value ?: return
        if (current.isResting || current.completedSteps >= current.steps.size) return
        val updated = current.completeCurrentStep()
        setActiveWorkout(updated)
        if (updated.isResting) startRestTimer()
    }

    fun skipRest() {
        timerJob?.cancel()
        setActiveWorkout(activeWorkout.value?.moveToNextStep())
    }

    fun adjustRest(seconds: Int) {
        val updated = activeWorkout.value?.let {
            if (!it.isResting) it
            else {
                val remaining = (it.restSecondsRemaining + seconds).coerceAtLeast(0)
                it.copy(
                    restSecondsRemaining = remaining,
                    restEndsAtEpochMillis = System.currentTimeMillis() + remaining * 1_000L,
                )
            }
        }
        setActiveWorkout(updated)
    }

    private fun startRestTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1_000)
                val current = activeWorkout.value ?: break
                if (!current.isResting) break
                val restored = current.restoredAt()
                if (!restored.isResting) {
                    setActiveWorkout(restored)
                    break
                }
                setActiveWorkout(restored)
            }
        }
    }

    fun finishActiveWorkout() {
        val active = activeWorkout.value ?: return
        timerJob?.cancel()
        val elapsedMinutes = java.time.Duration.between(
            active.startedAt,
            LocalDateTime.now(),
        ).toMinutes().toInt().coerceAtLeast(1)
        viewModelScope.launch {
            runCatching {
                val workout = WorkoutSession(
                        type = active.recommendation.type,
                        title = active.recommendation.title,
                        startedAt = active.startedAt,
                        durationMinutes = elapsedMinutes,
                        rpe = if (active.recommendation.type == WorkoutType.RECOVERY) 3 else 6,
                        detail = "${active.completedSteps}/${active.steps.size}단계 완료 · " +
                            active.steps.joinToString(" · "),
                        sourceRecommendationId = active.recommendation.id,
                        recommendationDate = active.recommendation.date,
                    )
                repository.save(workout)
                syncWorkoutSafely(workout)
            }.onSuccess {
                setActiveWorkout(null)
            }.onFailure {
                reportDatabaseError("WORKOUT_SAVE_FAILED", "운동 기록을 저장하지 못했습니다.", "save")
            }
        }
    }

    fun cancelActiveWorkout() {
        timerJob?.cancel()
        setActiveWorkout(null)
    }

    fun addQuickWorkout(type: WorkoutType) {
        viewModelScope.launch {
            val workout = WorkoutSession(
                    type = type,
                    title = "${type.label} 빠른 기록",
                    startedAt = LocalDateTime.now().minusMinutes(30),
                    durationMinutes = 30,
                    rpe = 6,
                    detail = "사용자가 빠른 기록으로 추가",
                )
            repository.save(workout)
            syncWorkoutSafely(workout)
        }
    }

    fun deleteWorkout(id: String) {
        viewModelScope.launch {
            runCatching {
                repository.delete(id)
                firebaseSyncService?.deleteWorkout(id)
            }
                .onFailure { error ->
                    recordError(error.toFirebaseAppError("deleteWorkout"))
                }
        }
    }

    fun saveWorkout(draft: WorkoutDraft, existing: WorkoutSession? = null) {
        if (draft.validate().isNotEmpty()) return
        viewModelScope.launch {
            runCatching {
                val workout = draft.toSession(existing)
                repository.save(workout)
                syncWorkoutSafely(workout)
            }
                .onFailure {
                    reportDatabaseError("WORKOUT_SAVE_FAILED", "운동 기록을 저장하지 못했습니다.", "save")
                }
        }
    }

    fun dismissError() {
        lastError.value = null
    }

    fun clearErrorHistory() {
        errorLogStore.clear()
        errorHistory.value = emptyList()
    }

    fun syncNow() {
        val service = firebaseSyncService ?: return
        viewModelScope.launch {
            runCatching { service.syncAll(uiState.value.workouts) }
                .onFailure { error ->
                    recordError(error.toFirebaseAppError("syncAll"))
                }
        }
    }

    private suspend fun syncWorkoutSafely(workout: WorkoutSession) {
        val service = firebaseSyncService ?: return
        runCatching { service.syncWorkout(workout) }
            .onFailure { error ->
                recordError(error.toFirebaseAppError("syncWorkout"))
            }
    }

    private fun Throwable.toFirebaseAppError(operation: String): AppError {
        val causes = generateSequence(this) { it.cause }.toList()
        val root = causes.last()
        val authError = causes.filterIsInstance<FirebaseAuthException>().firstOrNull()
        val firestoreError = causes.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
        return when {
            authError?.errorCode in setOf(
                "ERROR_OPERATION_NOT_ALLOWED",
                "ERROR_ADMIN_RESTRICTED_OPERATION",
            ) ->
                AppError(
                    code = "FIREBASE_ANONYMOUS_AUTH_DISABLED",
                    category = ErrorCategory.FIREBASE,
                    userMessage = "Firebase Console에서 익명 인증과 사용자 가입을 허용하세요.",
                    operation = operation,
                    isRecoverable = false,
                )
            authError != null ->
                AppError(
                    code = "FIREBASE_AUTH_${authError.errorCode.removePrefix("ERROR_")}",
                    category = ErrorCategory.FIREBASE,
                    userMessage = "Firebase 익명 인증에 실패했습니다. 인증 설정을 확인하세요.",
                    operation = operation,
                )
            firestoreError?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                AppError(
                    code = "FIRESTORE_PERMISSION_DENIED",
                    category = ErrorCategory.FIREBASE,
                    userMessage = "Firestore 보안 규칙이 쓰기를 거부했습니다.",
                    operation = operation,
                    isRecoverable = false,
                )
            firestoreError?.code == FirebaseFirestoreException.Code.UNAVAILABLE ->
                AppError(
                    code = "FIRESTORE_UNAVAILABLE",
                    category = ErrorCategory.FIREBASE,
                    userMessage = "Firestore에 연결할 수 없습니다. 네트워크를 확인하세요.",
                    operation = operation,
                )
            root is IOException ->
                AppError(
                    code = "FIREBASE_NETWORK_ERROR",
                    category = ErrorCategory.FIREBASE,
                    userMessage = "Firebase 네트워크 연결에 실패했습니다.",
                    operation = operation,
                )
            else ->
                AppError(
                    code = "FIREBASE_SYNC_FAILED",
                    category = ErrorCategory.FIREBASE,
                    userMessage = "Firebase 동기화에 실패했습니다.",
                    operation = operation,
                )
        }
    }

    private fun reportDatabaseError(code: String, message: String, operation: String) {
        recordError(AppError(
            code = code,
            category = ErrorCategory.DATABASE,
            userMessage = message,
            operation = operation,
        ))
    }

    private fun recordError(error: AppError) {
        lastError.value = error
        errorLogStore.append(error)
        errorHistory.value = errorLogStore.load()
    }

    private fun setActiveWorkout(workout: ActiveWorkout?) {
        activeWorkout.value = workout
        persistActiveWorkout(workout)
    }

    private fun persistActiveWorkout(workout: ActiveWorkout?) {
        if (workout == null) {
            ACTIVE_KEYS.forEach { key -> savedStateHandle.remove<Any?>(key) }
            activeWorkoutStore.save(null)
            return
        }
        savedStateHandle["active.id"] = workout.recommendation.id
        savedStateHandle["active.date"] = workout.recommendation.date.toString()
        savedStateHandle["active.type"] = workout.recommendation.type.name
        savedStateHandle["active.title"] = workout.recommendation.title
        savedStateHandle["active.reason"] = workout.recommendation.reason
        savedStateHandle["active.duration"] = workout.recommendation.durationMinutes
        savedStateHandle["active.difficulty"] = workout.recommendation.difficulty
        savedStateHandle["active.steps"] = ArrayList(workout.steps)
        savedStateHandle["active.startedAt"] = workout.startedAt.toString()
        savedStateHandle["active.index"] = workout.currentStepIndex
        savedStateHandle["active.completed"] = workout.completedSteps
        savedStateHandle["active.restSeconds"] = workout.restSecondsRemaining
        savedStateHandle["active.isResting"] = workout.isResting
        savedStateHandle["active.restEndsAt"] = workout.restEndsAtEpochMillis
        activeWorkoutStore.save(workout)
    }

    private fun restoreActiveWorkout(): ActiveWorkout? {
        val id = savedStateHandle.get<String>("active.id") ?: return null
        return runCatching {
            val steps = savedStateHandle.get<ArrayList<String>>("active.steps").orEmpty()
            ActiveWorkout(
                recommendation = com.openai.interactivefitness.domain.Recommendation(
                    id = id,
                    date = LocalDate.parse(checkNotNull(savedStateHandle.get<String>("active.date"))),
                    type = WorkoutType.valueOf(
                        checkNotNull(savedStateHandle.get<String>("active.type")),
                    ),
                    title = checkNotNull(savedStateHandle.get<String>("active.title")),
                    reason = checkNotNull(savedStateHandle.get<String>("active.reason")),
                    durationMinutes = checkNotNull(savedStateHandle.get<Int>("active.duration")),
                    difficulty = checkNotNull(savedStateHandle.get<String>("active.difficulty")),
                    exercises = steps,
                ),
                startedAt = LocalDateTime.parse(
                    checkNotNull(savedStateHandle.get<String>("active.startedAt")),
                ),
                steps = steps,
                currentStepIndex = savedStateHandle["active.index"] ?: 0,
                completedSteps = savedStateHandle["active.completed"] ?: 0,
                restSecondsRemaining = savedStateHandle["active.restSeconds"] ?: 0,
                isResting = savedStateHandle["active.isResting"] ?: false,
                restEndsAtEpochMillis = savedStateHandle["active.restEndsAt"],
            )
        }.getOrElse {
            ACTIVE_KEYS.forEach { key -> savedStateHandle.remove<Any?>(key) }
            null
        }
    }

    companion object {
        fun factory(
            repository: WorkoutRepository,
            activeWorkoutStore: ActiveWorkoutStore,
            errorLogStore: ErrorLogStore,
            firebaseSyncService: FirebaseSyncService?,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    modelClass: Class<T>,
                    extras: CreationExtras,
                ): T {
                    require(modelClass.isAssignableFrom(FitnessViewModel::class.java))
                    return FitnessViewModel(
                        repository,
                        activeWorkoutStore,
                        errorLogStore,
                        firebaseSyncService,
                        extras.createSavedStateHandle(),
                    ) as T
                }
            }

        private val ACTIVE_KEYS = listOf(
            "active.id", "active.date", "active.type", "active.title", "active.reason",
            "active.duration", "active.difficulty", "active.steps", "active.startedAt",
            "active.index", "active.completed", "active.restSeconds", "active.isResting",
            "active.restEndsAt",
        )
    }
}
