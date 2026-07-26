package com.openai.interactivefitness.data

import android.content.Context
import com.openai.interactivefitness.domain.ActiveWorkout
import com.openai.interactivefitness.domain.Recommendation
import com.openai.interactivefitness.domain.WorkoutType
import java.time.LocalDate
import java.time.LocalDateTime
import org.json.JSONArray

class ActiveWorkoutStore(context: Context) {
    private val preferences = context.getSharedPreferences(
        "active_workout_checkpoint",
        Context.MODE_PRIVATE,
    )

    fun save(workout: ActiveWorkout?) {
        if (workout == null) {
            preferences.edit().clear().apply()
            return
        }
        preferences.edit()
            .putString("id", workout.recommendation.id)
            .putString("date", workout.recommendation.date.toString())
            .putString("type", workout.recommendation.type.name)
            .putString("title", workout.recommendation.title)
            .putString("reason", workout.recommendation.reason)
            .putInt("duration", workout.recommendation.durationMinutes)
            .putString("difficulty", workout.recommendation.difficulty)
            .putString("steps", JSONArray(workout.steps).toString())
            .putString("startedAt", workout.startedAt.toString())
            .putInt("index", workout.currentStepIndex)
            .putInt("completed", workout.completedSteps)
            .putInt("restSeconds", workout.restSecondsRemaining)
            .putBoolean("isResting", workout.isResting)
            .apply {
                workout.restEndsAtEpochMillis?.let { putLong("restEndsAt", it) }
                    ?: remove("restEndsAt")
            }
            .apply()
    }

    fun load(): ActiveWorkout? {
        val id = preferences.getString("id", null) ?: return null
        return runCatching {
            val json = JSONArray(checkNotNull(preferences.getString("steps", null)))
            val steps = List(json.length()) { index -> json.getString(index) }
            ActiveWorkout(
                recommendation = Recommendation(
                    id = id,
                    date = LocalDate.parse(checkNotNull(preferences.getString("date", null))),
                    type = WorkoutType.valueOf(
                        checkNotNull(preferences.getString("type", null)),
                    ),
                    title = checkNotNull(preferences.getString("title", null)),
                    reason = checkNotNull(preferences.getString("reason", null)),
                    durationMinutes = preferences.getInt("duration", 0),
                    difficulty = checkNotNull(preferences.getString("difficulty", null)),
                    exercises = steps,
                ),
                startedAt = LocalDateTime.parse(
                    checkNotNull(preferences.getString("startedAt", null)),
                ),
                steps = steps,
                currentStepIndex = preferences.getInt("index", 0),
                completedSteps = preferences.getInt("completed", 0),
                restSecondsRemaining = preferences.getInt("restSeconds", 0),
                isResting = preferences.getBoolean("isResting", false),
                restEndsAtEpochMillis = if (preferences.contains("restEndsAt")) {
                    preferences.getLong("restEndsAt", 0L)
                } else {
                    null
                },
            )
        }.getOrElse {
            preferences.edit().clear().apply()
            null
        }
    }
}
