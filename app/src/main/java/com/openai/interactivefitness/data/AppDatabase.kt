package com.openai.interactivefitness.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        WorkoutEntity::class,
        StrengthSetEntity::class,
        WorkoutIntervalEntity::class,
        CustomWorkoutPlanEntity::class,
        CustomPlanExerciseEntity::class,
        CustomPlanSetEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
    abstract fun customWorkoutPlanDao(): CustomWorkoutPlanDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "interactive-fitness.db",
            ).addMigrations(
                MIGRATION_1_2,
                MIGRATION_2_3,
                MIGRATION_3_4,
                MIGRATION_4_5,
            ).build()

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS strength_sets (
                        id TEXT NOT NULL PRIMARY KEY,
                        workoutId TEXT NOT NULL,
                        position INTEGER NOT NULL,
                        exercise TEXT NOT NULL,
                        weightKg REAL NOT NULL,
                        reps INTEGER NOT NULL,
                        rpe INTEGER NOT NULL,
                        FOREIGN KEY(workoutId) REFERENCES workout_sessions(id) ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_strength_sets_workoutId ON strength_sets(workoutId)",
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS workout_intervals (
                        id TEXT NOT NULL PRIMARY KEY,
                        workoutId TEXT NOT NULL,
                        position INTEGER NOT NULL,
                        durationSeconds INTEGER NOT NULL,
                        distanceMeters INTEGER NOT NULL,
                        note TEXT NOT NULL,
                        FOREIGN KEY(workoutId) REFERENCES workout_sessions(id) ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_workout_intervals_workoutId ON workout_intervals(workoutId)",
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE workout_sessions ADD COLUMN sourceRecommendationId TEXT",
                )
                db.execSQL(
                    "ALTER TABLE workout_sessions ADD COLUMN recommendationDate TEXT",
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE workout_sessions ADD COLUMN dataSource TEXT NOT NULL DEFAULT 'LOCAL'",
                )
                db.execSQL(
                    "ALTER TABLE workout_sessions ADD COLUMN externalRecordId TEXT",
                )
                db.execSQL(
                    "ALTER TABLE workout_sessions ADD COLUMN sourceModifiedAt TEXT",
                )
                db.execSQL(
                    "ALTER TABLE workout_sessions ADD COLUMN healthConnectSyncState TEXT NOT NULL DEFAULT 'NOT_SYNCED'",
                )
                db.execSQL(
                    "ALTER TABLE workout_sessions ADD COLUMN lastSyncedAt TEXT",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_workout_sessions_externalRecordId " +
                        "ON workout_sessions(externalRecordId)",
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS custom_workout_plans (
                        id TEXT NOT NULL PRIMARY KEY,
                        title TEXT NOT NULL,
                        type TEXT NOT NULL,
                        durationMinutes INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS custom_plan_exercises (
                        id TEXT NOT NULL PRIMARY KEY,
                        planId TEXT NOT NULL,
                        exerciseId TEXT NOT NULL,
                        exerciseName TEXT NOT NULL,
                        position INTEGER NOT NULL,
                        restSeconds INTEGER NOT NULL,
                        FOREIGN KEY(planId) REFERENCES custom_workout_plans(id) ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_custom_plan_exercises_planId " +
                        "ON custom_plan_exercises(planId)",
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS custom_plan_sets (
                        id TEXT NOT NULL PRIMARY KEY,
                        planExerciseId TEXT NOT NULL,
                        position INTEGER NOT NULL,
                        weightKg REAL NOT NULL,
                        reps INTEGER NOT NULL,
                        FOREIGN KEY(planExerciseId) REFERENCES custom_plan_exercises(id) ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_custom_plan_sets_planExerciseId " +
                        "ON custom_plan_sets(planExerciseId)",
                )
            }
        }
    }
}
