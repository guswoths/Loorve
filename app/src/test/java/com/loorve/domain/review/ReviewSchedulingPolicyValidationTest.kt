package com.loorve.domain.review

import com.loorve.domain.usecase.notificationEvents
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewSchedulingPolicyValidationTest {
    private val zone = java.time.ZoneId.of("Asia/Seoul")
    private val today = LocalDate.of(2026, 10, 1)

    @Test
    fun `minimum exam date scenario is enforced`() {
        val blocked = ReviewSchedulingEngine.validateExamDate(today, LocalDate.of(2026, 10, 4), 1)
            as ValidationResult.Blocked
        assertEquals(2L, blocked.availableDaysForNewLearning)
        assertEquals(LocalDate.of(2026, 10, 6), blocked.recommendedEarliestExamDate)
        assertTrue(ReviewSchedulingEngine.validateExamDate(today, LocalDate.of(2026, 10, 6), 1) is ValidationResult.Valid)
    }

    @Test
    fun `today and past exam dates are always blocked`() {
        assertTrue(ReviewSchedulingEngine.validateExamDate(today, today, 0) is ValidationResult.Blocked)
        assertTrue(ReviewSchedulingEngine.validateExamDate(today, today.minusDays(1), 2) is ValidationResult.Blocked)
    }

    @Test
    fun `last review date respects zero one and two day buffers`() {
        val exam = LocalDate.of(2026, 10, 20)
        assertEquals(LocalDate.of(2026, 10, 19), ReviewSchedulingEngine.lastReviewDate(exam, 0))
        assertEquals(LocalDate.of(2026, 10, 18), ReviewSchedulingEngine.lastReviewDate(exam, 1))
        assertEquals(LocalDate.of(2026, 10, 17), ReviewSchedulingEngine.lastReviewDate(exam, 2))
    }

    @Test
    fun `review count boundaries are exact`() {
        val expected = mapOf(3L to 2, 6L to 2, 7L to 3, 13L to 3, 14L to 4, 29L to 4,
            30L to 5, 59L to 5, 60L to 6, 119L to 6, 120L to 7)
        expected.forEach { (days, count) ->
            assertEquals(count, ReviewSchedulingEngine.getMinimumReviewCount(days))
        }
        assertEquals(0, ReviewSchedulingEngine.getMinimumReviewCount(2))
    }

    @Test
    fun `hard or low mastery adds at most one review`() {
        assertEquals(4, ReviewSchedulingEngine.getTargetReviewCount(7, ReviewDifficulty.HARD, 2))
        assertEquals(4, ReviewSchedulingEngine.getTargetReviewCount(7, ReviewDifficulty.MEDIUM, 2))
        assertEquals(4, ReviewSchedulingEngine.getTargetReviewCount(7, ReviewDifficulty.HARD, 3))
    }

    @Test
    fun `sufficient example produces durable dates and methods`() {
        val result = create(
            recordDate = LocalDate.of(2026, 10, 1),
            examDate = LocalDate.of(2026, 11, 15),
            difficulty = ReviewDifficulty.MEDIUM,
            importance = ReviewImportance.NORMAL,
            mastery = 3,
            minutes = 15
        )
        assertEquals(LocalDate.of(2026, 11, 13), result.lastReviewDate)
        assertEquals(43L, result.effectiveStudyDays)
        assertEquals(5, result.targetReviewCount)
        assertEquals(
            listOf(
                LocalDate.of(2026, 10, 2),
                LocalDate.of(2026, 10, 6),
                LocalDate.of(2026, 10, 12),
                LocalDate.of(2026, 10, 22),
                LocalDate.of(2026, 11, 13)
            ),
            result.schedules.map { it.scheduledDate }
        )
        assertTrue(result.schedules.all { it.recommendedMethod.isNotBlank() })
        assertEquals(result.lastReviewDate, result.schedules.last().scheduledDate)
    }

    @Test
    fun `compressed example preserves protected last date`() {
        val result = create(
            recordDate = LocalDate.of(2026, 10, 1),
            examDate = LocalDate.of(2026, 10, 12),
            difficulty = ReviewDifficulty.HARD,
            importance = ReviewImportance.HIGH,
            mastery = 2,
            minutes = 20
        )
        assertEquals(LocalDate.of(2026, 10, 10), result.lastReviewDate)
        assertEquals(9L, result.effectiveStudyDays)
        assertEquals(4, result.targetReviewCount)
        assertEquals(
            listOf(
                LocalDate.of(2026, 10, 2),
                LocalDate.of(2026, 10, 4),
                LocalDate.of(2026, 10, 6),
                LocalDate.of(2026, 10, 10)
            ),
            result.schedules.map { it.scheduledDate }
        )
        assertEquals(ReviewPlanStatus.SCHEDULED, result.status)
        assertTrue(result.compressed)
        assertFalse(result.schedules.any { it.scheduledDate >= LocalDate.of(2026, 10, 11) })
    }

    @Test
    fun `generated dates are unique ascending and at least one day apart`() {
        val result = create(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 15))
        val dates = result.schedules.map { it.scheduledDate }
        assertEquals(dates.distinct().sorted(), dates)
        assertTrue(dates.zipWithNext().all { (a, b) -> java.time.temporal.ChronoUnit.DAYS.between(a, b) >= 1 })
    }

    @Test
    fun `cram status is exposed below three effective days`() {
        val result = create(LocalDate.of(2026, 10, 8), LocalDate.of(2026, 10, 12))
        assertEquals(ReviewPlanStatus.CRAM_MODE_REQUIRED, result.status)
        assertEquals(2L, result.effectiveStudyDays)
        assertEquals(0, result.generatedReviewCount)
    }

    @Test
    fun `all four outcomes reschedule without violating boundaries`() {
        val outcomes = listOf(
            ReviewOutcome.EASY to 7L,
            ReviewOutcome.SUCCESS to 6L,
            ReviewOutcome.DIFFICULT to 3L,
            ReviewOutcome.FAILURE to 2L
        )
        outcomes.forEach { (outcome, expectedDateOffset) ->
            val first = entry("first", today.plusDays(1))
            val next = entry("next", today.plusDays(5))
            val result = ReviewSchedulingEngine.rescheduleAfterReviewOutcome(
                listOf(first, next), "first", outcome, today,
                SchedulerExam(examDate = today.plusDays(20), finalReviewBufferDays = 1)
            )
            assertTrue(result.applied)
            assertEquals(today.plusDays(expectedDateOffset), result.schedules.last().scheduledDate)
            assertTrue(result.schedules.map { it.scheduledDate }.distinct().size == 2)
            assertTrue(result.schedules.all { it.scheduledDate < today.plusDays(19) })
        }
    }

    @Test
    fun `daily overload redistribution moves movable work and reports impossible final overload`() {
        val date = today.plusDays(2)
        val movable = ReviewScheduleEntry("movable", "movable", 1, date, estimatedReviewMinutes = 40)
        val result = ReviewSchedulingEngine.rebalanceDailyLoad(
            listOf(movable, movable.copy(reviewId = "second", studyRecordId = "second")),
            SchedulerExam(examDate = today.plusDays(30), maxDailyReviewMinutes = 60),
            today.plusDays(1)
        )
        assertEquals(ReviewPlanStatus.SCHEDULED, result.status)
        assertNotEquals(date, result.schedules.first { it.reviewId == "second" }.scheduledDate)

        val unresolved = ReviewSchedulingEngine.rebalanceDailyLoad(
            listOf(movable.copy(reviewId = "final", isFinalReview = true, estimatedReviewMinutes = 120)),
            SchedulerExam(examDate = today.plusDays(30), maxDailyReviewMinutes = 60),
            today.plusDays(1)
        )
        assertEquals(ReviewPlanStatus.OVERLOADED_UNRESOLVED, unresolved.status)
        assertFalse(unresolved.warningMessage.isNullOrBlank())
    }

    @Test
    fun `records added over time keep per-record spacing and expose final-day overload`() {
        val exam = SchedulerExam(
            examDate = LocalDate.of(2026, 11, 15),
            maxDailyReviewMinutes = 30,
            finalReviewBufferDays = 1
        )
        val a = create(LocalDate.of(2026, 10, 1), exam.examDate)
        val b = create(
            LocalDate.of(2026, 10, 8), exam.examDate,
            difficulty = ReviewDifficulty.HARD,
            importance = ReviewImportance.HIGH,
            mastery = 2,
            minutes = 20
        )
        val c = create(
            LocalDate.of(2026, 10, 25), exam.examDate,
            difficulty = ReviewDifficulty.EASY,
            importance = ReviewImportance.NORMAL,
            mastery = 4
        )
        assertEquals(43L, a.effectiveStudyDays)
        assertEquals(36L, b.effectiveStudyDays)
        assertEquals(19L, c.effectiveStudyDays)
        assertEquals(
            listOf(
                LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 6),
                LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 22),
                LocalDate.of(2026, 11, 13)
            ),
            a.schedules.map { it.scheduledDate }
        )
        assertEquals(
            listOf(
                LocalDate.of(2026, 10, 9), LocalDate.of(2026, 10, 11),
                LocalDate.of(2026, 10, 13), LocalDate.of(2026, 10, 17),
                LocalDate.of(2026, 10, 27), LocalDate.of(2026, 11, 13)
            ),
            b.schedules.map { it.scheduledDate }
        )
        assertEquals(
            listOf(
                LocalDate.of(2026, 10, 26), LocalDate.of(2026, 10, 30),
                LocalDate.of(2026, 11, 4), LocalDate.of(2026, 11, 13)
            ),
            c.schedules.map { it.scheduledDate }
        )
        val all = a.schedules + b.schedules + c.schedules
        val before = all.groupingBy { it.scheduledDate }.fold(0) { total, item ->
            total + item.estimatedReviewMinutes
        }
        assertEquals(50, before[LocalDate.of(2026, 11, 13)])
        val after = ReviewSchedulingEngine.rebalanceDailyLoad(
            schedules = all,
            exam = exam,
            reviewStartDate = LocalDate.of(2026, 10, 2)
        )
        assertEquals(ReviewPlanStatus.OVERLOADED_UNRESOLVED, after.status)
        assertEquals(before, after.schedules.groupingBy { it.scheduledDate }.fold(0) { total, item ->
            total + item.estimatedReviewMinutes
        })
    }

    @Test
    fun `no generated schedule or notification is placed on exam date`() {
        val examDate = LocalDate.of(2026, 11, 15)
        val result = create(LocalDate.of(2026, 10, 1), examDate)
        assertTrue(result.schedules.none { it.scheduledDate >= examDate.minusDays(1) })
        assertTrue(result.schedules.flatMap {
            com.loorve.domain.model.ReviewScheduleItem(
                id = it.reviewId,
                studyRecordId = it.studyRecordId,
                reviewDate = it.scheduledDate.atStartOfDay(zone).toInstant().toEpochMilli(),
                planStatus = it.status
            ).notificationEvents("uid", zone, examDate)
        }
            .all { it.scheduledDate < examDate })
    }

    private fun create(
        recordDate: LocalDate,
        examDate: LocalDate,
        difficulty: ReviewDifficulty = ReviewDifficulty.MEDIUM,
        importance: ReviewImportance = ReviewImportance.NORMAL,
        mastery: Int? = 3,
        minutes: Int = 15
    ): SchedulingResult = ReviewSchedulingEngine.createReviewSchedules(
        SchedulerStudyRecord(
            studyRecordId = "record",
            studiedAtDate = recordDate,
            difficulty = difficulty,
            importance = importance,
            initialMastery = mastery,
            estimatedReviewMinutes = minutes
        ),
        SchedulerExam(examDate = examDate, maxDailyReviewMinutes = 120),
        today = recordDate,
        config = SchedulerConfig(finalReviewBufferDays = 1)
    )

    private fun entry(id: String, date: LocalDate) = ReviewScheduleEntry(
        reviewId = id,
        studyRecordId = "record",
        reviewIndex = 1,
        scheduledDate = date
    )
}
