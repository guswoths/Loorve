package com.loorve.domain.usecase

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class ForgettingCurveSchedulerTest {

    private val studyDate = LocalDate.of(2026, 8, 20)

    @Test
    fun `1일차 복습일 계산 - studyDate+1`() {
        assertEquals(studyDate.plusDays(1), ForgettingCurveScheduler.generateReviewDates(studyDate)[0])
    }

    @Test
    fun `3일차 복습일 계산 - studyDate+3`() {
        assertEquals(studyDate.plusDays(3), ForgettingCurveScheduler.generateReviewDates(studyDate)[1])
    }

    @Test
    fun `7일차 복습일 계산 - studyDate+7`() {
        assertEquals(studyDate.plusDays(7), ForgettingCurveScheduler.generateReviewDates(studyDate)[2])
    }

    @Test
    fun `14일차 복습일 계산 - studyDate+14`() {
        assertEquals(studyDate.plusDays(14), ForgettingCurveScheduler.generateReviewDates(studyDate)[3])
    }

    @Test
    fun `30일차 복습일 계산 - studyDate+30`() {
        assertEquals(studyDate.plusDays(30), ForgettingCurveScheduler.generateReviewDates(studyDate)[4])
    }

    @Test
    fun `전체 5단계 복습 스케줄 리스트 반환 확인`() {
        val dates = ForgettingCurveScheduler.generateReviewDates(studyDate)
        assertEquals(5, dates.size)
        assertEquals(listOf(1, 3, 7, 14, 30).map { studyDate.plusDays(it.toLong()) }, dates)
    }

    @Test
    fun `복습 완료 시 다음 단계 intervalDays 반환`() {
        // round=1 완료 → 2회차 간격(3) 반환
        assertEquals(3, ForgettingCurveScheduler.nextInterval(1, isCompleted = true))
        assertEquals(7, ForgettingCurveScheduler.nextInterval(2, isCompleted = true))
        assertEquals(14, ForgettingCurveScheduler.nextInterval(3, isCompleted = true))
        assertEquals(30, ForgettingCurveScheduler.nextInterval(4, isCompleted = true))
    }

    @Test
    fun `5단계 이후 완료 시 null 반환 (복습 종료)`() {
        assertNull(ForgettingCurveScheduler.nextInterval(5, isCompleted = true))
    }

    @Test
    fun `복습 미완료 시 동일 intervalDays 재반환`() {
        assertEquals(1, ForgettingCurveScheduler.nextInterval(1, isCompleted = false))
        assertEquals(7, ForgettingCurveScheduler.nextInterval(3, isCompleted = false))
        assertEquals(30, ForgettingCurveScheduler.nextInterval(5, isCompleted = false))
    }

    @Test
    fun `getIntervalForRound - 유효 범위(1~5) 정확히 반환`() {
        assertEquals(1,  ForgettingCurveScheduler.getIntervalForRound(1))
        assertEquals(3,  ForgettingCurveScheduler.getIntervalForRound(2))
        assertEquals(7,  ForgettingCurveScheduler.getIntervalForRound(3))
        assertEquals(14, ForgettingCurveScheduler.getIntervalForRound(4))
        assertEquals(30, ForgettingCurveScheduler.getIntervalForRound(5))
    }

    @Test
    fun `getIntervalForRound - 범위 초과(0, 6)는 null 반환`() {
        assertNull(ForgettingCurveScheduler.getIntervalForRound(0))
        assertNull(ForgettingCurveScheduler.getIntervalForRound(6))
    }

    @Test
    fun `nextInterval - round=0 경계 입력 시 null 반환`() {
        // round=0은 INTERVALS[-1]에 해당하므로 null이어야 한다
        assertNull(ForgettingCurveScheduler.nextInterval(0, isCompleted = true))
        assertNull(ForgettingCurveScheduler.nextInterval(0, isCompleted = false))
    }
}
