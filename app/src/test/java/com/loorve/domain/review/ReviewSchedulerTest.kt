package com.loorve.domain.review

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class ReviewSchedulerTest {

    private val today = LocalDate.of(2026, 9, 1)
    private val examDate = today.plusDays(30)

    // ──────────────────────────────────────────
    // generateSchedule 기본 테스트
    // ──────────────────────────────────────────

    @Test
    fun `generateSchedule - 정상 케이스 시 5회차 이하 일정 생성`() {
        val result = ReviewScheduler.generateSchedule(
            learningDate = today,
            examDate = examDate,
            studyRecordId = "rec001",
            title = "테스트 학습 내용",
            blockId = "block001",
            uid = "user001"
        )
        assertTrue("복습 일정이 최소 1개 이상이어야 함", result.items.isNotEmpty())
        assertTrue("복습 일정이 5개 이하여야 함", result.items.size <= 5)
    }

    @Test
    fun `generateSchedule - 모든 복습일은 학습일 이후이고 시험일 이전이어야 함`() {
        val result = ReviewScheduler.generateSchedule(
            learningDate = today,
            examDate = examDate,
            studyRecordId = "rec002",
            title = "범위 검증",
            blockId = "block002",
            uid = "user001"
        )
        result.items.forEach { item ->
            val reviewDate = item.reviewDate.toLocalDate()
            assertTrue("복습일은 학습일보다 미래여야 함", reviewDate > today)
            assertTrue("복습일은 시험일 이전이어야 함", reviewDate < examDate)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `generateSchedule - 시험일이 학습일과 같으면 예외 발생`() {
        ReviewScheduler.generateSchedule(
            learningDate = today,
            examDate = today, // ❌ 잘못된 입력
            studyRecordId = "rec003",
            title = "예외 테스트",
            blockId = "block003",
            uid = "user001"
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `generateSchedule - 시험일이 학습일보다 과거이면 예외 발생`() {
        ReviewScheduler.generateSchedule(
            learningDate = today,
            examDate = today.minusDays(1), // ❌ 잘못된 입력
            studyRecordId = "rec004",
            title = "예외 테스트2",
            blockId = "block004",
            uid = "user001"
        )
    }

    // ──────────────────────────────────────────
    // completionRate 테스트
    // ──────────────────────────────────────────

    @Test
    fun `completionRate - 정상 계산`() {
        assertEquals(0.5, ReviewScheduler.completionRate(10, 5), 0.001)
    }

    @Test
    fun `completionRate - totalCount가 0이면 0 반환`() {
        assertEquals(0.0, ReviewScheduler.completionRate(0, 0), 0.001)
    }

    @Test
    fun `completionRate - 전체 완료 시 1점`() {
        assertEquals(1.0, ReviewScheduler.completionRate(5, 5), 0.001)
    }

    // ──────────────────────────────────────────
    // isAtRisk 테스트
    // ──────────────────────────────────────────

    @Test
    fun `isAtRisk - 미완료 수가 남은 날보다 많으면 true`() {
        assertTrue(ReviewScheduler.isAtRisk(daysUntilExam = 2, overdueCount = 3))
    }

    @Test
    fun `isAtRisk - 미완료가 0이면 false`() {
        assertFalse(ReviewScheduler.isAtRisk(daysUntilExam = 1, overdueCount = 0))
    }

    // ──────────────────────────────────────────
    // retrievabilityEstimate 테스트
    // ──────────────────────────────────────────

    @Test
    fun `retrievabilityEstimate - overdueDays가 0이면 1점`() {
        val result = ReviewScheduler.retrievabilityEstimate(overdueDays = 0L, successCount = 1)
        assertEquals(1.0, result, 0.001)
    }

    @Test
    fun `retrievabilityEstimate - overdueDays가 클수록 값이 감소`() {
        val r1 = ReviewScheduler.retrievabilityEstimate(1L, 1)
        val r2 = ReviewScheduler.retrievabilityEstimate(5L, 1)
        assertTrue("overdue가 길수록 retrievability가 낮아야 함", r1 > r2)
    }

    // ──────────────────────────────────────────
    // calcDeadlineBufferDays 테스트
    // ──────────────────────────────────────────

    @Test
    fun `calcDeadlineBufferDays - 최소 3일 이상 반환`() {
        val buf = ReviewScheduler.calcDeadlineBufferDays(
            prepStartDate = today,
            examDate = today.plusDays(10)
        )
        assertTrue("버퍼는 최소 3일이어야 함", buf >= 3L)
    }

    // ──────────────────────────────────────────
    // LocalDate 확장 함수 테스트
    // ──────────────────────────────────────────

    @Test
    fun `toEpochMillis - toLalDate 라운드트립 일치`() {
        val original = today
        val millis = original.toEpochMillis()
        val restored = millis.toLocalDate()
        assertEquals(original, restored)
    }
}