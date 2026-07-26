package com.openai.interactivefitness.data

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.openai.interactivefitness.domain.WorkoutSession
import com.openai.interactivefitness.domain.WorkoutType
import com.openai.interactivefitness.domain.WorkoutDataSource
import com.openai.interactivefitness.domain.HealthConnectSyncState
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

enum class HealthConnectStatus {
    UNAVAILABLE,
    UPDATE_REQUIRED,
    PERMISSION_REQUIRED,
    READY,
}

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
        return response.records.map { record ->
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
}
