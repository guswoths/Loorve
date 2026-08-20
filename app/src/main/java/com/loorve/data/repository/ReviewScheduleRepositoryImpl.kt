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
import javax.inject.Inject

class ReviewScheduleRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore  // ✅ 주석 해제 및 실제 의존성 추가
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
        val listener = scheduleCollection(uid)
            .whereGreaterThanOrEqualTo("scheduledDate", startDate)
            .whereLessThanOrEqualTo("scheduledDate", endDate)
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
            java.time.LocalDate.now().toString(),
            java.time.LocalDate.now().toString()
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
}