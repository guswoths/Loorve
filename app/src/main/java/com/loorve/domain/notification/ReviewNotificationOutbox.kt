package com.loorve.domain.notification

import java.time.LocalDate

enum class ReviewNotificationKind {
    NORMAL_REVIEW,
    INCOMPLETE_REVIEW,
    FINAL_REVIEW_MORNING,
    FINAL_REVIEW_EVENING,
    CANCEL
}

data class ReviewNotificationOutbox(
    val id: String,
    val uid: String,
    val reviewId: String,
    val studyRecordId: String,
    val kind: ReviewNotificationKind,
    val scheduledDate: LocalDate,
    val triggerAtMillis: Long,
    val status: String = "PENDING"
)
