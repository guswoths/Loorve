// app/src/main/java/com/loorve/domain/repository/StudyRecordRepository.kt
package com.loorve.domain.repository

import com.loorve.domain.model.StudyRecord

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
}