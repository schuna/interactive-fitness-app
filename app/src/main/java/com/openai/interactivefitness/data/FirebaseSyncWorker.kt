package com.openai.interactivefitness.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.openai.interactivefitness.FitnessApplication
import kotlinx.coroutines.flow.first

class FirebaseSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val application = applicationContext as FitnessApplication
        val service = application.firebaseSyncService ?: return Result.success()
        return runCatching {
            service.syncAll(application.workoutRepository.workouts.first())
        }.fold(
            onSuccess = { Result.success() },
            onFailure = { Result.retry() },
        )
    }
}
