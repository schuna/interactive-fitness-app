package com.openai.interactivefitness

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.openai.interactivefitness.data.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CustomPlanMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migration4To5CreatesCustomPlanTablesWithoutChangingWorkoutData() {
        helper.createDatabase(TEST_DATABASE, 4).apply {
            execSQL(
                """
                INSERT INTO workout_sessions (
                    id, type, title, startedAt, durationMinutes, rpe, detail,
                    sourceRecommendationId, recommendationDate, dataSource,
                    externalRecordId, sourceModifiedAt, healthConnectSyncState, lastSyncedAt
                ) VALUES (
                    'existing', 'STRENGTH', '기존 기록', '2026-07-26T10:00:00',
                    30, 6, '', NULL, NULL, 'LOCAL', NULL, NULL, 'NOT_SYNCED', NULL
                )
                """.trimIndent(),
            )
            close()
        }

        helper.runMigrationsAndValidate(
            TEST_DATABASE,
            5,
            true,
            AppDatabase.MIGRATION_4_5,
        ).use { database ->
            database.query(
                "SELECT COUNT(*) FROM workout_sessions WHERE id = 'existing'",
            ).use { cursor ->
                cursor.moveToFirst()
                assertEquals(1, cursor.getInt(0))
            }
            database.query(
                "SELECT COUNT(*) FROM custom_workout_plans",
            ).use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
            }
        }
    }

    private companion object {
        const val TEST_DATABASE = "custom-plan-migration-test"
    }
}
