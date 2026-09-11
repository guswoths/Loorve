package com.loorve.domain.review

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ReviewSchedulingEngineTest {

    private val today = LocalDate.of(2026, 10, 1)

    @Test
    fun `오늘 시험일은 차단된다`() {
        val result = ReviewSchedulingEngine.validateExamDate(today, today, 1)
        assertTrue(result is ValidationResult.Blocked)
    }

    @Test
    fun `과거 시험일은 차단된다`() {
        val result = ReviewSchedulingEngine.validateExamDate(today, today.minusDays(1), 1)
        assertTrue(result is ValidationResult.Blocked)
    }

    @Test
    fun `유효 학습기간이 3일 미만이면 차단된다`() {
        val result = ReviewSchedulingEngine.validateExamDate(today, today.plusDays(4), 1)
        assertTrue(result is ValidationResult.Blocked)
    }

    @Test
    fun `버퍼 1일의 최소 유효 시험일은 10월 6일이다`() {
        val valid = ReviewSchedulingEngine.validateExamDate(today, today.plusDays(5), 1)
        assertTrue(valid is ValidationResult.Valid)
        val blocked = ReviewSchedulingEngine.validateExamDate(today, today.plusDays(4), 1)
            as ValidationResult.Blocked
        assertEquals(LocalDate.of(2026, 10, 6), blocked.recommendedEarliestExamDate)
    }

    @Test
    fun `버퍼 0 1 2일의 마지막 정규 복습일을 계산한다`() {
        val exam = LocalDate.of(2026, 10, 20)
        assertEquals(exam.minusDays(1), ReviewSchedulingEngine.lastReviewDate(exam, 0))
        assertEquals(exam.minusDays(2), ReviewSchedulingEngine.lastReviewDate(exam, 1))
        assertEquals(exam.minusDays(3), ReviewSchedulingEngine.lastReviewDate(exam, 2))
    }

    @Test
    fun `복습 최소 회차 경계값을 계산한다`() {
        assertEquals(0, ReviewSchedulingEngine.getMinimumReviewCount(2))
        assertEquals(2, ReviewSchedulingEngine.getMinimumReviewCount(3))
        assertEquals(2, ReviewSchedulingEngine.getMinimumReviewCount(6))
        assertEquals(3, ReviewSchedulingEngine.getMinimumReviewCount(7))
        assertEquals(4, ReviewSchedulingEngine.getMinimumReviewCount(14))
        assertEquals(5, ReviewSchedulingEngine.getMinimumReviewCount(30))
        assertEquals(6, ReviewSchedulingEngine.getMinimumReviewCount(60))
        assertEquals(7, ReviewSchedulingEngine.getMinimumReviewCount(120))
    }

    @Test
    fun `난이도와 낮은 숙련도는 선호 회차를 추가한다`() {
        assertEquals(
            4,
            ReviewSchedulingEngine.getTargetReviewCount(7, ReviewDifficulty.HARD, 2)
        )
        assertEquals(
            3,
            ReviewSchedulingEngine.getTargetReviewCount(7, ReviewDifficulty.EASY, 5)
        )
    }

    @Test
    fun `생성 날짜는 중복 없이 오름차순이고 마지막 날짜에 고정된다`() {
        val exam = LocalDate.of(2026, 11, 15)
        val dates = ReviewSchedulingEngine.generateScaledReviewDates(
            studyDate = today,
            examDate = exam,
            finalReviewBufferDays = 1,
            targetReviewCount = 7
        )
        assertEquals(dates.distinct().sorted(), dates)
        assertEquals(exam.minusDays(2), dates.last())
        assertFalse(dates.any { it == exam })
    }

    @Test
    fun `기존 기록의 기간 부족은 기본 정책에서 압축 모드로 표시된다`() {
        val result = ReviewSchedulingEngine.createReviewSchedules(
            record = SchedulerStudyRecord("short", today),
            exam = SchedulerExam(examDate = today.plusDays(3)),
            today = today
        )
        assertEquals(ReviewPlanStatus.CRAM_MODE_REQUIRED, result.status)
        assertTrue(result.schedules.isEmpty())
    }

    @Test
    fun `완료되지 않은 학습기록은 정규 일정을 만들지 않는다`() {
        val result = ReviewSchedulingEngine.createReviewSchedules(
            record = SchedulerStudyRecord("incomplete", today, isCompleted = false),
            exam = SchedulerExam(examDate = today.plusDays(30)),
            today = today
        )
        assertTrue(result.schedules.isEmpty())
        assertEquals(ReviewPlanStatus.INSUFFICIENT_WINDOW, result.status)
    }

    @Test
    fun `하루 복습 시간 초과 시 낮은 우선순위 항목을 이동한다`() {
        val date = today.plusDays(2)
        val first = entry("a", date, 80, 10.0)
        val second = entry("b", date, 40, 1.0)
        val result = ReviewSchedulingEngine.rebalanceDailyLoad(
            schedules = listOf(first, second),
            exam = SchedulerExam(examDate = today.plusDays(30), maxDailyReviewMinutes = 100),
            reviewStartDate = today.plusDays(1)
        )
        assertEquals(ReviewPlanStatus.SCHEDULED, result.status)
        assertEquals(date, result.schedules.first { it.reviewId == "a" }.scheduledDate)
        assertTrue(result.schedules.first { it.reviewId == "b" }.scheduledDate != date)
    }

    @Test
    fun `이동할 수 없으면 과부하 미해결 상태를 반환한다`() {
        val date = today.plusDays(2)
        val result = ReviewSchedulingEngine.rebalanceDailyLoad(
            schedules = listOf(entry("final", date, 120, 100.0, isFinal = true)),
            exam = SchedulerExam(examDate = today.plusDays(30), maxDailyReviewMinutes = 60),
            reviewStartDate = today.plusDays(1)
        )
        assertEquals(ReviewPlanStatus.OVERLOADED_UNRESOLVED, result.status)
        assertFalse(result.warningMessage.isNullOrBlank())
    }

    @Test
    fun `어려운 결과는 다음 미래 복습을 앞당긴다`() {
        val first = entry("r1", today.plusDays(1), 10, 1.0)
        val next = entry("r2", today.plusDays(5), 10, 1.0)
        val result = ReviewSchedulingEngine.rescheduleAfterReviewOutcome(
            schedules = listOf(first, next),
            completedReviewId = "r1",
            outcome = ReviewOutcome.FAILURE,
            today = today,
            exam = SchedulerExam(examDate = today.plusDays(30))
        )
        assertTrue(result.applied)
        assertTrue(result.schedules.first { it.reviewId == "r2" }.scheduledDate.isBefore(next.scheduledDate))
    }

    @Test
    fun `쉬운 결과는 마지막 복습일을 넘기지 않고 늦출 수 있다`() {
        val first = entry("r1", today.plusDays(1), 10, 1.0)
        val next = entry("r2", today.plusDays(3), 10, 1.0)
        val result = ReviewSchedulingEngine.rescheduleAfterReviewOutcome(
            schedules = listOf(first, next),
            completedReviewId = "r1",
            outcome = ReviewOutcome.EASY,
            today = today,
            exam = SchedulerExam(examDate = today.plusDays(10), finalReviewBufferDays = 1)
        )
        assertTrue(result.applied)
        assertTrue(
            !result.schedules.first { it.reviewId == "r2" }.scheduledDate.isAfter(
                today.plusDays(8)
            )
        )
    }

    @Test
    fun `priority는 마지막 고우선 복습을 보호한다`() {
        val finalScore = ReviewSchedulingEngine.priorityScore(
            daysUntilExam = 2,
            importance = ReviewImportance.HIGH,
            difficulty = ReviewDifficulty.HARD,
            initialMastery = 1,
            isFinalReview = true
        )
        val ordinaryScore = ReviewSchedulingEngine.priorityScore(
            daysUntilExam = 10,
            importance = ReviewImportance.LOW,
            difficulty = ReviewDifficulty.EASY,
            initialMastery = 5,
            isFinalReview = false
        )
        assertTrue(finalScore > ordinaryScore)
    }

    @Test
    fun `일정에는 한국어 복습 방법과 알림 계획이 포함된다`() {
        val result = ReviewSchedulingEngine.createReviewSchedules(
            record = SchedulerStudyRecord("method", today, estimatedReviewMinutes = 15),
            exam = SchedulerExam(examDate = today.plusDays(30)),
            today = today
        )
        assertTrue(result.schedules.isNotEmpty())
        assertTrue(result.schedules.all { it.recommendedMethod.isNotBlank() })
        assertEquals(9, result.schedules.first().notificationPlan.defaultNotificationHour)
    }

    private fun entry(
        id: String,
        date: LocalDate,
        minutes: Int,
        priority: Double,
        isFinal: Boolean = false
    ) = ReviewScheduleEntry(
        reviewId = id,
        studyRecordId = id,
        reviewIndex = 1,
        scheduledDate = date,
        priorityScore = priority,
        estimatedReviewMinutes = minutes,
        isFinalReview = isFinal
    )
}
