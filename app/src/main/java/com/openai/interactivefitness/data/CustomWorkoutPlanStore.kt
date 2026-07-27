package com.openai.interactivefitness.data

import android.content.Context
import com.openai.interactivefitness.domain.CustomWorkoutPlan
import com.openai.interactivefitness.domain.ExerciseCatalog
import com.openai.interactivefitness.domain.PlannedExercise
import com.openai.interactivefitness.domain.PlannedSet
import com.openai.interactivefitness.domain.WorkoutType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray

class CustomWorkoutPlanStore(
    context: Context,
    private val dao: CustomWorkoutPlanDao,
) {
    private val preferences =
        context.getSharedPreferences(LEGACY_PREFERENCES, Context.MODE_PRIVATE)
    private val migrationMutex = Mutex()
    private var migrationChecked = false

    val plans: Flow<List<CustomWorkoutPlan>> = flow {
        migrateLegacyPlans()
        emitAll(
            dao.observeAll().map { rows ->
                rows.map { row ->
                    val plannedExercises = row.exercises
                        .sortedBy { it.exercise.position }
                        .map { exerciseRow ->
                            PlannedExercise(
                                id = exerciseRow.exercise.id,
                                exerciseId = exerciseRow.exercise.exerciseId,
                                exerciseName = exerciseRow.exercise.exerciseName,
                                restSeconds = exerciseRow.exercise.restSeconds,
                                sets = exerciseRow.sets
                                    .sortedBy(CustomPlanSetEntity::position)
                                    .map {
                                        PlannedSet(
                                            id = it.id,
                                            weightKg = it.weightKg,
                                            reps = it.reps,
                                        )
                                    },
                            )
                        }
                    CustomWorkoutPlan(
                        id = row.plan.id,
                        title = row.plan.title,
                        type = WorkoutType.valueOf(row.plan.type),
                        durationMinutes = row.plan.durationMinutes,
                        exercises = plannedExercises.map(PlannedExercise::exerciseName),
                        plannedExercises = plannedExercises,
                    )
                }
            },
        )
    }

    suspend fun save(plan: CustomWorkoutPlan) {
        saveToRoom(plan.withStructuredExercises())
    }

    suspend fun delete(id: String) {
        dao.deleteById(id)
    }

    private suspend fun saveToRoom(plan: CustomWorkoutPlan) {
        val exercises = plan.plannedExercises.mapIndexed { index, exercise ->
            CustomPlanExerciseEntity(
                id = exercise.id,
                planId = plan.id,
                exerciseId = exercise.exerciseId,
                exerciseName = exercise.exerciseName,
                position = index,
                restSeconds = exercise.restSeconds,
            )
        }
        dao.replace(
            plan = CustomWorkoutPlanEntity(
                id = plan.id,
                title = plan.title,
                type = plan.type.name,
                durationMinutes = plan.durationMinutes,
            ),
            exercises = exercises,
            sets = plan.plannedExercises.flatMap { exercise ->
                exercise.sets.mapIndexed { index, set ->
                    CustomPlanSetEntity(
                        id = set.id,
                        planExerciseId = exercise.id,
                        position = index,
                        weightKg = set.weightKg,
                        reps = set.reps,
                    )
                }
            },
        )
    }

    private suspend fun migrateLegacyPlans() {
        migrationMutex.withLock {
            if (migrationChecked) return@withLock
            migrationChecked = true
            if (dao.count() > 0) {
                preferences.edit().remove(LEGACY_KEY).apply()
                return@withLock
            }
            val legacyPlans = parseLegacyPlans()
            legacyPlans.forEach { saveToRoom(it.withStructuredExercises()) }
            if (legacyPlans.isNotEmpty()) {
                preferences.edit().remove(LEGACY_KEY).apply()
            }
        }
    }

    private fun parseLegacyPlans(): List<CustomWorkoutPlan> = runCatching {
        val json = JSONArray(preferences.getString(LEGACY_KEY, "[]"))
        (0 until json.length()).map { index ->
            val item = json.getJSONObject(index)
            val exercises = item.getJSONArray("exercises")
            val names = (0 until exercises.length()).map(exercises::getString)
            val plannedJson = item.optJSONArray("plannedExercises")
            CustomWorkoutPlan(
                id = item.getString("id"),
                title = item.getString("title"),
                type = WorkoutType.valueOf(item.getString("type")),
                durationMinutes = item.getInt("durationMinutes"),
                exercises = names,
                plannedExercises = if (plannedJson == null) {
                    emptyList()
                } else {
                    (0 until plannedJson.length()).map { exerciseIndex ->
                        val exercise = plannedJson.getJSONObject(exerciseIndex)
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

    private fun CustomWorkoutPlan.withStructuredExercises(): CustomWorkoutPlan {
        if (plannedExercises.isNotEmpty()) return this
        return copy(
            plannedExercises = exercises.map { name ->
                val catalogItem = ExerciseCatalog.exercises.firstOrNull { it.name == name }
                PlannedExercise(
                    exerciseId = catalogItem?.id ?: "custom-${name.hashCode()}",
                    exerciseName = name,
                    sets = List(3) { PlannedSet(weightKg = 10.0, reps = 10) },
                )
            },
        )
    }

    private companion object {
        const val LEGACY_PREFERENCES = "custom_workout_plans"
        const val LEGACY_KEY = "plans"
    }
}
