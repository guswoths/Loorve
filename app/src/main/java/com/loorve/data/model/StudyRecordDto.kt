package com.loorve.data.model

import com.google.firebase.Timestamp
import com.loorve.domain.model.StudyRecord
import com.loorve.domain.review.ReviewDifficulty
import com.loorve.domain.review.ReviewImportance

data class StudyRecordDto(
    val id: String = "",
    val uid: String = "",
    val blockId: String = "",
    val examId: String = "",
    val title: String = "",
    val content: String = "",
    val learningDate: Long = 0L,
    val examDate: Long = 0L,
    val prepStartDate: Long = 0L,
    val recommendedCompletionDate: Long = 0L,
    val stage: Int = 0,
    val successCount: Int = 0,
    val stability: Double = 1.0,
    val completionRate: Double = 0.0,
    val plannedReviewCount: Int = 0,
    val completedReviewCount: Int = 0,
    val isAtRisk: Boolean = false,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
    ,val difficulty: String = "MEDIUM"
    ,val importance: String = "NORMAL"
    ,val initialMastery: Int? = null
    ,val estimatedReviewMinutes: Int = 15
    ,val optionalMinReviewCount: Int? = null
) {
    fun toDomain(): StudyRecord = StudyRecord(
        id = id,
        uid = uid,
        blockId = blockId,
        examId = examId,
        title = title,
        content = content,
        learningDate = learningDate,
        examDate = examDate,
        prepStartDate = prepStartDate,
        recommendedCompletionDate = recommendedCompletionDate,
        stage = stage,
        successCount = successCount,
        stability = stability,
        plannedReviewCount = plannedReviewCount,
        completedReviewCount = completedReviewCount,
        isAtRisk = isAtRisk,
        createdAt = createdAt?.toDate()?.time ?: 0L,
        updatedAt = updatedAt?.toDate()?.time ?: 0L
        ,difficulty = runCatching { ReviewDifficulty.valueOf(difficulty) }.getOrDefault(ReviewDifficulty.MEDIUM)
        ,importance = runCatching { ReviewImportance.valueOf(importance) }.getOrDefault(ReviewImportance.NORMAL)
        ,initialMastery = initialMastery
        ,estimatedReviewMinutes = estimatedReviewMinutes
        ,optionalMinReviewCount = optionalMinReviewCount
    )
}

fun StudyRecord.toDto(): StudyRecordDto = StudyRecordDto(
    id = id,
    uid = uid,
    blockId = blockId,
    examId = examId,
    title = title,
    content = content,
    learningDate = learningDate,
    examDate = examDate,
    prepStartDate = prepStartDate,
    recommendedCompletionDate = recommendedCompletionDate,
    stage = stage,
    successCount = successCount,
    stability = stability,
    plannedReviewCount = plannedReviewCount,
    completedReviewCount = completedReviewCount,
    isAtRisk = isAtRisk,
    difficulty = difficulty.name,
    importance = importance.name,
    initialMastery = initialMastery,
    estimatedReviewMinutes = estimatedReviewMinutes,
    optionalMinReviewCount = optionalMinReviewCount
    // createdAt/updatedAt: Firestore에서 FieldValue.serverTimestamp()
)