package com.loorve.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.loorve.domain.model.ReviewScheduleItem
import com.loorve.domain.model.StudyRecord
import com.loorve.domain.notification.ReviewNotificationOutbox
import com.loorve.domain.repository.ReviewSchedulingRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await

@Singleton
class ReviewSchedulingRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ReviewSchedulingRepository {
    private fun requireOwner(uid: String) {
        val currentUid = auth.currentUser?.uid
            ?: throw SecurityException("로그인이 필요합니다.")
        require(currentUid == uid) { "본인의 데이터만 변경할 수 있습니다." }
    }

    private fun recordRef(uid: String, id: String) =
        firestore.collection("users").document(uid).collection("studyRecords").document(id)

    private fun scheduleRef(uid: String, id: String) =
        firestore.collection("users").document(uid).collection("reviewScheduleItems").document(id)

    private fun outboxRef(uid: String, id: String) =
        firestore.collection("users").document(uid).collection("reviewNotificationOutbox").document(id)

    override suspend fun saveStudyRecordWithSchedules(
        uid: String,
        record: StudyRecord,
        schedules: List<ReviewScheduleItem>,
        notifications: List<ReviewNotificationOutbox>
    ): Result<Unit> = runCatching {
        requireOwner(uid)
        require(record.uid == uid) { "학습기록 소유자가 현재 사용자와 다릅니다." }
        val ref = recordRef(uid, record.id)
        val batch = firestore.batch()
        batch.set(ref, recordMap(record))
        schedules.forEach { schedule ->
            require(schedule.uid == uid) { "복습 일정 소유자가 현재 사용자와 다릅니다." }
            batch.set(scheduleRef(uid, schedule.id), scheduleMap(schedule))
        }
        notifications.forEach { batch.set(outboxRef(uid, it.id), outboxMap(it)) }
        batch.commit().await()
    }

    override suspend fun completeReviewAndUpdateSchedules(
        uid: String,
        completed: ReviewScheduleItem,
        changedFutureSchedules: List<ReviewScheduleItem>,
        notifications: List<ReviewNotificationOutbox>
    ): Result<Unit> = runCatching {
        requireOwner(uid)
        require(completed.uid == uid) { "복습 일정 소유자가 현재 사용자와 다릅니다." }
        val batch = firestore.batch()
        batch.update(scheduleRef(uid, completed.id), scheduleMap(completed))
        changedFutureSchedules.forEach {
            require(it.uid == uid) { "복습 일정 소유자가 현재 사용자와 다릅니다." }
            batch.update(scheduleRef(uid, it.id), scheduleMap(it))
        }
        notifications.forEach { batch.set(outboxRef(uid, it.id), outboxMap(it)) }
        batch.commit().await()
    }

    private fun recordMap(record: StudyRecord): Map<String, Any?> = mapOf(
        "id" to record.id, "uid" to record.uid, "blockId" to record.blockId,
        "examId" to record.examId, "title" to record.title, "content" to record.content,
        "learningDate" to record.learningDate, "examDate" to record.examDate,
        "prepStartDate" to record.prepStartDate,
        "recommendedCompletionDate" to record.recommendedCompletionDate,
        "stage" to record.stage, "successCount" to record.successCount,
        "stability" to record.stability, "plannedReviewCount" to record.plannedReviewCount,
        "completedReviewCount" to record.completedReviewCount, "isAtRisk" to record.isAtRisk,
        "difficulty" to record.difficulty.name, "importance" to record.importance.name,
        "initialMastery" to record.initialMastery,
        "estimatedReviewMinutes" to record.estimatedReviewMinutes,
        "optionalMinReviewCount" to record.optionalMinReviewCount,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun scheduleMap(item: ReviewScheduleItem): Map<String, Any?> = mapOf(
        "id" to item.id, "studyRecordId" to item.studyRecordId, "blockId" to item.blockId,
        "uid" to item.uid, "title" to item.title, "reviewDate" to item.reviewDate,
        "originalReviewDate" to item.originalReviewDate, "stage" to item.stage,
        "reviewOrder" to item.reviewOrder, "status" to item.status.name,
        "previousGapDays" to item.previousGapDays, "overdueDays" to item.overdueDays,
        "compressedReview" to item.compressedReview,
        "completionResult" to item.completionResult?.name, "completedAt" to item.completedAt,
        "planStatus" to item.planStatus.name, "priorityScore" to item.priorityScore,
        "estimatedReviewMinutes" to item.estimatedReviewMinutes,
        "recommendedMethod" to item.recommendedMethod, "isFinalReview" to item.isFinalReview,
        "rescheduleReason" to item.rescheduleReason, "outcome" to item.outcome,
        "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun outboxMap(event: ReviewNotificationOutbox): Map<String, Any?> = mapOf(
        "id" to event.id, "uid" to event.uid, "reviewId" to event.reviewId,
        "studyRecordId" to event.studyRecordId, "kind" to event.kind.name,
        "scheduledDate" to event.scheduledDate.toString(),
        "triggerAtMillis" to event.triggerAtMillis, "status" to event.status,
        "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp()
    )
}
