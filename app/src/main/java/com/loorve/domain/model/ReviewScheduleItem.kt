package com.loorve.domain.model

import com.loorve.domain.review.ReviewPlanStatus

enum class ReviewStatus {
    PENDING, COMPLETED, OVERDUE, FINAL_URGENT_REVIEW,
    CRAM_MODE_REQUIRED, OVERLOADED_UNRESOLVED
}
enum class CompletionResult { REMEMBERED, FORGOT }

data class ReviewScheduleItem(
    val id: String = "",
    val studyRecordId: String = "",
    val blockId: String = "",
    val uid: String = "",
    val title: String = "",
    val reviewDate: Long = 0L,
    val originalReviewDate: Long = 0L,
    val stage: Int = 0,
    val reviewOrder: Int = 0,
    val status: ReviewStatus = ReviewStatus.PENDING,
    val previousGapDays: Long = 1L,
    val overdueDays: Long = 0L,
    val compressedReview: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val completionResult: CompletionResult? = null,
    val completedAt: Long? = null,
    val customAlarmTime: Pair<Int, Int>? = null,
    val planStatus: ReviewPlanStatus = ReviewPlanStatus.SCHEDULED,
    val priorityScore: Double = 0.0,
    val estimatedReviewMinutes: Int = 15,
    val recommendedMethod: String = "",
    val isFinalReview: Boolean = false,
    val rescheduleReason: String? = null,
    val outcome: String? = null
)