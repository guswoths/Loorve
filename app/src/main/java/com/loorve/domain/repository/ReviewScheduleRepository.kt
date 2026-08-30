package com.loorve.domain.repository

import com.loorve.domain.model.ReviewSchedule
import kotlinx.coroutines.flow.Flow

interface ReviewScheduleRepository {

    /**
     * 복습 일정을 저장합니다.
     *
     * reviewSchedule.userId가 비어 있으면 저장에 실패해야 합니다.
     */
    suspend fun saveReviewSchedule(
        reviewSchedule: ReviewSchedule
    ): Result<Unit>

    /**
     * 복습 일정을 저장합니다.
     *
     * 전달받은 uid를 Firestore 사용자 문서 경로와 ReviewSchedule.userId에 사용합니다.
     */
    suspend fun saveReviewSchedule(
        uid: String,
        reviewSchedule: ReviewSchedule
    ): Result<Unit>

    /**
     * 지정 기간에 포함된 복습 일정을 실시간으로 조회합니다.
     *
     * startDate와 endDate 형식: yyyy-MM-dd
     */
    fun getReviewSchedulesByDateRange(
        uid: String,
        startDate: String,
        endDate: String
    ): Flow<List<ReviewSchedule>>

    /**
     * 특정 진도에 연결된 모든 복습 일정을 조회합니다.
     *
     * 진도 삭제 전 연결된 알람 및 Firestore 일정 데이터를 정리할 때 사용합니다.
     */
    suspend fun getReviewSchedulesByProgressId(
        uid: String,
        progressId: String
    ): Result<List<ReviewSchedule>>

    /**
     * 지정 시점 이후의 미완료 복습 일정을 조회합니다.
     *
     * 앱 재부팅 후 알람 복원 및 로그아웃 전 예약 알람 취소에 사용합니다.
     */
    suspend fun getUpcomingIncompleteSchedules(
        uid: String,
        fromMillis: Long
    ): Result<List<ReviewSchedule>>

    /**
     * 복습 일정 한 건을 조회합니다.
     *
     * 문서가 존재하지 않으면 성공 결과의 값은 null입니다.
     */
    suspend fun getReviewScheduleById(
        uid: String,
        scheduleId: String
    ): Result<ReviewSchedule?>

    /**
     * 복습 일정을 완료 처리합니다.
     */
    suspend fun completeReviewSchedule(
        uid: String,
        scheduleId: String
    ): Result<Unit>

    /**
     * 복습 일정의 완료 여부를 변경합니다.
     */
    suspend fun updateReviewCompletion(
        uid: String,
        scheduleId: String,
        isCompleted: Boolean
    ): Result<Unit>

    /**
     * 복습 일정 한 건을 삭제합니다.
     */
    suspend fun deleteReviewSchedule(
        uid: String,
        scheduleId: String
    ): Result<Unit>
}