package com.loorve.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.loorve.domain.model.ReviewSchedule
import com.loorve.domain.repository.ReviewScheduleRepository
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ReviewScheduleRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ReviewScheduleRepository {

    companion object {
        private const val TAG = "ReviewScheduleRepo"
        private val KST = java.time.ZoneId.of("Asia/Seoul")
    }

    override suspend fun saveReviewSchedule(
        reviewSchedule: ReviewSchedule
    ): Result<Unit> = saveReviewSchedule(
        uid = reviewSchedule.userId,
        reviewSchedule = reviewSchedule
    )

    override suspend fun saveReviewSchedule(
        uid: String,
        reviewSchedule: ReviewSchedule
    ): Result<Unit> {
        return runCatching {
            require(uid.isNotBlank()) { "사용자 ID가 비어 있습니다." }
            require(reviewSchedule.scheduleId.isNotBlank()) { "복습 일정 ID가 비어 있습니다." }

            val scheduleToSave = reviewSchedule.copy(userId = uid)

            firestore
                .collection("users")
                .document(uid)
                .collection("reviewSchedules")
                .document(scheduleToSave.scheduleId)
                .set(scheduleToSave.toFirestoreMap())
                .await()
        }
    }

    override fun getReviewSchedulesByDateRange(
        uid: String,
        startDate: String,
        endDate: String
    ): Flow<List<ReviewSchedule>> = callbackFlow {
        if (uid.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val startMillis = parseStartOfDayMillis(startDate)
        val endExclusiveMillis = parseNextDayStartMillis(endDate)

        val listenerRegistration = firestore
            .collection("users")
            .document(uid)
            .collection("reviewSchedules")
            .whereGreaterThanOrEqualTo("reviewDate", startMillis)
            .whereLessThan("reviewDate", endExclusiveMillis)
            .orderBy("reviewDate", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "복습일정 스냅샷 오류 (리스너 유지): ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val schedules = snapshot?.documents.orEmpty()
                    .mapNotNull { document ->
                        runCatching {
                            document.toObject(ReviewSchedule::class.java)
                                ?.apply {
                                    if (scheduleId.isBlank()) scheduleId = document.id
                                }
                        }.getOrNull()
                    }

                Log.d(TAG, "복습일정 스냅샷 수신: ${schedules.size}건 (${startDate}~${endDate})")
                trySend(schedules)
            }

        awaitClose { listenerRegistration.remove() }
    }

    override suspend fun getReviewSchedulesByProgressId(
        uid: String,
        progressId: String
    ): Result<List<ReviewSchedule>> {
        return runCatching {
            require(uid.isNotBlank()) { "사용자 ID가 비어 있습니다." }
            require(progressId.isNotBlank()) { "진도 ID가 비어 있습니다." }

            firestore
                .collection("users")
                .document(uid)
                .collection("reviewSchedules")
                .whereEqualTo("originProgressId", progressId)
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    doc.toObject(ReviewSchedule::class.java)
                        ?.also { if (it.scheduleId.isBlank()) it.scheduleId = doc.id }
                }
        }
    }

    override suspend fun getUpcomingIncompleteSchedules(
        uid: String,
        fromMillis: Long
    ): Result<List<ReviewSchedule>> {
        return runCatching {
            require(uid.isNotBlank()) { "사용자 ID가 비어 있습니다." }

            firestore
                .collection("users")
                .document(uid)
                .collection("reviewSchedules")
                .whereEqualTo("isCompleted", false)
                .whereGreaterThanOrEqualTo("reviewDate", fromMillis)
                .orderBy("reviewDate", Query.Direction.ASCENDING)
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    doc.toObject(ReviewSchedule::class.java)
                        ?.also { if (it.scheduleId.isBlank()) it.scheduleId = doc.id }
                }
        }
    }

    override suspend fun getReviewScheduleById(
        uid: String,
        scheduleId: String
    ): Result<ReviewSchedule?> {
        return runCatching {
            require(uid.isNotBlank()) { "사용자 ID가 비어 있습니다." }
            require(scheduleId.isNotBlank()) { "복습 일정 ID가 비어 있습니다." }

            firestore
                .collection("users")
                .document(uid)
                .collection("reviewSchedules")
                .document(scheduleId)
                .get()
                .await()
                .let { doc ->
                    doc.toObject(ReviewSchedule::class.java)
                        ?.also { if (it.scheduleId.isBlank()) it.scheduleId = doc.id }
                }
        }
    }

    override suspend fun completeReviewSchedule(
        uid: String,
        scheduleId: String
    ): Result<Unit> = updateCompletion(uid, scheduleId, true)

    override suspend fun updateReviewCompletion(
        uid: String,
        scheduleId: String,
        isCompleted: Boolean
    ): Result<Unit> = updateCompletion(uid, scheduleId, isCompleted)

    override suspend fun deleteReviewSchedule(
        uid: String,
        scheduleId: String
    ): Result<Unit> {
        return runCatching {
            require(uid.isNotBlank()) { "사용자 ID가 비어 있습니다." }
            require(scheduleId.isNotBlank()) { "복습 일정 ID가 비어 있습니다." }

            firestore
                .collection("users")
                .document(uid)
                .collection("reviewSchedules")
                .document(scheduleId)
                .delete()
                .await()
        }
    }

    private suspend fun updateCompletion(
        uid: String,
        scheduleId: String,
        isCompleted: Boolean
    ): Result<Unit> {
        return runCatching {
            require(uid.isNotBlank()) { "사용자 ID가 비어 있습니다." }
            require(scheduleId.isNotBlank()) { "복습 일정 ID가 비어 있습니다." }

            firestore
                .collection("users")
                .document(uid)
                .collection("reviewSchedules")
                .document(scheduleId)
                .update(
                    mapOf(
                        "isCompleted" to isCompleted,
                        "updatedAt" to System.currentTimeMillis()
                    )
                )
                .await()
        }
    }

    private fun ReviewSchedule.toFirestoreMap(): Map<String, Any> {
        return mapOf(
            "scheduleId"       to scheduleId,
            "blockId"          to blockId,
            "uid"              to userId,
            "originProgressId" to originProgressId,
            "title"            to title,
            "reviewDate"       to reviewDate,
            "reviewDateText"   to reviewDateText,
            "reviewOrder"      to reviewOrder,
            "scheduleType"     to scheduleType,
            "isCompleted"      to isCompleted,
            "createdAt"        to createdAt,
            "updatedAt"        to updatedAt
        )
    }

    private fun parseStartOfDayMillis(date: String): Long =
        java.time.LocalDate.parse(date)
            .atStartOfDay(KST)
            .toInstant()
            .toEpochMilli()

    private fun parseNextDayStartMillis(date: String): Long =
        java.time.LocalDate.parse(date)
            .plusDays(1)
            .atStartOfDay(KST)
            .toInstant()
            .toEpochMilli()
}