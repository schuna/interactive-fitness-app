package com.openai.interactivefitness

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.openai.interactivefitness.data.AppDatabase
import com.openai.interactivefitness.data.ActiveWorkoutStore
import com.openai.interactivefitness.data.ErrorLogStore
import com.openai.interactivefitness.data.FirebaseSyncService
import com.openai.interactivefitness.data.RoomWorkoutRepository
import com.openai.interactivefitness.data.WorkoutRepository
import com.openai.interactivefitness.data.CustomWorkoutPlanStore
import com.openai.interactivefitness.data.GeminiIntentRouter

class FitnessApplication : Application() {
    private val database: AppDatabase by lazy {
        AppDatabase.create(this)
    }

    override fun onCreate() {
        super.onCreate()
        configureFirebaseAppCheck()
    }

    val activeWorkoutStore: ActiveWorkoutStore by lazy {
        ActiveWorkoutStore(this)
    }
    val errorLogStore: ErrorLogStore by lazy {
        ErrorLogStore(this)
    }
    val customWorkoutPlanStore: CustomWorkoutPlanStore by lazy {
        CustomWorkoutPlanStore(this, database.customWorkoutPlanDao())
    }
    val firebaseSyncService: FirebaseSyncService? by lazy {
        FirebaseSyncService.createOrNull(this)
    }
    val geminiIntentRouter: GeminiIntentRouter? by lazy {
        if (FirebaseApp.getApps(this).isEmpty()) null
        else runCatching { GeminiIntentRouter() }.getOrNull()
    }

    val workoutRepository: WorkoutRepository by lazy {
        RoomWorkoutRepository(
            database.workoutDao(),
        )
    }

    private fun configureFirebaseAppCheck() {
        if (FirebaseApp.getApps(this).isEmpty()) return

        val providerFactory = if (BuildConfig.DEBUG) {
            runCatching {
                Class.forName(
                    "com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory",
                ).getMethod("getInstance")
                    .invoke(null) as AppCheckProviderFactory
            }.getOrElse {
                Log.w("FitnessApplication", "Debug App Check provider unavailable", it)
                PlayIntegrityAppCheckProviderFactory.getInstance()
            }
        } else {
            PlayIntegrityAppCheckProviderFactory.getInstance()
        }

        FirebaseAppCheck.getInstance().apply {
            installAppCheckProviderFactory(providerFactory)
            if (BuildConfig.DEBUG) {
                getAppCheckToken(false)
                    .addOnFailureListener {
                        Log.w("FitnessApplication", "App Check debug token request failed", it)
                    }
            }
        }
    }
}
