// AFTER — 전체 코드
package com.loorve.domain.usecase

import android.util.Log
import com.loorve.data.notification.ReviewAlarmScheduler
import com.loorve.domain.repository.ReviewScheduleRepository
import javax.inject.Inject

/**
 * 복습 일정의 완료 여부를 토글(업데이트)하고 알람을 연동하는 UseCase.
 */
class UpdateReviewCompletionUseCase @Inject constructor(
    private val repository: ReviewScheduleRepository,
    private val alarmScheduler: ReviewAlarmScheduler          // ← 추가
) {

    companion object {
        private const val TAG = "UpdateReviewCompletion"
    }

    /**
     * @param uid         현재 인증된 사용자 UID
     * @param scheduleId  변경할 복습 일정 ID
     * @param isCompleted 변경할 완료 여부
     */
    suspend operator fun invoke(
        uid: String,
        scheduleId: String,
        isCompleted: Boolean
    ): Result<Unit> {
        require(uid.isNotBlank()) { "uid는 비어 있을 수 없습니다." }
        require(scheduleId.isNotBlank()) { "scheduleId는 비어 있을 수 없습니다." }

        // 1. DB 업데이트
        val result = repository.updateReviewCompletion(uid, scheduleId, isCompleted)
        if (result.isFailure) return result

        // 2. 알람 처리
        if (isCompleted) {
            // 완료 → 알람 취소
            alarmScheduler.cancelReviewAlarm(scheduleId)
            Log.d(TAG, "알람 취소 완료: scheduleId=$scheduleId")
        } else {
            // 미완료 복원 → reviewDate 조회 후 재예약
            val scheduleResult = repository.getReviewScheduleById(uid, scheduleId)
            if (scheduleResult.isFailure) {
                Log.w(TAG, "알람 재예약 실패 — 스케줄 조회 오류: ${scheduleResult.exceptionOrNull()?.message}")
                return result  // DB 업데이트는 성공했으므로 success 유지
            }
            val schedule = scheduleResult.getOrNull() ?: run {
                Log.w(TAG, "알람 재예약 실패 — 스케줄 없음: scheduleId=$scheduleId")
                return result
            }
            val alarmResult = alarmScheduler.scheduleReviewAlarm(
                reviewScheduleId = scheduleId,
                triggerAtMillis  = schedule.reviewDate
            )
            when (alarmResult) {
                ReviewAlarmScheduler.ScheduleResult.FAILED ->
                    Log.w(TAG, "알람 재예약 실패 (FAILED): scheduleId=$scheduleId")
                ReviewAlarmScheduler.ScheduleResult.FALLBACK_INEXACT ->
                    Log.w(TAG, "알람 재예약 비정확 폴백 (FALLBACK_INEXACT): scheduleId=$scheduleId")
                ReviewAlarmScheduler.ScheduleResult.EXACT ->
                    Log.d(TAG, "알람 재예약 완료: scheduleId=$scheduleId")
            }
        }

        return result
    }
}