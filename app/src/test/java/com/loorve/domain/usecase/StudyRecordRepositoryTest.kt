package com.loorve.domain.usecase

import com.loorve.domain.model.StudyRecord
import com.loorve.domain.repository.StudyRecordRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class StudyRecordRepositoryTest {

    private lateinit var repository: StudyRecordRepository
    private val uid = "test-uid"
    private val seoulZone = ZoneId.of("Asia/Seoul")

    @Before
    fun setUp() {
        repository = mockk()
    }

    private fun makeStudyRecord(
        id: String = "record-001",
        learningDate: LocalDate = LocalDate.of(2026, 9, 5)
    ): StudyRecord {
        return StudyRecord(
            id = id,
            uid = uid,
            blockId = "block-001",
            examId = "exam-001",
            title = "1단원 복습",
            content = "1단원 개념 정리",
            learningDate = learningDate.atStartOfDay(seoulZone).toInstant().toEpochMilli(),
            examDate = learningDate.plusDays(30).atStartOfDay(seoulZone).toInstant().toEpochMilli()
        )
    }

    @Test
    fun `getStudyRecordsByDateRange() - 범위 내 학습기록 반환 성공`() = runTest {
        val startMillis = LocalDate.of(2026, 9, 1).atStartOfDay(seoulZone).toInstant().toEpochMilli()
        val endMillis = LocalDate.of(2026, 9, 30).atTime(23, 59, 59).atZone(seoulZone).toInstant().toEpochMilli()
        val records = listOf(makeStudyRecord())

        coEvery {
            repository.getStudyRecordsByDateRange(uid, startMillis, endMillis)
        } returns Result.success(records)

        val result = repository.getStudyRecordsByDateRange(uid, startMillis, endMillis)

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
        assertEquals("record-001", result.getOrNull()?.first()?.id)
        coVerify(exactly = 1) { repository.getStudyRecordsByDateRange(uid, startMillis, endMillis) }
    }

    @Test
    fun `getAllStudyRecords() - 사용자 전체 학습기록 반환 성공`() = runTest {
        val records = listOf(
            makeStudyRecord("rec-1", LocalDate.of(2026, 9, 1)),
            makeStudyRecord("rec-2", LocalDate.of(2026, 9, 5))
        )

        coEvery { repository.getAllStudyRecords(uid) } returns Result.success(records)

        val result = repository.getAllStudyRecords(uid)

        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull()?.size)
        coVerify(exactly = 1) { repository.getAllStudyRecords(uid) }
    }
}
