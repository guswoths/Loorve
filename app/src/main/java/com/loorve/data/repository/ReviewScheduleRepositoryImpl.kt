package com.loorve.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.loorve.domain.model.ReviewSchedule
import com.loorve.domain.repository.ReviewScheduleRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ReviewScheduleRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ReviewScheduleRepository {

    override suspend fun saveReviewSchedule(
        reviewSchedule: ReviewSchedule
    ): Result<Unit> {
        return runCatching {
            require(reviewSchedule.userId.isNotBlank()) {
                "사용자 ID가 비어 있습니다."
            }

            require(reviewSchedule.scheduleId.isNotBlank()) {
                "복습 일정 ID가 비어 있습니다."
            }

            val scheduleData = hashMapOf(
                "scheduleId" to reviewSchedule.scheduleId,
                "blockId" to reviewSchedule.blockId,
                "userId" to reviewSchedule.userId,
                "title" to reviewSchedule.title,
                "reviewDate" to reviewSchedule.reviewDate,
                "reviewDateText" to reviewSchedule.reviewDateText,
                "reviewOrder" to reviewSchedule.reviewOrder,
                "scheduleType" to reviewSchedule.scheduleType,
                "isCompleted" to reviewSchedule.isCompleted,
                "createdAt" to reviewSchedule.createdAt,
                "updatedAt" to reviewSchedule.updatedAt
            )

            firestore
                .collection("users")
                .document(reviewSchedule.userId)
                .collection("reviewSchedules")
                .document(reviewSchedule.scheduleId)
                .set(scheduleData)
                .await()
        }
    }

    override fun getReviewSchedulesByDateRange(
        uid: String,
        startDate: String,
        endDate: String
    ): Flow<List<ReviewSchedule>> = callbackFlow {
        if (uid.isBlank()) {
            close(IllegalArgumentException("사용자 ID가 비어 있습니다."))
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
                    close(error)
                    return@addSnapshotListener
                }

                val schedules = snapshot?.documents.orEmpty()
                    .mapNotNull { document ->
                        document.toObject(ReviewSchedule::class.java)
                    }

                trySend(schedules)
            }

        awaitClose {
            listenerRegistration.remove()
        }
    }

    override suspend fun completeReviewSchedule(
        uid: String,
        scheduleId: String
    ): Result<Unit> {
        return updateCompletion(
            uid = uid,
            scheduleId = scheduleId,
            isCompleted = true
        )
    }

    override suspend fun updateReviewCompletion(
        uid: String,
        scheduleId: String,
        isCompleted: Boolean
    ): Result<Unit> {
        return updateCompletion(
            uid = uid,
            scheduleId = scheduleId,
            isCompleted = isCompleted
        )
    }

    override suspend fun deleteReviewSchedule(
        uid: String,
        scheduleId: String
    ): Result<Unit> {
        return runCatching {
            require(uid.isNotBlank()) {
                "사용자 ID가 비어 있습니다."
            }

            require(scheduleId.isNotBlank()) {
                "복습 일정 ID가 비어 있습니다."
            }

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
            require(uid.isNotBlank()) {
                "사용자 ID가 비어 있습니다."
            }

            require(scheduleId.isNotBlank()) {
                "복습 일정 ID가 비어 있습니다."
            }

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

    private fun parseStartOfDayMillis(date: String): Long {
        val localDate = java.time.LocalDate.parse(date)

        return localDate
            .atStartOfDay(java.time.ZoneId.of("Asia/Seoul"))
            .toInstant()
            .toEpochMilli()
    }

    private fun parseNextDayStartMillis(date: String): Long {
        val localDate = java.time.LocalDate.parse(date)

        return localDate
            .plusDays(1)
            .atStartOfDay(java.time.ZoneId.of("Asia/Seoul"))
            .toInstant()
            .toEpochMilli()
    }
}