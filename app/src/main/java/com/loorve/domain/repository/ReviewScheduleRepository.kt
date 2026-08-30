package com.loorve.domain.repository

import com.loorve.domain.model.ReviewSchedule
import kotlinx.coroutines.flow.Flow

interface ReviewScheduleRepository {

    suspend fun saveReviewSchedule(
        reviewSchedule: ReviewSchedule
    ): Result<Unit>

    // ← 추가: uid + schedule 오버로드 (SaveProgressAndScheduleUseCase 지원)
    suspend fun saveReviewSchedule(
        uid: String,
        reviewSchedule: ReviewSchedule
    ): Result<Unit>

    fun getReviewSchedulesByDateRange(
        uid: String,
        startDate: String,
        endDate: String
    ): Flow<List<ReviewSchedule>>

    // ← 추가: SignOutUseCase / BootCompletedReceiver 지원
    suspend fun getUpcomingIncompleteSchedules(
        uid: String,
        fromMillis: Long
    ): Result<List<ReviewSchedule>>

    // ← 추가: UpdateReviewCompletionUseCase 지원
    suspend fun getReviewScheduleById(
        uid: String,
        scheduleId: String
    ): Result<ReviewSchedule?>

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