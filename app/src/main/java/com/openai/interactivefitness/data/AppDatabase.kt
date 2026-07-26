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
    ],
    version = 3,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "interactive-fitness.db",
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()

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
    }
}
