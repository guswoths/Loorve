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
}