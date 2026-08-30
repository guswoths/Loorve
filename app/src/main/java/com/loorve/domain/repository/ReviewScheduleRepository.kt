package com.loorve.domain.repository

import com.loorve.domain.model.ReviewSchedule
import kotlinx.coroutines.flow.Flow

interface ReviewScheduleRepository {

    suspend fun saveReviewSchedule(
        reviewSchedule: ReviewSchedule
    ): Result<Unit>

    fun getReviewSchedulesByDateRange(
        uid: String,
        startDate: String,
        endDate: String
    ): Flow<List<ReviewSchedule>>

    suspend fun completeReviewSchedule(
        uid: String,
        scheduleId: String
    ): Result<Unit>

    suspend fun updateReviewCompletion(
        uid: String,
        scheduleId: String,
        isCompleted: Boolean
    ): Result<Unit>

    suspend fun deleteReviewSchedule(
        uid: String,
        scheduleId: String
    ): Result<Unit>
}