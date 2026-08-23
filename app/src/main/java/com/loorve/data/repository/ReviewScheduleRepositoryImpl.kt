// app/src/main/java/com/loorve/data/repository/ReviewScheduleRepositoryImpl.kt
package com.loorve.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.loorve.domain.model.ReviewSchedule
import com.loorve.domain.repository.ReviewScheduleRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class ReviewScheduleRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ReviewScheduleRepository {

    private fun scheduleCollection(uid: String) =
        firestore.collection("users").document(uid).collection("reviewSchedules")

    override suspend fun createReviewSchedule(uid: String, schedule: ReviewSchedule): Result<Unit> {
        return try {
            scheduleCollection(uid).document(schedule.reviewScheduleId).set(schedule).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getReviewSchedulesByDateRange(
        uid: String, startDate: String, endDate: String
    ): Flow<List<ReviewSchedule>> = callbackFlow {
        // ✅ 수정: "scheduledDate" 문자열 필드 → "reviewDate" epoch ms Long 필드로 통일
        // startDate / endDate ("yyyy-MM-dd") → epoch milliseconds (Asia/Seoul 기준)
        val zone = ZoneId.of("Asia/Seoul")
        val startEpoch = LocalDate.parse(startDate).atStartOfDay(zone).toInstant().toEpochMilli()
        val endEpoch = LocalDate.parse(endDate).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1

        val listener = scheduleCollection(uid)
            .whereGreaterThanOrEqualTo("reviewDate", startEpoch)
            .whereLessThanOrEqualTo("reviewDate", endEpoch)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val list = snapshot?.toObjects(ReviewSchedule::class.java) ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    override fun getTodayReviewSchedules(uid: String): Flow<List<ReviewSchedule>> =
        getReviewSchedulesByDateRange(
            uid,
            LocalDate.now().toString(),
            LocalDate.now().toString()
        )

    override fun getOverdueAndIncompleteSchedules(uid: String): Flow<List<ReviewSchedule>> =
        emptyFlow() // TODO: 필요 시 구현

    override suspend fun completeReviewSchedule(uid: String, scheduleId: String): Result<Unit> =
        updateReviewCompletion(uid, scheduleId, true)

    override suspend fun updateReviewCompletion(
        uid: String, scheduleId: String, isCompleted: Boolean
    ): Result<Unit> {
        return try {
            scheduleCollection(uid).document(scheduleId)
                .update("isCompleted", isCompleted).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveReviewSchedules(uid: String, schedules: List<ReviewSchedule>): Result<Unit> {
        return try {
            val batch = firestore.batch()
            schedules.forEach { schedule ->
                val ref = scheduleCollection(uid).document(schedule.reviewScheduleId)
                batch.set(ref, schedule)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteReviewSchedule(uid: String, scheduleId: String): Result<Unit> {
        return try {
            scheduleCollection(uid).document(scheduleId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getReviewScheduleById(
        uid: String,
        scheduleId: String
    ): Result<ReviewSchedule> = runCatching {
        firestore.collection("users").document(uid)
            .collection("reviewSchedules").document(scheduleId)
            .get().await()
            .toObject(ReviewSchedule::class.java)
            ?: error("ReviewSchedule not found: $scheduleId")
    }

    override suspend fun getReviewSchedulesByProgressId(
        uid: String,
        progressId: String
    ): Result<List<ReviewSchedule>> = runCatching {
        firestore.collection("users").document(uid)
            .collection("reviewSchedules")
            .whereEqualTo("originProgressId", progressId)
            .get().await()
            .toObjects(ReviewSchedule::class.java)
    }

    override suspend fun getUpcomingIncompleteSchedules(
        uid: String,
        fromMillis: Long
    ): Result<List<ReviewSchedule>> = runCatching {
        val snapshot = firestore
            .collection("users")
            .document(uid)
            .collection("reviewSchedules")
            .whereEqualTo("isCompleted", false)
            .whereGreaterThan("reviewDate", fromMillis)
            .get()
            .await()

        snapshot.documents.mapNotNull { doc ->
            doc.toObject(ReviewSchedule::class.java)
        }
    }

    override suspend fun saveReviewSchedule(
        uid: String,
        schedule: ReviewSchedule
    ): Result<Unit> = runCatching {
        firestore
            .collection("users")
            .document(uid)
            .collection("reviewSchedules")
            .document(schedule.reviewScheduleId)
            .set(schedule)
            .await()
    }
}