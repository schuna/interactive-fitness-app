package com.openai.interactivefitness

import android.app.Application
import com.openai.interactivefitness.data.AppDatabase
import com.openai.interactivefitness.data.RoomWorkoutRepository
import com.openai.interactivefitness.data.WorkoutRepository

class FitnessApplication : Application() {
    val workoutRepository: WorkoutRepository by lazy {
        RoomWorkoutRepository(
            AppDatabase.create(this).workoutDao(),
        )
    }
}
