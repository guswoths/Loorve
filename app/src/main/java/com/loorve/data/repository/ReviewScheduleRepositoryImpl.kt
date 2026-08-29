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
import javax.inject.Singleton

@Singleton
class ReviewScheduleRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ReviewScheduleRepository {

    companion object {
        private const val TAG = "ReviewScheduleRepo"
    }

    // ─────────────────────────────────────────────────────────────
    // ✅ uid 유효성 + 인증 상태 헬퍼 (모든 쓰기 함수 진입 시 호출)
    // ─────────────────────────────────────────────────────────────
    /**
     * uid가 비어 있거나 현재 Firebase 인증 사용자와 불일치하면 예외를 던진다.
     * - uid 빈값  → 로그인 정보 없음
     * - currentUser null → 세션 만료 (재로그인 필요)
     * - uid ≠ currentUser.uid → 위변조 시도 또는 계정 전환 버그
     */
    @Throws(IllegalStateException::class)
    private fun requireValidAuth(uid: String) {
        if (uid.isBlank()) {
            throw IllegalStateException("로그인 정보가 없습니다. uid가 비어 있습니다.")
        }
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            throw IllegalStateException("로그인 세션이 만료되었습니다. 다시 로그인해 주세요.")
        }
        if (currentUser.uid != uid) {
            throw IllegalStateException("인증 정보가 일치하지 않습니다. 다시 로그인해 주세요.")
        }
    }

    /**
     * FirebaseFirestoreException을 사람이 읽기 좋은 메시지로 변환한다.
     */
    private fun FirebaseFirestoreException.toUserMessage(): String = when (code) {
        FirebaseFirestoreException.Code.PERMISSION_DENIED ->
            "저장에 실패했습니다: 권한이 없습니다. 로그인 상태를 확인해 주세요."
        FirebaseFirestoreException.Code.UNAVAILABLE ->
            "서버에 연결할 수 없습니다. 네트워크 연결을 확인해 주세요."
        FirebaseFirestoreException.Code.NOT_FOUND ->
            "대상 문서를 찾을 수 없습니다."
        else -> "Firestore 오류: ${message}"
    }

    // ─────────────────────────────────────────────────────────────
    // 내부 컬렉션 참조
    // ─────────────────────────────────────────────────────────────
    private fun scheduleCollection(uid: String) =
        firestore.collection("users").document(uid).collection("reviewSchedules")

    // ─────────────────────────────────────────────────────────────
    // 쓰기 함수
    // ─────────────────────────────────────────────────────────────

    override suspend fun createReviewSchedule(uid: String, schedule: ReviewSchedule): Result<Unit> {
        return try {
            requireValidAuth(uid)                                           // ✅ uid 유효성 체크
            scheduleCollection(uid).document(schedule.reviewScheduleId).set(schedule).await()
            Log.d(TAG, "createReviewSchedule 완료: uid=$uid, id=${schedule.reviewScheduleId}")
            Result.success(Unit)
        } catch (e: IllegalStateException) {
            Log.e(TAG, "createReviewSchedule 인증 실패: ${e.message}")
            Result.failure(e)
        } catch (e: FirebaseFirestoreException) {
            Log.e(TAG, "createReviewSchedule Firestore 오류(${e.code}): uid=$uid", e)
            Result.failure(IllegalStateException(e.toUserMessage(), e))
        } catch (e: Exception) {
            Log.e(TAG, "createReviewSchedule 실패: uid=$uid", e)
            Result.failure(e)
        }
    }

    override suspend fun saveReviewSchedule(
        uid: String,
        schedule: ReviewSchedule
    ): Result<Unit> {
        return try {
            requireValidAuth(uid)                                           // ✅ uid 유효성 체크
            firestore
                .collection("users")
                .document(uid)
                .collection("reviewSchedules")
                .document(schedule.reviewScheduleId)
                .set(schedule)
                .await()
            Log.d(TAG, "saveReviewSchedule 완료: uid=$uid, id=${schedule.reviewScheduleId}")
            Result.success(Unit)
        } catch (e: IllegalStateException) {
            Log.e(TAG, "saveReviewSchedule 인증 실패: ${e.message}")
            Result.failure(e)
        } catch (e: FirebaseFirestoreException) {
            Log.e(TAG, "saveReviewSchedule Firestore 오류(${e.code}): uid=$uid", e)
            Result.failure(IllegalStateException(e.toUserMessage(), e))
        } catch (e: Exception) {
            Log.e(TAG, "saveReviewSchedule 실패: uid=$uid", e)
            Result.failure(e)
        }
    }

    override suspend fun saveReviewSchedules(uid: String, schedules: List<ReviewSchedule>): Result<Unit> {
        return try {
            requireValidAuth(uid)                                           // ✅ uid 유효성 체크
            val batch = firestore.batch()
            schedules.forEach { schedule ->
                val ref = scheduleCollection(uid).document(schedule.reviewScheduleId)
                batch.set(ref, schedule)
            }
            batch.commit().await()
            Log.d(TAG, "saveReviewSchedules 완료: uid=$uid, count=${schedules.size}")
            Result.success(Unit)
        } catch (e: IllegalStateException) {
            Log.e(TAG, "saveReviewSchedules 인증 실패: ${e.message}")
            Result.failure(e)
        } catch (e: FirebaseFirestoreException) {
            Log.e(TAG, "saveReviewSchedules Firestore 오류(${e.code}): uid=$uid", e)
            Result.failure(IllegalStateException(e.toUserMessage(), e))
        } catch (e: Exception) {
            Log.e(TAG, "saveReviewSchedules 실패: uid=$uid", e)
            Result.failure(e)
        }
    }

    override suspend fun updateReviewCompletion(
        uid: String, scheduleId: String, isCompleted: Boolean
    ): Result<Unit> {
        return try {
            requireValidAuth(uid)                                           // ✅ uid 유효성 체크
            scheduleCollection(uid).document(scheduleId)
                .update("isCompleted", isCompleted).await()
            Result.success(Unit)
        } catch (e: IllegalStateException) {
            Log.e(TAG, "updateReviewCompletion 인증 실패: ${e.message}")
            Result.failure(e)
        } catch (e: FirebaseFirestoreException) {
            Log.e(TAG, "updateReviewCompletion Firestore 오류(${e.code}): uid=$uid", e)
            Result.failure(IllegalStateException(e.toUserMessage(), e))
        } catch (e: Exception) {
            Log.e(TAG, "updateReviewCompletion 실패: uid=$uid", e)
            Result.failure(e)
        }
    }

    override suspend fun completeReviewSchedule(uid: String, scheduleId: String): Result<Unit> =
        updateReviewCompletion(uid, scheduleId, true)

    override suspend fun deleteReviewSchedule(uid: String, scheduleId: String): Result<Unit> {
        return try {
            requireValidAuth(uid)                                           // ✅ uid 유효성 체크
            scheduleCollection(uid).document(scheduleId).delete().await()
            Log.d(TAG, "deleteReviewSchedule 완료: uid=$uid, scheduleId=$scheduleId")
            Result.success(Unit)
        } catch (e: IllegalStateException) {
            Log.e(TAG, "deleteReviewSchedule 인증 실패: ${e.message}")
            Result.failure(e)
        } catch (e: FirebaseFirestoreException) {
            Log.e(TAG, "deleteReviewSchedule Firestore 오류(${e.code}): uid=$uid", e)
            Result.failure(IllegalStateException(e.toUserMessage(), e))
        } catch (e: Exception) {
            Log.e(TAG, "deleteReviewSchedule 실패: uid=$uid", e)
            Result.failure(e)
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 읽기 함수 (uid 체크 포함 — 방어적 적용)
    // ─────────────────────────────────────────────────────────────

    override fun getReviewSchedulesByDateRange(
        uid: String, startDate: String, endDate: String
    ): Flow<List<ReviewSchedule>> = callbackFlow {
        if (uid.isBlank()) {
            close(IllegalStateException("uid가 비어 있습니다."))
            return@callbackFlow
        }
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

    override suspend fun getReviewScheduleById(
        uid: String,
        scheduleId: String
    ): Result<ReviewSchedule> = runCatching {
        if (uid.isBlank()) error("uid가 비어 있습니다.")
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
        if (uid.isBlank()) error("uid가 비어 있습니다.")
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
        if (uid.isBlank()) error("uid가 비어 있습니다.")
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
}