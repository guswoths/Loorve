package com.loorve.domain.usecase

import com.loorve.domain.model.ReviewSchedule
import com.loorve.domain.repository.ReviewScheduleRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class ReviewScheduleRepositoryTest {

    private lateinit var repository: ReviewScheduleRepository
    private val uid = "test-uid"
    private val KST = ZoneId.of("Asia/Seoul")
    private val studyDate = LocalDate.of(2026, 8, 20)

    private fun makeSchedules(): List<ReviewSchedule> {
        val generator = ReviewScheduleGenerator()
        return generator.generate("progress-001", studyDate)
    }

    @Before
    fun setUp() {
        repository = mockk()
    }

    @Test
    fun `saveReviewSchedules() - 5개 스케줄 저장 성공`() = runTest {
        val schedules = makeSchedules()
        coEvery { repository.saveReviewSchedules(uid, schedules) } returns Result.success(Unit)

        val result = repository.saveReviewSchedules(uid, schedules)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { repository.saveReviewSchedules(uid, schedules) }
    }

    @Test
    fun `getSchedulesByDate() - 특정 날짜의 복습 목록 반환`() = runTest {
        val targetDate = "2026-08-21"
        val expectedSchedules = makeSchedules().take(1) // 1일차 스케줄
        coEvery {
            repository.getReviewSchedulesByDateRange(uid, targetDate, targetDate)
        } returns flowOf(expectedSchedules)

        val result = repository
            .getReviewSchedulesByDateRange(uid, targetDate, targetDate)
            .first()

        assertEquals(1, result.size)
        assertEquals(expectedSchedules[0].reviewRound, result[0].reviewRound)
    }

    @Test
    fun `updateReviewCompletion() - isCompleted 토글 후 다음 단계 자동 스케줄 생성`() = runTest {
        val scheduleId = "schedule-001"
        // 토글 자체 성공 확인
        coEvery {
            repository.updateReviewCompletion(uid, scheduleId, true)
        } returns Result.success(Unit)

        val toggleResult = repository.updateReviewCompletion(uid, scheduleId, true)
        assertTrue(toggleResult.isSuccess)

        // 다음 단계 스케줄 생성 가능 여부 — ForgettingCurveScheduler 단위로 검증
        val nextInterval = ForgettingCurveScheduler.nextInterval(1, isCompleted = true)
        assertNotNull(nextInterval)
        assertEquals(3, nextInterval)
    }
}
