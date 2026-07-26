package com.openai.interactivefitness.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.openai.interactivefitness.FitnessApplication

class HealthConnectSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val application = applicationContext as FitnessApplication
        val firebaseStatus = application.firebaseSyncService?.status?.value
        if (
            firebaseStatus == null ||
            firebaseStatus == FirebaseSyncStatus.SIGNED_OUT ||
            firebaseStatus == FirebaseSyncStatus.UNCONFIGURED
        ) {
            return Result.success()
        }
        val manager = HealthConnectManager(applicationContext)
        if (manager.status() != HealthConnectStatus.READY) return Result.success()
        return runCatching {
            manager.readRecentWorkouts().forEach {
                application.workoutRepository.save(it)
            }
        }.fold(
            onSuccess = { Result.success() },
            onFailure = { Result.retry() },
        )
    }
}
