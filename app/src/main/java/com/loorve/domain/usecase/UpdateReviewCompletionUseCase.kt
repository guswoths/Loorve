package com.loorve.domain.usecase

import android.util.Log
import com.loorve.data.notification.ReviewAlarmScheduler
import com.loorve.domain.repository.ReviewScheduleRepository
import javax.inject.Inject

class UpdateReviewCompletionUseCase @Inject constructor(
    private val repository: ReviewScheduleRepository,
    private val alarmScheduler: ReviewAlarmScheduler
) {

    companion object {
        private const val TAG = "UpdateReviewCompletion"
    }

    suspend operator fun invoke(
        uid: String,
        scheduleId: String,
        isCompleted: Boolean
    ): Result<Unit> {
        require(uid.isNotBlank()) { "uid는 비어 있을 수 없습니다." }
        require(scheduleId.isNotBlank()) { "scheduleId는 비어 있을 수 없습니다." }

        val result = repository.updateReviewCompletion(uid, scheduleId, isCompleted)
        if (result.isFailure) return result

        if (isCompleted) {
            alarmScheduler.cancelReviewAlarm(scheduleId)
            Log.d(TAG, "알람 취소 완료: scheduleId=$scheduleId")
        } else {
            // ✅ 수정: 추가된 인터페이스 메서드 사용
            val scheduleResult = repository.getReviewScheduleById(uid, scheduleId)
            if (scheduleResult.isFailure) {
                Log.w(TAG, "알람 재예약 실패 — 스케줄 조회 오류: ${scheduleResult.exceptionOrNull()?.message}")
                return result
            }
            val schedule = scheduleResult.getOrNull() ?: run {
                Log.w(TAG, "알람 재예약 실패 — 스케줄 없음: scheduleId=$scheduleId")
                return result
            }
            // ✅ 수정: ReviewSchedule.reviewDate 필드는 Long으로 동일하게 존재
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