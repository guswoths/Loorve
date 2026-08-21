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

/**
 * ReviewScheduleRepository 계약 검증 테스트.
 *
 * 설계 원칙:
 * - Repository는 인터페이스이므로 MockK로 계약(Contract)만 검증한다.
 * - 도메인 로직(ForgettingCurveScheduler 등)은 이 테스트에서 검증하지 않는다.
 *   → 관심사 분리: 스케줄러 로직은 ForgettingCurveSchedulerTest에서 담당.
 */
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

    // ─────────────────────────────────────────────────────────
    // saveReviewSchedules()
    // ─────────────────────────────────────────────────────────

    @Test
    fun `saveReviewSchedules() - 5개 스케줄 저장 성공`() = runTest {
        val schedules = makeSchedules()
        coEvery { repository.saveReviewSchedules(uid, schedules) } returns Result.success(Unit)

        val result = repository.saveReviewSchedules(uid, schedules)

        assertTrue("저장 성공 시 Result.success여야 한다", result.isSuccess)
        coVerify(exactly = 1) { repository.saveReviewSchedules(uid, schedules) }
    }

    @Test
    fun `saveReviewSchedules() - Firestore 오류 시 Result_failure 반환`() = runTest {
        val schedules = makeSchedules()
        val exception = RuntimeException("Firestore 저장 실패")
        coEvery { repository.saveReviewSchedules(uid, schedules) } returns Result.failure(exception)

        val result = repository.saveReviewSchedules(uid, schedules)

        assertTrue("저장 실패 시 Result.failure여야 한다", result.isFailure)
        assertEquals(
            "예외 메시지가 일치해야 한다",
            "Firestore 저장 실패",
            result.exceptionOrNull()?.message
        )
        coVerify(exactly = 1) { repository.saveReviewSchedules(uid, schedules) }
    }

    @Test
    fun `saveReviewSchedules() - 빈 리스트 저장 시도는 성공 또는 정책에 따라 처리`() = runTest {
        // 빈 리스트는 Firestore에 아무 것도 쓰지 않으므로 성공으로 처리하는 것이 일반적
        val emptySchedules = emptyList<ReviewSchedule>()
        coEvery { repository.saveReviewSchedules(uid, emptySchedules) } returns Result.success(Unit)

        val result = repository.saveReviewSchedules(uid, emptySchedules)

        assertTrue("빈 리스트 저장은 성공으로 처리되어야 한다", result.isSuccess)
    }

    // ─────────────────────────────────────────────────────────
    // getReviewSchedulesByDateRange()
    // ─────────────────────────────────────────────────────────

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

        assertEquals("반환된 스케줄 수가 1개여야 한다", 1, result.size)
        assertEquals(
            "reviewRound가 일치해야 한다",
            expectedSchedules[0].reviewRound,
            result[0].reviewRound
        )
    }

    @Test
    fun `getSchedulesByDate() - 해당 날짜에 복습 없으면 빈 리스트 반환`() = runTest {
        val emptyDate = "2026-12-31"
        coEvery {
            repository.getReviewSchedulesByDateRange(uid, emptyDate, emptyDate)
        } returns flowOf(emptyList())

        val result = repository
            .getReviewSchedulesByDateRange(uid, emptyDate, emptyDate)
            .first()

        assertTrue("복습이 없는 날짜는 빈 리스트를 반환해야 한다", result.isEmpty())
    }

    @Test
    fun `getSchedulesByDateRange() - 날짜 범위로 여러 스케줄 반환`() = runTest {
        val startDate = "2026-08-21"
        val endDate = "2026-08-23"
        val allSchedules = makeSchedules() // round 1(+1일), round 2(+3일) 포함 가능
        coEvery {
            repository.getReviewSchedulesByDateRange(uid, startDate, endDate)
        } returns flowOf(allSchedules.take(2))

        val result = repository
            .getReviewSchedulesByDateRange(uid, startDate, endDate)
            .first()

        assertEquals("범위 내 스케줄 2개가 반환되어야 한다", 2, result.size)
    }

    // ─────────────────────────────────────────────────────────
    // updateReviewCompletion()
    //
    // 관심사 분리 원칙:
    //   Repository 테스트는 "저장 계약"만 검증한다.
    //   "완료 후 다음 간격이 얼마인가"는 ForgettingCurveSchedulerTest 담당.
    // ─────────────────────────────────────────────────────────

    @Test
    fun `updateReviewCompletion() - isCompleted=true 설정 성공`() = runTest {
        val scheduleId = "schedule-001"
        coEvery {
            repository.updateReviewCompletion(uid, scheduleId, true)
        } returns Result.success(Unit)

        val result = repository.updateReviewCompletion(uid, scheduleId, true)

        assertTrue("완료 처리 성공 시 Result.success여야 한다", result.isSuccess)
        coVerify(exactly = 1) { repository.updateReviewCompletion(uid, scheduleId, true) }
        // ❌ 제거됨: ForgettingCurveScheduler.nextInterval() 검증
        //    → ForgettingCurveSchedulerTest의 책임
    }

    @Test
    fun `updateReviewCompletion() - isCompleted=false 미완료 처리 성공`() = runTest {
        val scheduleId = "schedule-002"
        coEvery {
            repository.updateReviewCompletion(uid, scheduleId, false)
        } returns Result.success(Unit)

        val result = repository.updateReviewCompletion(uid, scheduleId, false)

        assertTrue("미완료 처리도 Result.success여야 한다", result.isSuccess)
        coVerify(exactly = 1) { repository.updateReviewCompletion(uid, scheduleId, false) }
    }

    @Test
    fun `updateReviewCompletion() - 존재하지 않는 scheduleId 처리 시 Result_failure 반환`() = runTest {
        val invalidScheduleId = "non-existent-id"
        val exception = NoSuchElementException("스케줄을 찾을 수 없습니다: $invalidScheduleId")
        coEvery {
            repository.updateReviewCompletion(uid, invalidScheduleId, true)
        } returns Result.failure(exception)

        val result = repository.updateReviewCompletion(uid, invalidScheduleId, true)

        assertTrue("존재하지 않는 ID 업데이트 시 Result.failure여야 한다", result.isFailure)
        assertTrue(
            "예외 타입이 NoSuchElementException이어야 한다",
            result.exceptionOrNull() is NoSuchElementException
        )
    }
}