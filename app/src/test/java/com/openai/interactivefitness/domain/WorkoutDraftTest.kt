package com.openai.interactivefitness.domain

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutDraftTest {
    @Test
    fun rejectsBlankTitleAndOutOfRangeNumbers() {
        val errors = WorkoutDraft(
            title = " ",
            durationMinutes = "0",
            rpe = "11",
        ).validate()

        assertTrue("title" in errors)
        assertTrue("durationMinutes" in errors)
        assertTrue("rpe" in errors)
    }

    @Test
    fun editingPreservesIdentityAndStartTime() {
        val original = WorkoutSession(
            id = "existing",
            type = WorkoutType.RUNNING,
            title = "기존 기록",
            startedAt = LocalDateTime.of(2026, 7, 26, 8, 0),
            durationMinutes = 30,
            rpe = 5,
            detail = "기존 상세",
        )
        val updated = WorkoutDraft(
            type = WorkoutType.RUNNING,
            title = "수정된 기록",
            durationMinutes = "40",
            rpe = "7",
            detail = "수정된 상세",
        ).toSession(original)

        assertEquals(original.id, updated.id)
        assertEquals(original.startedAt, updated.startedAt)
        assertEquals(40, updated.durationMinutes)
        assertEquals("수정된 기록", updated.title)
    }

    @Test
    fun structuredSetsAndIntervalsAreCopiedToSession() {
        val set = StrengthSet(
            exercise = "스쿼트",
            weightKg = 60.0,
            reps = 8,
            rpe = 7,
        )
        val interval = WorkoutInterval(
            durationSeconds = 300,
            distanceMeters = 1_000,
            note = "편안한 페이스",
        )
        val session = WorkoutDraft(
            type = WorkoutType.STRENGTH,
            title = "구조화 운동",
            strengthSets = listOf(set),
            intervals = listOf(interval),
        ).toSession(now = LocalDateTime.of(2026, 7, 26, 9, 0))

        assertEquals(listOf(set), session.strengthSets)
        assertEquals(listOf(interval), session.intervals)
    }
}
