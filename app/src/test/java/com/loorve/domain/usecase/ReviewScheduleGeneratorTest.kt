package com.loorve.domain.usecase

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class ReviewScheduleGeneratorTest {

    private val generator = ReviewScheduleGenerator()
    private val studyDate = LocalDate.of(2026, 8, 20)
    private val progressId = "progress-test-id"
    private val KST = ZoneId.of("Asia/Seoul")

    @Test
    fun `진도 입력(studyDate) 시 5개 ReviewSchedule 객체 생성`() {
        val schedules = generator.generate(progressId, studyDate)
        assertEquals(5, schedules.size)
    }

    @Test
    fun `생성된 ReviewSchedule의 reviewDate 정확성 검증`() {
        val schedules = generator.generate(progressId, studyDate)
        val expectedIntervals = listOf(1, 3, 7, 14, 30)
        schedules.forEachIndexed { index, schedule ->
            val expectedEpochMs = studyDate.plusDays(expectedIntervals[index].toLong())
                .atStartOfDay(KST).toInstant().toEpochMilli()
            assertEquals(expectedEpochMs, schedule.reviewDate)
        }
    }

    @Test
    fun `동일 studyDate 중복 입력 시 스케줄 덮어쓰기 또는 병합 정책 확인`() {
        // Generator 자체는 UUID를 새로 생성하므로 항상 신규 5개 반환
        // 중복 병합 정책은 Repository 레이어에서 처리 — 여기서는 항상 5개임을 확인
        val schedules1 = generator.generate(progressId, studyDate)
        val schedules2 = generator.generate(progressId, studyDate)
        assertEquals(5, schedules1.size)
        assertEquals(5, schedules2.size)
        // UUID가 다르므로 ID는 달라야 함
        assertNotEquals(schedules1[0].reviewScheduleId, schedules2[0].reviewScheduleId)
    }

    @Test
    fun `originProgressId 연결 정확성 검증`() {
        val schedules = generator.generate(progressId, studyDate)
        schedules.forEach { schedule ->
            assertEquals(progressId, schedule.originProgressId)
        }
    }

    @Test
    fun `reviewRound 1부터 5까지 순서대로 설정`() {
        val schedules = generator.generate(progressId, studyDate)
        schedules.forEachIndexed { index, schedule ->
            assertEquals(index + 1, schedule.reviewRound)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `빈 originProgressId 입력 시 예외 발생`() {
        generator.generate("", studyDate)
    }

    @Test
    fun `생성된 ReviewSchedule의 isCompleted 초기값은 false이다`() {
        val schedules = generator.generate(progressId, studyDate)
        schedules.forEach { schedule ->
            assertFalse("초기 isCompleted는 false여야 한다", schedule.isCompleted)
        }
    }

    @Test
    fun `생성 직후 createdAt과 updatedAt은 동일하다`() {
        val schedules = generator.generate(progressId, studyDate)
        schedules.forEach { schedule ->
            assertEquals("초기 createdAt == updatedAt이어야 한다",
                schedule.createdAt, schedule.updatedAt)
        }
    }
}
