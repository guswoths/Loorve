package com.loorve.domain.model

enum class ReviewStatus { PENDING, COMPLETED, OVERDUE, FINAL_URGENT_REVIEW }
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
    val completionResult: CompletionResult? = null,
    val completedAt: Long? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)