package com.openai.interactivefitness.data

import android.content.Context
import com.openai.interactivefitness.domain.CustomWorkoutPlan
import com.openai.interactivefitness.domain.WorkoutType
import com.openai.interactivefitness.domain.PlannedExercise
import com.openai.interactivefitness.domain.PlannedSet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject

class CustomWorkoutPlanStore(context: Context) {
    private val preferences =
        context.getSharedPreferences("custom_workout_plans", Context.MODE_PRIVATE)
    private val mutablePlans = MutableStateFlow(load())
    val plans: StateFlow<List<CustomWorkoutPlan>> = mutablePlans

    fun save(plan: CustomWorkoutPlan) {
        val updated = (mutablePlans.value.filterNot { it.id == plan.id } + plan)
            .sortedBy(CustomWorkoutPlan::title)
        persist(updated)
    }

    fun delete(id: String) = persist(mutablePlans.value.filterNot { it.id == id })

    private fun persist(plans: List<CustomWorkoutPlan>) {
        val json = JSONArray()
        plans.forEach { plan ->
            json.put(JSONObject().apply {
                put("id", plan.id)
                put("title", plan.title)
                put("type", plan.type.name)
                put("durationMinutes", plan.durationMinutes)
                put("exercises", JSONArray(plan.exercises))
                put("plannedExercises", JSONArray().apply {
                    plan.plannedExercises.forEach { exercise ->
                        put(JSONObject().apply {
                            put("id", exercise.id)
                            put("exerciseId", exercise.exerciseId)
                            put("exerciseName", exercise.exerciseName)
                            put("restSeconds", exercise.restSeconds)
                            put("sets", JSONArray().apply {
                                exercise.sets.forEach { set ->
                                    put(JSONObject().apply {
                                        put("id", set.id)
                                        put("weightKg", set.weightKg)
                                        put("reps", set.reps)
                                    })
                                }
                            })
                        })
                    }
                })
            })
        }
        preferences.edit().putString(KEY, json.toString()).apply()
        mutablePlans.value = plans
    }

    private fun load(): List<CustomWorkoutPlan> = runCatching {
        val json = JSONArray(preferences.getString(KEY, "[]"))
        (0 until json.length()).map { index ->
            val item = json.getJSONObject(index)
            val exercises = item.getJSONArray("exercises")
            val plannedExercises = item.optJSONArray("plannedExercises")
            CustomWorkoutPlan(
                id = item.getString("id"),
                title = item.getString("title"),
                type = WorkoutType.valueOf(item.getString("type")),
                durationMinutes = item.getInt("durationMinutes"),
                exercises = (0 until exercises.length()).map(exercises::getString),
                plannedExercises = if (plannedExercises == null) {
                    emptyList()
                } else {
                    (0 until plannedExercises.length()).map { exerciseIndex ->
                        val exercise = plannedExercises.getJSONObject(exerciseIndex)
                        val sets = exercise.getJSONArray("sets")
                        PlannedExercise(
                            id = exercise.getString("id"),
                            exerciseId = exercise.getString("exerciseId"),
                            exerciseName = exercise.getString("exerciseName"),
                            restSeconds = exercise.optInt("restSeconds", 60),
                            sets = (0 until sets.length()).map { setIndex ->
                                val set = sets.getJSONObject(setIndex)
                                PlannedSet(
                                    id = set.getString("id"),
                                    weightKg = set.getDouble("weightKg"),
                                    reps = set.getInt("reps"),
                                )
                            },
                        )
                    }
                },
            )
        }
    }.getOrDefault(emptyList())

    private companion object {
        const val KEY = "plans"
    }
}
