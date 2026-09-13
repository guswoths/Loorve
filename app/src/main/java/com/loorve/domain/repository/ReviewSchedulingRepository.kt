package com.loorve.domain.repository

import com.loorve.domain.model.ReviewScheduleItem
import com.loorve.domain.model.StudyRecord
import com.loorve.domain.notification.ReviewNotificationOutbox

interface ReviewSchedulingRepository {
    suspend fun saveStudyRecordWithSchedules(
        uid: String,
        record: StudyRecord,
        schedules: List<ReviewScheduleItem>,
        notifications: List<ReviewNotificationOutbox>
    ): Result<Unit>

    suspend fun completeReviewAndUpdateSchedules(
        uid: String,
        completed: ReviewScheduleItem,
        changedFutureSchedules: List<ReviewScheduleItem>,
        notifications: List<ReviewNotificationOutbox>
    ): Result<Unit>
}
