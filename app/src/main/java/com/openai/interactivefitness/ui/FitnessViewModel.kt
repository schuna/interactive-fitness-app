package com.openai.interactivefitness.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.openai.interactivefitness.data.WorkoutRepository
import com.openai.interactivefitness.domain.DailyCondition
import com.openai.interactivefitness.domain.Recommendation
import com.openai.interactivefitness.domain.RecommendationEngine
import com.openai.interactivefitness.domain.WeeklySummary
import com.openai.interactivefitness.domain.WorkoutSession
import com.openai.interactivefitness.domain.WorkoutDraft
import com.openai.interactivefitness.domain.WorkoutType
import java.time.LocalDateTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FitnessUiState(
    val workouts: List<WorkoutSession> = emptyList(),
    val condition: DailyCondition = DailyCondition(),
    val recommendation: Recommendation? = null,
    val weeklySummary: WeeklySummary = WeeklySummary(0, 0, 0, 0, 0f),
)

class FitnessViewModel(
    private val repository: WorkoutRepository,
    private val recommendationEngine: RecommendationEngine = RecommendationEngine(),
) : ViewModel() {
    private val condition = MutableStateFlow(DailyCondition())

    val uiState: StateFlow<FitnessUiState> =
        combine(repository.workouts, condition) { workouts, currentCondition ->
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
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FitnessUiState())

    fun updateCondition(fatigue: Int? = null, soreness: Int? = null, hasPain: Boolean? = null) {
        condition.value = condition.value.copy(
            fatigue = fatigue ?: condition.value.fatigue,
            soreness = soreness ?: condition.value.soreness,
            hasPain = hasPain ?: condition.value.hasPain,
        )
    }

    fun completeRecommendation() {
        val recommendation = uiState.value.recommendation ?: return
        viewModelScope.launch {
            repository.save(
                WorkoutSession(
                    type = recommendation.type,
                    title = recommendation.title,
                    startedAt = LocalDateTime.now().minusMinutes(recommendation.durationMinutes.toLong()),
                    durationMinutes = recommendation.durationMinutes,
                    rpe = if (recommendation.type == WorkoutType.RECOVERY) 3 else 6,
                    detail = recommendation.exercises.joinToString(" · "),
                ),
            )
        }
    }

    fun addQuickWorkout(type: WorkoutType) {
        viewModelScope.launch {
            repository.save(
                WorkoutSession(
                    type = type,
                    title = "${type.label} 빠른 기록",
                    startedAt = LocalDateTime.now().minusMinutes(30),
                    durationMinutes = 30,
                    rpe = 6,
                    detail = "사용자가 빠른 기록으로 추가",
                ),
            )
        }
    }

    fun deleteWorkout(id: String) {
        viewModelScope.launch {
            repository.delete(id)
        }
    }

    fun saveWorkout(draft: WorkoutDraft, existing: WorkoutSession? = null) {
        if (draft.validate().isNotEmpty()) return
        viewModelScope.launch {
            repository.save(draft.toSession(existing))
        }
    }

    companion object {
        fun factory(repository: WorkoutRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(FitnessViewModel::class.java))
                    return FitnessViewModel(repository) as T
                }
            }
    }
}
