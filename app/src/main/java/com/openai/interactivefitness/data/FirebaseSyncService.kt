package com.openai.interactivefitness.data

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.openai.interactivefitness.domain.WorkoutSession
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine

enum class FirebaseSyncStatus {
    UNCONFIGURED,
    SIGNED_OUT,
    AUTHENTICATING,
    READY,
    SYNCING,
    FAILED,
}

class GoogleSignInRequiredException :
    IllegalStateException("Google sign-in is required for cloud sync")

class FirebaseSyncService private constructor(
    app: FirebaseApp,
) {
    private val context = app.applicationContext
    private val auth = FirebaseAuth.getInstance(app)
    private val firestore = FirebaseFirestore.getInstance(app)
    private val mutableStatus = MutableStateFlow(
        if (auth.currentUser?.isAnonymous == false) {
            FirebaseSyncStatus.READY
        } else {
            FirebaseSyncStatus.SIGNED_OUT
        },
    )
    val status: StateFlow<FirebaseSyncStatus> = mutableStatus.asStateFlow()

    fun isGoogleSignedIn(): Boolean =
        auth.currentUser?.let { !it.isAnonymous } == true

    init {
        if (auth.currentUser?.isAnonymous == true) auth.signOut()
    }

    suspend fun signInWithGoogle(idToken: String) {
        mutableStatus.value = FirebaseSyncStatus.AUTHENTICATING
        runCatching {
            auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null))
                .awaitResult<AuthResult>()
        }.onSuccess {
            mutableStatus.value = FirebaseSyncStatus.READY
        }.onFailure {
            mutableStatus.value = FirebaseSyncStatus.SIGNED_OUT
        }.getOrThrow()
    }

    fun signOut() {
        auth.signOut()
        mutableStatus.value = FirebaseSyncStatus.SIGNED_OUT
    }

    fun scheduleRetry() {
        val request = OneTimeWorkRequestBuilder<FirebaseSyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "firebase-workout-sync",
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    suspend fun syncWorkout(workout: WorkoutSession) {
        val uid = ensureAuthenticated()
        mutableStatus.value = FirebaseSyncStatus.SYNCING
        runCatching {
            firestore.collection("users")
                .document(uid)
                .collection("workoutSessions")
                .document(workout.id)
                .set(workout.toFirestoreMap())
                .awaitResult()
        }.onSuccess {
            mutableStatus.value = FirebaseSyncStatus.READY
        }.onFailure {
            mutableStatus.value = FirebaseSyncStatus.FAILED
            throw it
        }
    }

    suspend fun deleteWorkout(id: String) {
        val uid = ensureAuthenticated()
        mutableStatus.value = FirebaseSyncStatus.SYNCING
        runCatching {
            firestore.collection("users")
                .document(uid)
                .collection("workoutSessions")
                .document(id)
                .delete()
                .awaitResult()
        }.onSuccess {
            mutableStatus.value = FirebaseSyncStatus.READY
        }.onFailure {
            mutableStatus.value = FirebaseSyncStatus.FAILED
            throw it
        }
    }

    suspend fun syncAll(workouts: List<WorkoutSession>) {
        ensureAuthenticated()
        workouts.forEach { syncWorkout(it) }
    }

    private suspend fun ensureAuthenticated(): String {
        val user = auth.currentUser
        if (user != null && !user.isAnonymous) return user.uid
        mutableStatus.value = FirebaseSyncStatus.SIGNED_OUT
        throw GoogleSignInRequiredException()
    }

    companion object {
        fun createOrNull(context: Context): FirebaseSyncService? =
            FirebaseApp.getApps(context).firstOrNull()?.let(::FirebaseSyncService)
    }
}

private fun WorkoutSession.toFirestoreMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "type" to type.name,
    "title" to title,
    "startedAt" to startedAt.toString(),
    "durationMinutes" to durationMinutes,
    "rpe" to rpe,
    "detail" to detail,
    "sourceRecommendationId" to sourceRecommendationId,
    "recommendationDate" to recommendationDate?.toString(),
    "strengthSets" to strengthSets.map {
        mapOf(
            "id" to it.id,
            "exercise" to it.exercise,
            "weightKg" to it.weightKg,
            "reps" to it.reps,
            "rpe" to it.rpe,
        )
    },
    "intervals" to intervals.map {
        mapOf(
            "id" to it.id,
            "durationSeconds" to it.durationSeconds,
            "distanceMeters" to it.distanceMeters,
            "note" to it.note,
        )
    },
    "updatedAt" to com.google.firebase.Timestamp.now(),
)

private suspend fun <T> Task<T>.awaitResult(): T =
    suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { task ->
            if (task.isSuccessful) {
                @Suppress("UNCHECKED_CAST")
                continuation.resume(task.result as T)
            } else {
                continuation.resumeWithException(
                    task.exception ?: IllegalStateException("Firebase task failed"),
                )
            }
        }
    }
