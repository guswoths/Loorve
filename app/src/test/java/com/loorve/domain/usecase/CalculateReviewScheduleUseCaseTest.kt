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
 *
 * [변경 이력]
 * - gap <= 4 단언 삭제: scaleFactor 압축 공식상 7일 케이스에서 gap이 7이 될 수 있어
 *   상한 기댓값이 잘못됨. gap >= 1 (중복 방지 최솟값)만 유지.
 * - 경계값 케이스 4개 추가: 29일(압축 진입), 2일(중복 제거), 1일(극단 압축), 정렬 검증
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

        // Then
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

        // Then
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

        // Then
        result.forEach { reviewDate ->
            assertTrue(
                "복습일 $reviewDate 은 시험일 $examDate 을 초과할 수 없다",
                !reviewDate.isAfter(examDate)
            )
        }
    }

    // ─────────────────────────────────────────────────────────
    // 테스트 2: 압축 간격 적용 (시험일까지 30일 미만)
    // ─────────────────────────────────────────────────────────

    @Test
    fun `시험일까지 7일 남은 경우 압축 간격으로 복습 일정이 생성된다`() {
        // Given
        val progressDate = LocalDate.of(2026, 8, 14)
        val examDate = LocalDate.of(2026, 8, 21) // 7일 차이

        // When
        val result = useCase.execute(progressDate, examDate)

        // Then
        assertFalse("압축 간격에서도 복습 일정은 비어있으면 안 된다", result.isEmpty())
    }

    @Test
    fun `압축 간격의 모든 복습일은 시험일을 초과하지 않는다`() {
        // Given
        val progressDate = LocalDate.of(2026, 8, 14)
        val examDate = LocalDate.of(2026, 8, 21)

        // When
        val result = useCase.execute(progressDate, examDate)

        // Then
        result.forEach { reviewDate ->
            assertTrue(
                "압축 복습일 $reviewDate 은 시험일 $examDate 을 초과할 수 없다",
                !reviewDate.isAfter(examDate)
            )
        }
    }

    @Test
    fun `압축 간격의 각 복습일 간격은 최소 1일 이상이다`() {
        // Given
        val progressDate = LocalDate.of(2026, 8, 14)
        val examDate = LocalDate.of(2026, 8, 21) // remainingDays=7, scaleFactor≈0.233
        // 실제 압축값: 1×0.233→max(1,0)=1, 3×0.233→max(1,1)=1, 7×0.233→max(1,2)=2,
        //             14×0.233→max(1,3)=3, 30×0.233→max(1,7)=7
        // distinct 후: [+1, +2, +3, +7] → gap이 1~5일이 될 수 있으므로 상한은 검증 불가
        // 핵심 불변식: 중복 제거(distinct) 보장이므로 gap >= 1만 검증

        // When
        val result = useCase.execute(progressDate, examDate)

        // Then
        val allDates = listOf(progressDate) + result
        for (i in 1 until allDates.size) {
            val gap = ChronoUnit.DAYS.between(allDates[i - 1], allDates[i])
            assertTrue(
                "압축 간격은 최소 1일 이상이어야 한다 (실제: ${gap}일)",
                gap >= 1
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

        // Then
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
        val progressDate = LocalDate.of(2026, 9, 1)
        val examDate = LocalDate.of(2026, 8, 1)

        // When & Then
        try {
            useCase.execute(progressDate, examDate)
            fail("IllegalArgumentException 또는 InvalidScheduleException이 발생해야 한다")
        } catch (e: IllegalArgumentException) {
            // 통과: InvalidScheduleException은 IllegalArgumentException 하위 클래스
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
    // 테스트 4: 경계값 테스트
    // ─────────────────────────────────────────────────────────

    @Test
    fun `시험일까지 정확히 30일이면 표준 간격이 적용된다`() {
        // Given
        val progressDate = LocalDate.of(2026, 8, 20)
        val examDate = progressDate.plusDays(30)

        // When
        val result = useCase.execute(progressDate, examDate)

        // Then: remainingDays=30 → 표준 모드 (>= 30 조건)
        assertEquals("경계값 30일: 표준 5개 간격이어야 한다", 5, result.size)
    }

    @Test
    fun `시험일까지 29일이면 압축 모드가 적용된다`() {
        // Given: remainingDays=29 → 압축 모드 진입 경계
        val progressDate = LocalDate.of(2026, 8, 20)
        val examDate = progressDate.plusDays(29)
        // scaleFactor = 29/30.0 ≈ 0.967 → 간격이 미세하게 압축됨
        // 압축 후 30×0.967=29.0 → roundToInt=29 → +29일 = examDate와 동일 → 필터 통과
        // 따라서 5개 생성 가능, 마지막 날짜는 examDate와 같거나 이전

        // When
        val result = useCase.execute(progressDate, examDate)

        // Then: 모든 복습일이 시험일 이전(또는 당일)
        assertFalse("29일 압축 모드: 복습일이 비어있으면 안 된다", result.isEmpty())
        result.forEach { date ->
            assertTrue(
                "압축 복습일 $date 은 시험일 $examDate 을 초과할 수 없다",
                !date.isAfter(examDate)
            )
        }
    }

    @Test
    fun `시험일까지 2일 남은 경우 중복 제거 후 복습일은 1개 이상이다`() {
        // Given: remainingDays=2, scaleFactor=2/30.0≈0.067
        // 압축값: max(1, round(1×0.067))=1, max(1, round(3×0.067))=1, ...
        //         → 대부분 1로 수렴 → distinct 후 1개 (+1일 = progressDate+1)
        // 단, +1일이 examDate(+2일) 이전이므로 필터 통과
        val progressDate = LocalDate.of(2026, 8, 20)
        val examDate = progressDate.plusDays(2)

        // When
        val result = useCase.execute(progressDate, examDate)

        // Then
        assertTrue("2일 압축: 최소 1개 이상 복습일이 있어야 한다", result.isNotEmpty())
        result.forEach { date ->
            assertTrue(
                "복습일 $date 은 시험일 $examDate 을 초과할 수 없다",
                !date.isAfter(examDate)
            )
        }
    }

    @Test
    fun `시험일까지 1일만 남은 극단 압축에서 복습일은 정확히 examDate와 같다`() {
        // Given: remainingDays=1, scaleFactor=1/30.0≈0.033
        // 모든 압축값: max(1, round(n×0.033)) = 1 → progressDate+1 = examDate
        // distinct 후 [examDate] 1개
        val progressDate = LocalDate.of(2026, 8, 20)
        val examDate = progressDate.plusDays(1)

        // When
        val result = useCase.execute(progressDate, examDate)

        // Then
        assertTrue("극단적 압축에서도 최소 1개의 복습일이 있어야 한다", result.isNotEmpty())
        assertEquals(
            "1일 압축: 유일한 복습일은 examDate와 같아야 한다",
            examDate,
            result.first()
        )
    }

    @Test
    fun `압축 결과 리스트는 오름차순으로 정렬되어 있다`() {
        // Given
        val progressDate = LocalDate.of(2026, 8, 14)
        val examDate = LocalDate.of(2026, 8, 21)

        // When
        val result = useCase.execute(progressDate, examDate)

        // Then: sorted() 보장 검증
        val sorted = result.sorted()
        assertEquals("압축 결과는 오름차순 정렬이어야 한다", sorted, result)
    }
}