package com.loorve.data.notification

import com.loorve.domain.notification.ReviewNotificationAdapter
import com.loorve.domain.notification.ReviewNotificationPreferences
import com.loorve.domain.notification.ReviewNotificationOutbox
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmReviewNotificationAdapter @Inject constructor(
    private val alarmScheduler: ReviewAlarmScheduler
) : ReviewNotificationAdapter {
    override fun schedule(
        event: ReviewNotificationOutbox,
        preferences: ReviewNotificationPreferences
    ): Result<Unit> = runCatching {
        require(preferences.pushEnabled) { "복습 알림이 비활성화되어 있습니다." }
        when (alarmScheduler.scheduleReviewAlarm(event.id, event.triggerAtMillis)) {
            ReviewAlarmScheduler.ScheduleResult.FAILED ->
                error("복습 알림 예약에 실패했습니다.")
            else -> Unit
        }
    }

    override fun cancel(event: ReviewNotificationOutbox): Result<Unit> = runCatching {
        listOf(
            event.id,
            "${event.reviewId}_NORMAL_REVIEW",
            "${event.reviewId}_INCOMPLETE_REVIEW",
            "${event.reviewId}_FINAL_REVIEW_MORNING",
            "${event.reviewId}_FINAL_REVIEW_EVENING"
        ).distinct().forEach(alarmScheduler::cancelReviewAlarm)
    }
}
