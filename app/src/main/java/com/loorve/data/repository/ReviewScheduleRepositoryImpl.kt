// app/src/main/java/com/loorve/data/repository/ReviewScheduleRepositoryImpl.kt
package com.loorve.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
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

    companion object {
        private const val TAG = "ReviewScheduleRepo"
    }

    @Throws(IllegalStateException::class)
    private suspend fun requireValidAuthWithRefresh(uid: String) {
        if (uid.isBlank()) throw IllegalStateException("로그인 정보가 없습니다. uid가 비어 있습니다.")
        val currentUser = FirebaseAuth.getInstance().currentUser
            ?: throw IllegalStateException("로그인 세션이 만료되었습니다. 다시 로그인해 주세요.")
        if (currentUser.uid != uid) throw IllegalStateException("인증 정보가 일치하지 않습니다.")
        try { currentUser.getIdToken(true).await() } catch (e: Exception) {
            Log.w(TAG, "토큰 갱신 실패 (무시하고 진행): ${e.message}")
        }
    }

    private fun FirebaseFirestoreException.toUserMessage(): String = when (code) {
        FirebaseFirestoreException.Code.PERMISSION_DENIED -> "권한이 없습니다. 로그인 상태를 확인해 주세요."
        FirebaseFirestoreException.Code.UNAVAILABLE -> "네트워크 연결을 확인해 주세요."
        FirebaseFirestoreException.Code.NOT_FOUND -> "대상 문서를 찾을 수 없습니다."
        else -> "Firestore 오류: ${message}"
    }

    private fun scheduleCollection(uid: String) =
        firestore.collection("users").document(uid).collection("reviewSchedules")

    override suspend fun createReviewSchedule(uid: String, schedule: ReviewSchedule): Result<Unit> =
        runCatching {
            requireValidAuthWithRefresh(uid)
            scheduleCollection(uid).document(schedule.reviewScheduleId).set(schedule).await()
            Log.d(TAG, "createReviewSchedule 완료: uid=$uid, id=${schedule.reviewScheduleId}")
        }

    override suspend fun saveReviewSchedule(uid: String, schedule: ReviewSchedule): Result<Unit> =
        runCatching {
            requireValidAuthWithRefresh(uid)
            scheduleCollection(uid).document(schedule.reviewScheduleId).set(schedule).await()
            Log.d(TAG, "saveReviewSchedule 완료: uid=$uid, id=${schedule.reviewScheduleId}")
        }

    override suspend fun saveReviewSchedules(uid: String, schedules: List<ReviewSchedule>): Result<Unit> =
        runCatching {
            requireValidAuthWithRefresh(uid)
            val batch = firestore.batch()
            schedules.forEach { schedule ->
                batch.set(scheduleCollection(uid).document(schedule.reviewScheduleId), schedule)
            }
            batch.commit().await()
            Log.d(TAG, "saveReviewSchedules 완료: uid=$uid, count=${schedules.size}")
        }

    override suspend fun updateReviewCompletion(
        uid: String, scheduleId: String, isCompleted: Boolean
    ): Result<Unit> = runCatching {
        requireValidAuthWithRefresh(uid)
        scheduleCollection(uid).document(scheduleId).update("isCompleted", isCompleted).await()
    }

    override suspend fun completeReviewSchedule(uid: String, scheduleId: String): Result<Unit> =
        updateReviewCompletion(uid, scheduleId, true)

    override suspend fun deleteReviewSchedule(uid: String, scheduleId: String): Result<Unit> =
        runCatching {
            requireValidAuthWithRefresh(uid)
            scheduleCollection(uid).document(scheduleId).delete().await()
            Log.d(TAG, "deleteReviewSchedule 완료: uid=$uid, scheduleId=$scheduleId")
        }

    override fun getReviewSchedulesByDateRange(
        uid: String, startDate: String, endDate: String
    ): Flow<List<ReviewSchedule>> = callbackFlow {
        if (uid.isBlank()) { close(IllegalStateException("uid가 비어 있습니다.")); return@callbackFlow }
        val zone = ZoneId.of("Asia/Seoul")
        val startEpoch = LocalDate.parse(startDate).atStartOfDay(zone).toInstant().toEpochMilli()
        val endEpoch = LocalDate.parse(endDate).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        val listener = scheduleCollection(uid)
            .whereGreaterThanOrEqualTo("reviewDate", startEpoch)
            .whereLessThanOrEqualTo("reviewDate", endEpoch)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.toObjects(ReviewSchedule::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    override fun getTodayReviewSchedules(uid: String): Flow<List<ReviewSchedule>> =
        getReviewSchedulesByDateRange(uid, LocalDate.now().toString(), LocalDate.now().toString())

    override fun getOverdueAndIncompleteSchedules(uid: String): Flow<List<ReviewSchedule>> =
        emptyFlow()

    override suspend fun getReviewScheduleById(uid: String, scheduleId: String): Result<ReviewSchedule> =
        runCatching {
            if (uid.isBlank()) error("uid가 비어 있습니다.")
            firestore.collection("users").document(uid)
                .collection("reviewSchedules").document(scheduleId)
                .get().await()
                .toObject(ReviewSchedule::class.java) ?: error("ReviewSchedule not found: $scheduleId")
        }

    override suspend fun getReviewSchedulesByProgressId(
        uid: String, progressId: String
    ): Result<List<ReviewSchedule>> = runCatching {
        if (uid.isBlank()) error("uid가 비어 있습니다.")
        firestore.collection("users").document(uid)
            .collection("reviewSchedules")
            .whereEqualTo("originProgressId", progressId)
            .get().await()
            .toObjects(ReviewSchedule::class.java)
    }

    override suspend fun getUpcomingIncompleteSchedules(
        uid: String, fromMillis: Long
    ): Result<List<ReviewSchedule>> = runCatching {
        if (uid.isBlank()) error("uid가 비어 있습니다.")
        firestore.collection("users").document(uid)
            .collection("reviewSchedules")
            .whereEqualTo("isCompleted", false)
            .whereGreaterThan("reviewDate", fromMillis)
            .get().await()
            .documents.mapNotNull { it.toObject(ReviewSchedule::class.java) }
    }
}