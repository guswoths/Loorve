package com.loorve.data.model

import com.google.firebase.Timestamp
import com.loorve.domain.model.CompletionResult
import com.loorve.domain.model.ReviewScheduleItem
import com.loorve.domain.model.ReviewStatus

data class ReviewScheduleItemDto(
    val id: String = "",
    val studyRecordId: String = "",
    val blockId: String = "",
    val uid: String = "",
    val title: String = "",
    val reviewDate: Long = 0L,
    val originalReviewDate: Long = 0L,
    val stage: Int = 0,
    val reviewOrder: Int = 0,
    val status: String = "PENDING",
    val previousGapDays: Long = 1L,
    val overdueDays: Long = 0L,
    val compressedReview: Boolean = false,
    val completionResult: String? = null,
    val completedAt: Long? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toDomain(): ReviewScheduleItem = ReviewScheduleItem(
        id = id, studyRecordId = studyRecordId, blockId = blockId,
        uid = uid, title = title,
        reviewDate = reviewDate, originalReviewDate = originalReviewDate,
        stage = stage, reviewOrder = reviewOrder,
        status = runCatching { ReviewStatus.valueOf(status) }.getOrDefault(ReviewStatus.PENDING),
        previousGapDays = previousGapDays,
        overdueDays = overdueDays,
        compressedReview = compressedReview,
        completionResult = completionResult?.let {
            runCatching { CompletionResult.valueOf(it) }.getOrNull()
        },
        completedAt = completedAt,
        createdAt = createdAt?.toDate()?.time ?: 0L,
        updatedAt = updatedAt?.toDate()?.time ?: 0L
    )
}