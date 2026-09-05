// app/src/main/java/com/loorve/domain/repository/StudyRecordRepository.kt
package com.loorve.domain.repository

import com.loorve.domain.model.StudyRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

interface StudyRecordRepository {

    suspend fun saveStudyRecord(
        record: StudyRecord
    ): Result<String>

    suspend fun getStudyRecords(
        uid: String,
        blockId: String
    ): Result<List<StudyRecord>>

    suspend fun updateStudyRecord(
        record: StudyRecord
    ): Result<Unit>

    // ✅ [추가] 개별 학습기록 삭제
    suspend fun deleteStudyRecord(
        uid: String,
        record: StudyRecord
    ): Result<Unit>

    // ✅ [추가] 기간별 학습기록 조회 (홈 캘린더 dot 연동용)
    suspend fun getStudyRecordsByDateRange(
        uid: String,
        startDateMillis: Long,
        endDateMillis: Long
    ): Result<List<StudyRecord>> = Result.success(emptyList())

    // ✅ [추가] 사용자 전체 학습기록 조회
    suspend fun getAllStudyRecords(
        uid: String
    ): Result<List<StudyRecord>> = Result.success(emptyList())

    // ✅ [추가] 실시간 학습기록 관찰 (스냅샷 리스너)
    fun observeStudyRecords(
        uid: String
    ): Flow<List<StudyRecord>> = emptyFlow()

    // ✅ [추가] 특정 블록에 속한 모든 학습기록 일괄 삭제
    suspend fun deleteStudyRecordsByBlockId(
        uid: String,
        blockId: String
    ): Result<Unit> = Result.success(Unit)
}