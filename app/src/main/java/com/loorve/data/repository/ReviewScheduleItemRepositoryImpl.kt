package com.loorve.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.loorve.data.model.ReviewScheduleItemDto
import com.loorve.domain.model.ReviewScheduleItem
import com.loorve.domain.model.ReviewStatus
import com.loorve.domain.repository.ReviewScheduleItemRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ReviewScheduleItemRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ReviewScheduleItemRepository {

    private fun schedulesRef(uid: String) =
        firestore.collection("users").document(uid).collection("reviewScheduleItems")

    override suspend fun saveSchedules(
        uid: String,
        studyRecordId: String,
        items: List<ReviewScheduleItem>
    ): Result<Unit> = runCatching {
        val currentUid = auth.currentUser?.uid
            ?: throw SecurityException("인증되지 않은 사용자입니다.")
        require(currentUid == uid) { "본인의 일정만 저장할 수 있습니다." }

        val batch = firestore.batch()
        items.forEach { item ->
            val docRef = schedulesRef(uid).document(item.id)
            batch.set(docRef, buildItemMap(item))
        }
        batch.commit().await()
    }

    override suspend fun getSchedulesByStudyRecord(
        uid: String,
        studyRecordId: String
    ): Result<List<ReviewScheduleItem>> = runCatching {
        validateAuth(uid)
        schedulesRef(uid)
            .whereEqualTo("studyRecordId", studyRecordId)
            .get().await()
            .documents
            .mapNotNull { doc ->
                doc.toObject(ReviewScheduleItemDto::class.java)
                    ?.copy(id = doc.id)
                    ?.toDomain()
            }
    }

    override suspend fun getPendingSchedulesByBlock(
        uid: String,
        blockId: String
    ): Result<List<ReviewScheduleItem>> = runCatching {
        validateAuth(uid)
        schedulesRef(uid)
            .whereEqualTo("blockId", blockId)
            .whereIn("status", listOf(
                ReviewStatus.PENDING.name,
                ReviewStatus.OVERDUE.name
            ))
            .get().await()
            .documents
            .mapNotNull { doc ->
                doc.toObject(ReviewScheduleItemDto::class.java)
                    ?.copy(id = doc.id)
                    ?.toDomain()
            }
    }

    override suspend fun updateScheduleItem(
        uid: String,
        item: ReviewScheduleItem
    ): Result<Unit> = runCatching {
        validateAuth(uid)
        schedulesRef(uid).document(item.id)
            .update(buildItemMap(item) + mapOf("updatedAt" to FieldValue.serverTimestamp()))
            .await()
    }

    override suspend fun batchUpdatePendingSchedules(
        uid: String,
        items: List<ReviewScheduleItem>
    ): Result<Unit> = runCatching {
        validateAuth(uid)
        firestore.runBatch { batch ->
            items.forEach { item ->
                val ref = schedulesRef(uid).document(item.id)
                batch.update(ref, mapOf(
                    "reviewDate"       to item.reviewDate,
                    "previousGapDays"  to item.previousGapDays,
                    "compressedReview" to item.compressedReview,
                    "updatedAt"        to FieldValue.serverTimestamp()
                ))
            }
        }.await()
    }

    private fun validateAuth(uid: String) {
        val currentUid = auth.currentUser?.uid
            ?: throw SecurityException("인증되지 않은 사용자입니다.")
        require(currentUid == uid) { "본인의 데이터만 접근할 수 있습니다." }
    }

    private fun buildItemMap(item: ReviewScheduleItem): Map<String, Any?> = mapOf(
        "id"                  to item.id,
        "studyRecordId"       to item.studyRecordId,
        "blockId"             to item.blockId,
        "uid"                 to item.uid,
        "title"               to item.title,
        "reviewDate"          to item.reviewDate,
        "originalReviewDate"  to item.originalReviewDate,
        "stage"               to item.stage,
        "reviewOrder"         to item.reviewOrder,
        "status"              to item.status.name,
        "previousGapDays"     to item.previousGapDays,
        "overdueDays"         to item.overdueDays,
        "compressedReview"    to item.compressedReview,
        "completionResult"    to item.completionResult?.name,
        "completedAt"         to item.completedAt,
        "createdAt"           to FieldValue.serverTimestamp(),
        "updatedAt"           to FieldValue.serverTimestamp()
    )
}