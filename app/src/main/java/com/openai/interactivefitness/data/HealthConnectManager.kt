package com.openai.interactivefitness.data

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.openai.interactivefitness.domain.WorkoutSession
import com.openai.interactivefitness.domain.WorkoutType
import com.openai.interactivefitness.domain.WorkoutDataSource
import com.openai.interactivefitness.domain.HealthConnectSyncState
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

enum class HealthConnectStatus {
    UNAVAILABLE,
    UPDATE_REQUIRED,
    PERMISSION_REQUIRED,
    READY,
}

data class HealthConnectSummary(
    val steps: Long = 0,
    val distanceMeters: Long = 0,
    val averageHeartRate: Int? = null,
    val activeCaloriesKcal: Int = 0,
)

class HealthConnectManager(private val context: Context) {
    companion object {
        const val PROVIDER_PACKAGE_NAME = "com.google.android.apps.healthdata"
    }

    val permissions: Set<String> = setOf(
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getWritePermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
    )

    fun sdkStatus(): Int = HealthConnectClient.getSdkStatus(context)

    fun extendedPermissions(): Set<String> {
        if (sdkStatus() != HealthConnectClient.SDK_AVAILABLE) return emptySet()
        val features = HealthConnectClient.getOrCreate(context).features
        return buildSet {
            if (
                features.getFeatureStatus(
                    HealthConnectFeatures.FEATURE_READ_HEALTH_DATA_HISTORY,
                ) == HealthConnectFeatures.FEATURE_STATUS_AVAILABLE
            ) {
                add(HealthPermission.PERMISSION_READ_HEALTH_DATA_HISTORY)
            }
            if (
                features.getFeatureStatus(
                    HealthConnectFeatures.FEATURE_READ_HEALTH_DATA_IN_BACKGROUND,
                ) == HealthConnectFeatures.FEATURE_STATUS_AVAILABLE
            ) {
                add(HealthPermission.PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND)
            }
        }
    }

    fun scheduleBackgroundSync() {
        val request = PeriodicWorkRequestBuilder<HealthConnectSyncWorker>(
            6,
            TimeUnit.HOURS,
        ).setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build(),
        ).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "health-connect-workout-sync",
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    suspend fun status(): HealthConnectStatus =
        when (sdkStatus()) {
            HealthConnectClient.SDK_UNAVAILABLE -> HealthConnectStatus.UNAVAILABLE
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED ->
                HealthConnectStatus.UPDATE_REQUIRED
            else -> {
                val granted = HealthConnectClient.getOrCreate(context)
                    .permissionController
                    .getGrantedPermissions()
                if (granted.containsAll(permissions)) {
                    HealthConnectStatus.READY
                } else {
                    HealthConnectStatus.PERMISSION_REQUIRED
                }
            }
        }

    suspend fun hasPermissions(required: Set<String>): Boolean {
        if (required.isEmpty()) return true
        if (sdkStatus() != HealthConnectClient.SDK_AVAILABLE) return false
        return HealthConnectClient.getOrCreate(context)
            .permissionController
            .getGrantedPermissions()
            .containsAll(required)
    }

    suspend fun readRecentWorkouts(
        since: Instant = Instant.now().minus(Duration.ofDays(30)),
    ): List<WorkoutSession> {
        check(status() == HealthConnectStatus.READY) {
            "Health Connect permission is required"
        }
        val response = HealthConnectClient.getOrCreate(context).readRecords(
            ReadRecordsRequest(
                recordType = ExerciseSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.after(since),
                ascendingOrder = false,
            ),
        )
        return response.records
            .filter { it.metadata.dataOrigin.packageName != context.packageName }
            .map { record ->
            val type = when (record.exerciseType) {
                ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
                ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL -> WorkoutType.RUNNING
                ExerciseSessionRecord.EXERCISE_TYPE_BIKING,
                ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY -> WorkoutType.CYCLING
                ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING -> WorkoutType.STRENGTH
                else -> WorkoutType.RECOVERY
            }
            WorkoutSession(
                id = record.metadata.clientRecordId
                    ?: "health-connect:${record.metadata.id}",
                type = type,
                title = record.title?.toString()?.takeIf(String::isNotBlank)
                    ?: "Health Connect ${type.label}",
                startedAt = LocalDateTime.ofInstant(record.startTime, ZoneId.systemDefault()),
                durationMinutes = Duration.between(record.startTime, record.endTime)
                    .toMinutes().toInt().coerceAtLeast(1),
                rpe = 5,
                detail = "Health Connect에서 가져온 운동",
                dataSource = WorkoutDataSource.HEALTH_CONNECT,
                externalRecordId = record.metadata.id,
                sourceModifiedAt = LocalDateTime.ofInstant(
                    record.metadata.lastModifiedTime,
                    ZoneId.systemDefault(),
                ),
                healthConnectSyncState = HealthConnectSyncState.IMPORTED,
                lastSyncedAt = LocalDateTime.now(),
            )
            }
    }

    suspend fun readWeeklySummary(
        end: Instant = Instant.now(),
    ): HealthConnectSummary {
        check(status() == HealthConnectStatus.READY) {
            "Health Connect permission is required"
        }
        val result = HealthConnectClient.getOrCreate(context).aggregate(
            AggregateRequest(
                metrics = setOf(
                    StepsRecord.COUNT_TOTAL,
                    DistanceRecord.DISTANCE_TOTAL,
                    HeartRateRecord.BPM_AVG,
                    TotalCaloriesBurnedRecord.ENERGY_TOTAL,
                ),
                timeRangeFilter = TimeRangeFilter.between(
                    end.minus(Duration.ofDays(7)),
                    end,
                ),
            ),
        )
        return HealthConnectSummary(
            steps = result[StepsRecord.COUNT_TOTAL] ?: 0,
            distanceMeters = result[DistanceRecord.DISTANCE_TOTAL]
                ?.inMeters?.toLong() ?: 0,
            averageHeartRate = result[HeartRateRecord.BPM_AVG]?.toInt(),
            activeCaloriesKcal = result[TotalCaloriesBurnedRecord.ENERGY_TOTAL]
                ?.inKilocalories?.toInt() ?: 0,
        )
    }

    suspend fun writeWorkout(workout: WorkoutSession) {
        check(status() == HealthConnectStatus.READY) {
            "Health Connect permission is required"
        }
        val zoneId = ZoneId.systemDefault()
        val startTime = workout.startedAt.atZone(zoneId).toInstant()
        val endTime = startTime.plus(Duration.ofMinutes(workout.durationMinutes.toLong()))
        val startOffset = zoneId.rules.getOffset(startTime)
        val endOffset = zoneId.rules.getOffset(endTime)
        val exerciseType = when (workout.type) {
            WorkoutType.RUNNING -> ExerciseSessionRecord.EXERCISE_TYPE_RUNNING
            WorkoutType.CYCLING -> ExerciseSessionRecord.EXERCISE_TYPE_BIKING
            WorkoutType.STRENGTH -> ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING
            WorkoutType.RECOVERY -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
        }
        HealthConnectClient.getOrCreate(context).insertRecords(
            listOf(
                ExerciseSessionRecord(
                    startTime = startTime,
                    startZoneOffset = startOffset,
                    endTime = endTime,
                    endZoneOffset = endOffset,
                    exerciseType = exerciseType,
                    title = workout.title,
                    notes = workout.detail,
                    exerciseRoute = null,
                    metadata = Metadata.manualEntry(
                        clientRecordId = workout.id,
                        clientRecordVersion = 1,
                    ),
                ),
            ),
        )
    }

    suspend fun deleteWorkout(workout: WorkoutSession) {
        check(status() == HealthConnectStatus.READY) {
            "Health Connect permission is required"
        }
        HealthConnectClient.getOrCreate(context).deleteRecords(
            ExerciseSessionRecord::class,
            recordIdsList = listOfNotNull(workout.externalRecordId),
            clientRecordIdsList = if (workout.dataSource == WorkoutDataSource.LOCAL) {
                listOf(workout.id)
            } else {
                emptyList()
            },
        )
    }
}
