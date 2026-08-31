package com.loorve.domain.repository

import com.loorve.domain.model.ReviewScheduleItem
import com.loorve.domain.model.StudyRecord

interface StudyRecordRepository {
    suspend fun saveStudyRecord(record: StudyRecord): Result<String>
    suspend fun getStudyRecords(uid: String, blockId: String): Result<List<StudyRecord>>
    suspend fun updateStudyRecord(record: StudyRecord): Result<Unit>
}

interface ReviewScheduleItemRepository {
    suspend fun saveSchedules(
        uid: String,
        studyRecordId: String,
        items: List<ReviewScheduleItem>
    ): Result<Unit>

    suspend fun getSchedulesByStudyRecord(
        uid: String,
        studyRecordId: String
    ): Result<List<ReviewScheduleItem>>

    suspend fun getPendingSchedulesByBlock(
        uid: String,
        blockId: String
    ): Result<List<ReviewScheduleItem>>

    suspend fun updateScheduleItem(
        uid: String,
        item: ReviewScheduleItem
    ): Result<Unit>

    /** transaction 기반 일괄 미완료 일정 업데이트 (rescale 용) */
    suspend fun batchUpdatePendingSchedules(
        uid: String,
        items: List<ReviewScheduleItem>
    ): Result<Unit>
}