// app/src/main/java/com/loorve/domain/repository/ReviewScheduleItemRepository.kt
package com.loorve.domain.repository

import com.loorve.domain.model.ReviewScheduleItem
import kotlinx.coroutines.flow.Flow

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

    suspend fun getAllScheduleItems(
        uid: String
    ): Result<List<ReviewScheduleItem>>

    suspend fun updateScheduleItem(
        uid: String,
        item: ReviewScheduleItem
    ): Result<Unit>

    suspend fun updateScheduleCompletion(
        uid: String,
        scheduleId: String,
        isCompleted: Boolean
    ): Result<Unit>

    suspend fun batchUpdatePendingSchedules(
        uid: String,
        items: List<ReviewScheduleItem>
    ): Result<Unit>

    fun observeReviewScheduleItems(
        uid: String
    ): Flow<List<ReviewScheduleItem>>
}