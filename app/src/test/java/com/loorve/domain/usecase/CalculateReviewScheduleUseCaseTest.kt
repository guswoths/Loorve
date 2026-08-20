package com.loorve.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * CalculateReviewScheduleUseCase 단위 테스트
 * 망각곡선 기반 자동 복습 스케줄링 로직 검증
 *
 * 테스트 전략: 외부 의존성 없는 순수 도메인 로직 → Mock 불필요
 */
class CalculateReviewScheduleUseCaseTest {

    private lateinit var useCase: CalculateReviewScheduleUseCase

    @Before
    fun setUp() {
        useCase = CalculateReviewScheduleUseCase()
    }

    // ─────────────────────────────────────────────────────────
    // 테스트 1: 표준 간격 적용 (시험일까지 30일 이상)
    // ─────────────────────────────────────────────────────────

    @Test
    fun `시험일까지 30일 이상 남은 경우 표준 복습 간격 5개가 생성된다`() {
        // Given
        val progressDate = LocalDate.of(2026, 8, 1)
        val examDate = LocalDate.of(2026, 10, 1) // 61일 차이

        // When
        val result = useCase.execute(progressDate, examDate)

        // Then: 리스트 크기 5개 검증
        assertEquals("표준 간격은 5개여야 한다", 5, result.size)
    }

    @Test
    fun `시험일까지 30일 이상 남은 경우 표준 복습 간격이 1_3_7_14_30일을 따른다`() {
        // Given
        val progressDate = LocalDate.of(2026, 8, 1)
        val examDate = LocalDate.of(2026, 10, 1)
        val expectedIntervals = listOf(1L, 3L, 7L, 14L, 30L)

        // When
        val result = useCase.execute(progressDate, examDate)

        // Then: 각 날짜가 정확히 표준 간격을 따르는지 검증
        expectedIntervals.forEachIndexed { index, interval ->
            val expectedDate = progressDate.plusDays(interval)
            assertEquals(
                "인덱스 $index: +${interval}일 간격이어야 한다",
                expectedDate,
                result[index]
            )
        }
    }

    @Test
    fun `표준 간격의 모든 복습일은 시험일 이전이어야 한다`() {
        // Given
        val progressDate = LocalDate.of(2026, 8, 1)
        val examDate = LocalDate.of(2026, 10, 1)

        // When
        val result = useCase.execute(progressDate, examDate)

        // Then: 모든 날짜가 examDate 이전(또는 당일)
        result.forEach { reviewDate ->
            assertTrue(
                "복습일 $reviewDate 은 시험일 $examDate 을 초과할 수 없다",
                !reviewDate.isAfter(examDate)
            )
        }
    }

    // ─────────────────────────────────────────────────────────
    // 테스트 2: 압축 간격 적용 (시험일까지 14일 미만)
    // ─────────────────────────────────────────────────────────

    @Test
    fun `시험일까지 7일 남은 경우 압축 간격으로 복습 일정이 생성된다`() {
        // Given
        val progressDate = LocalDate.of(2026, 8, 14)
        val examDate = LocalDate.of(2026, 8, 21) // 7일 차이

        // When
        val result = useCase.execute(progressDate, examDate)

        // Then: 리스트가 비어있지 않아야 함
        assertFalse("압축 간격에서도 복습 일정은 비어있으면 안 된다", result.isEmpty())
    }

    @Test
    fun `압축 간격의 모든 복습일은 시험일을 초과하지 않는다`() {
        // Given
        val progressDate = LocalDate.of(2026, 8, 14)
        val examDate = LocalDate.of(2026, 8, 21)

        // When
        val result = useCase.execute(progressDate, examDate)

        // Then: 모든 복습일이 시험일 이전
        result.forEach { reviewDate ->
            assertTrue(
                "압축 복습일 $reviewDate 은 시험일 $examDate 을 초과할 수 없다",
                !reviewDate.isAfter(examDate)
            )
        }
    }

    @Test
    fun `압축 간격의 각 복습일 간격은 최소 1일 최대 4일 이내여야 한다`() {
        // Given
        val progressDate = LocalDate.of(2026, 8, 14)
        val examDate = LocalDate.of(2026, 8, 21)

        // When
        val result = useCase.execute(progressDate, examDate)

        // Then: 연속된 날짜 간의 간격이 1~4일 범위인지 검증
        val allDates = listOf(progressDate) + result
        for (i in 1 until allDates.size) {
            val gap = ChronoUnit.DAYS.between(allDates[i - 1], allDates[i])
            assertTrue(
                "압축 간격은 최소 1일 이상이어야 한다 (실제: ${gap}일)",
                gap >= 1
            )
            assertTrue(
                "압축 간격은 최대 4일 이내여야 한다 (실제: ${gap}일)",
                gap <= 4
            )
        }
    }

    @Test
    fun `압축 간격은 표준 간격보다 짧게 조정된다`() {
        // Given
        val progressDate = LocalDate.of(2026, 8, 14)
        val examDate = LocalDate.of(2026, 8, 21) // 7일

        // When
        val result = useCase.execute(progressDate, examDate)

        // Then: 압축 간격의 마지막 날짜가 표준 마지막 간격(+30일)보다 훨씬 이전
        val standardLastDate = progressDate.plusDays(30)
        assertTrue(
            "압축 모드의 마지막 복습일은 표준 +30일보다 이전이어야 한다",
            result.last().isBefore(standardLastDate)
        )
    }

    // ─────────────────────────────────────────────────────────
    // 테스트 3: 예외 케이스
    // ─────────────────────────────────────────────────────────

    @Test
    fun `진도 작성일이 시험일 이후이면 예외가 발생한다`() {
        // Given
        val progressDate = LocalDate.of(2026, 9, 1) // 시험일보다 이후
        val examDate = LocalDate.of(2026, 8, 1)

        // When & Then
        try {
            useCase.execute(progressDate, examDate)
            fail("IllegalArgumentException 또는 InvalidScheduleException이 발생해야 한다")
        } catch (e: IllegalArgumentException) {
            // 통과: IllegalArgumentException 계열이면 OK
        }
    }

    @Test
    fun `진도 작성일이 시험일과 같으면 예외가 발생한다`() {
        // Given
        val sameDate = LocalDate.of(2026, 8, 20)

        // When & Then
        try {
            useCase.execute(sameDate, sameDate)
            fail("동일 날짜 입력 시 예외가 발생해야 한다")
        } catch (e: IllegalArgumentException) {
            // 통과
        }
    }

    @Test
    fun `작성일이 시험일 이후일 때 예외 메시지에 관련 문구가 포함된다`() {
        // Given
        val progressDate = LocalDate.of(2026, 9, 1)
        val examDate = LocalDate.of(2026, 8, 1)

        // When & Then
        try {
            useCase.execute(progressDate, examDate)
            fail("예외가 발생해야 한다")
        } catch (e: IllegalArgumentException) {
            val message = e.message?.lowercase() ?: ""
            val containsKeyword = message.contains("시험일") ||
                message.contains("invalid") ||
                message.contains("이후") ||
                message.contains("before")
            assertTrue(
                "예외 메시지에 '시험일 이후' 또는 'invalid' 관련 문구가 포함되어야 한다 (실제: '${e.message}')",
                containsKeyword
            )
        }
    }

    // ─────────────────────────────────────────────────────────
    // 테스트 4: 경계값 테스트 (추가 안전망)
    // ─────────────────────────────────────────────────────────

    @Test
    fun `시험일까지 정확히 30일이면 표준 간격이 적용된다`() {
        // Given
        val progressDate = LocalDate.of(2026, 8, 20)
        val examDate = progressDate.plusDays(30)

        // When
        val result = useCase.execute(progressDate, examDate)

        // Then: 표준 5개 간격 생성
        assertEquals("경계값 30일: 표준 5개 간격이어야 한다", 5, result.size)
    }

    @Test
    fun `시험일까지 1일만 남은 극단적 압축 상황에서도 최소 1개의 복습일이 생성된다`() {
        // Given
        val progressDate = LocalDate.of(2026, 8, 20)
        val examDate = progressDate.plusDays(1)

        // When
        val result = useCase.execute(progressDate, examDate)

        // Then: 최소 1개 이상
        assertTrue("극단적 압축에서도 최소 1개의 복습일이 있어야 한다", result.isNotEmpty())
    }
}
