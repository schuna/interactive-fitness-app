package com.openai.interactivefitness

import android.app.Application
import com.openai.interactivefitness.data.AppDatabase
import com.openai.interactivefitness.data.ActiveWorkoutStore
import com.openai.interactivefitness.data.ErrorLogStore
import com.openai.interactivefitness.data.FirebaseSyncService
import com.openai.interactivefitness.data.RoomWorkoutRepository
import com.openai.interactivefitness.data.WorkoutRepository

class FitnessApplication : Application() {
    val activeWorkoutStore: ActiveWorkoutStore by lazy {
        ActiveWorkoutStore(this)
    }
    val errorLogStore: ErrorLogStore by lazy {
        ErrorLogStore(this)
    }
    val firebaseSyncService: FirebaseSyncService? by lazy {
        FirebaseSyncService.createOrNull(this)
    }

    val workoutRepository: WorkoutRepository by lazy {
        RoomWorkoutRepository(
            AppDatabase.create(this).workoutDao(),
        )
    }
}
