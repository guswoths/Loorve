package com.loorve.domain.repository

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.loorve.domain.model.ReviewSchedule
import com.loorve.domain.repository.ReviewScheduleRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReviewScheduleRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ReviewScheduleRepository {

    // Firestore 경로: users/{uid}/reviewSchedules
    private fun reviewSchedulesCollection(uid: String) =
        firestore.collection("users")
            .document(uid)
            .collection("reviewSchedules")

    private fun reviewScheduleDocument(uid: String, scheduleId: String) =
        reviewSchedulesCollection(uid).document(scheduleId)

    // ──────────────────────────────────────────────
    // CREATE
    // ──────────────────────────────────────────────

    override suspend fun createReviewSchedule(
        uid: String,
        schedule: ReviewSchedule
    ): Result<Unit> {
        return try {
            require(uid.isNotBlank()) { "uid는 비어 있을 수 없습니다." }
            require(schedule.originProgressId.isNotBlank()) { "originProgressId는 비어 있을 수 없습니다." }
            require(schedule.reviewDate > 0L) { "reviewDate는 유효한 epoch ms 값이어야 합니다." }
            require(schedule.reviewRound >= 1) { "reviewRound는 1 이상이어야 합니다." }

            val docRef = if (schedule.reviewScheduleId.isBlank()) {
                reviewSchedulesCollection(uid).document()
            } else {
                reviewScheduleDocument(uid, schedule.reviewScheduleId)
            }

            val data = buildScheduleMap(schedule)
            docRef.set(data).await()

            Log.d(TAG, "reviewSchedule 생성 완료: uid=$uid, scheduleId=${docRef.id}")
            Result.success(Unit)
        } catch (e: FirebaseFirestoreException) {
            Log.e(TAG, "Firestore reviewSchedule 생성 실패: code=${e.code}, uid=$uid", e)
            val message = when (e.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                    "저장 권한이 없습니다. Firestore 보안 규칙과 로그인 상태를 확인해주세요."
                FirebaseFirestoreException.Code.UNAVAILABLE ->
                    "서버에 연결할 수 없습니다. 네트워크 연결을 확인해주세요."
                else -> "복습 일정을 저장하지 못했습니다: ${e.message}"
            }
            Result.failure(IllegalStateException(message, e))
        } catch (e: IOException) {
            Log.e(TAG, "네트워크 오류로 reviewSchedule 생성 실패: uid=$uid", e)
            Result.failure(IllegalStateException("네트워크 연결을 확인해주세요.", e))
        } catch (e: Exception) {
            Log.e(TAG, "reviewSchedule 생성 실패: uid=$uid", e)
            Result.failure(e)
        }
    }

    // ──────────────────────────────────────────────
    // READ - 날짜 범위 Flow
    // ──────────────────────────────────────────────

    /**
     * @param startDate "yyyy-MM-dd" 형식 (KST)
     * @param endDate   "yyyy-MM-dd" 형식 (KST), 해당 날짜 포함
     *
     * Firestore 인덱스 필요: reviewDate ASC (단일 필드 인덱스로 충분)
     */
    override fun getReviewSchedulesByDateRange(
        uid: String,
        startDate: String,
        endDate: String
    ): Flow<List<ReviewSchedule>> = callbackFlow {
        if (uid.isBlank()) {
            close(IllegalArgumentException("uid는 비어 있을 수 없습니다."))
            return@callbackFlow
        }

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).apply {
            timeZone = TimeZone.getTimeZone("Asia/Seoul")
        }

        val startEpoch = runCatching { sdf.parse(startDate)?.time ?: 0L }.getOrDefault(0L)
        // endDate 자정 + 하루(86400000ms) - 1ms = 해당 날짜 끝까지 포함
        val endEpoch = runCatching { sdf.parse(endDate)?.time?.plus(86_400_000L - 1L) ?: 0L }
            .getOrDefault(0L)

        val registration = reviewSchedulesCollection(uid)
            .whereGreaterThanOrEqualTo("reviewDate", startEpoch)
            .whereLessThanOrEqualTo("reviewDate", endEpoch)
            .orderBy("reviewDate", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents?.mapNotNull { it.toReviewSchedule() } ?: emptyList())
            }

        awaitClose { registration.remove() }
    }

    // ──────────────────────────────────────────────
    // READ - 오늘 복습 일정 Flow
    // ──────────────────────────────────────────────

    /**
     * 오늘(KST 자정 ~ 오늘 끝) 에 해당하는 모든 일정 반환
     */
    override fun getTodayReviewSchedules(uid: String): Flow<List<ReviewSchedule>> = callbackFlow {
        if (uid.isBlank()) {
            close(IllegalArgumentException("uid는 비어 있을 수 없습니다."))
            return@callbackFlow
        }

        val todayStart = getTodayStartEpochMs()
        val todayEnd = todayStart + 86_400_000L - 1L

        val registration = reviewSchedulesCollection(uid)
            .whereGreaterThanOrEqualTo("reviewDate", todayStart)
            .whereLessThanOrEqualTo("reviewDate", todayEnd)
            .orderBy("reviewDate", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents?.mapNotNull { it.toReviewSchedule() } ?: emptyList())
            }

        awaitClose { registration.remove() }
    }

    // ──────────────────────────────────────────────
    // READ - 기한 초과 & 미완료 Flow
    // ──────────────────────────────────────────────

    /**
     * reviewDate < 오늘 자정 && isCompleted == false
     *
     * ⚠️ Firestore 복합 인덱스 필요:
     *   Collection: reviewSchedules
     *   Fields: isCompleted ASC, reviewDate ASC
     *   Firebase Console > Firestore > 인덱스 탭에서 생성 필요
     */
    override fun getOverdueAndIncompleteSchedules(uid: String): Flow<List<ReviewSchedule>> =
        callbackFlow {
            if (uid.isBlank()) {
                close(IllegalArgumentException("uid는 비어 있을 수 없습니다."))
                return@callbackFlow
            }

            val todayStart = getTodayStartEpochMs()

            val registration = reviewSchedulesCollection(uid)
                .whereEqualTo("isCompleted", false)
                .whereLessThan("reviewDate", todayStart)
                .orderBy("reviewDate", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    trySend(
                        snapshot?.documents?.mapNotNull { it.toReviewSchedule() } ?: emptyList()
                    )
                }

            awaitClose { registration.remove() }
        }

    // ──────────────────────────────────────────────
    // UPDATE - 완료 처리
    // ──────────────────────────────────────────────

    override suspend fun completeReviewSchedule(
        uid: String,
        scheduleId: String
    ): Result<Unit> {
        return try {
            require(uid.isNotBlank()) { "uid는 비어 있을 수 없습니다." }
            require(scheduleId.isNotBlank()) { "scheduleId는 비어 있을 수 없습니다." }

            val now = Timestamp.now()
            reviewScheduleDocument(uid, scheduleId)
                .update(
                    mapOf(
                        "isCompleted" to true,
                        "updatedAt" to now
                    )
                )
                .await()

            Log.d(TAG, "reviewSchedule 완료 처리: uid=$uid, scheduleId=$scheduleId")
            Result.success(Unit)
        } catch (e: FirebaseFirestoreException) {
            Log.e(TAG, "Firestore reviewSchedule 완료 처리 실패: code=${e.code}", e)
            val message = when (e.code) {
                FirebaseFirestoreException.Code.NOT_FOUND ->
                    "해당 복습 일정을 찾을 수 없습니다."
                FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                    "수정 권한이 없습니다."
                else -> "복습 일정 완료 처리에 실패했습니다: ${e.message}"
            }
            Result.failure(IllegalStateException(message, e))
        } catch (e: Exception) {
            Log.e(TAG, "reviewSchedule 완료 처리 실패: uid=$uid, scheduleId=$scheduleId", e)
            Result.failure(e)
        }
    }

    // ──────────────────────────────────────────────
    // UPDATE - 완료 여부 토글
    // ──────────────────────────────────────────────

    override suspend fun updateReviewCompletion(
        uid: String,
        scheduleId: String,
        isCompleted: Boolean
    ): Result<Unit> {
        return try {
            require(uid.isNotBlank()) { "uid는 비어 있을 수 없습니다." }
            require(scheduleId.isNotBlank()) { "scheduleId는 비어 있을 수 없습니다." }

            val now = Timestamp.now()
            reviewScheduleDocument(uid, scheduleId)
                .update(
                    mapOf(
                        "isCompleted" to isCompleted,
                        "updatedAt" to now
                    )
                )
                .await()

            Log.d(TAG, "reviewSchedule 완료 토글: uid=$uid, scheduleId=$scheduleId, isCompleted=$isCompleted")
            Result.success(Unit)
        } catch (e: FirebaseFirestoreException) {
            Log.e(TAG, "Firestore reviewSchedule 토글 실패: code=${e.code}", e)
            val message = when (e.code) {
                FirebaseFirestoreException.Code.NOT_FOUND ->
                    "해당 복습 일정을 찾을 수 없습니다."
                FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                    "수정 권한이 없습니다."
                else -> "복습 일정 완료 처리에 실패했습니다: ${e.message}"
            }
            Result.failure(IllegalStateException(message, e))
        } catch (e: Exception) {
            Log.e(TAG, "reviewSchedule 토글 실패: uid=$uid, scheduleId=$scheduleId", e)
            Result.failure(e)
        }
    }

    // ──────────────────────────────────────────────
    // BATCH SAVE
    // ──────────────────────────────────────────────

    override suspend fun saveReviewSchedules(
        uid: String,
        schedules: List<ReviewSchedule>
    ): Result<Unit> {
        return try {
            require(uid.isNotBlank()) { "uid는 비어 있을 수 없습니다." }
            require(schedules.isNotEmpty()) { "schedules는 비어 있을 수 없습니다." }

            val batch = firestore.batch()
            schedules.forEach { schedule ->
                val docRef = if (schedule.reviewScheduleId.isBlank()) {
                    reviewSchedulesCollection(uid).document()
                } else {
                    reviewScheduleDocument(uid, schedule.reviewScheduleId)
                }
                batch.set(docRef, buildScheduleMap(schedule))
            }
            batch.commit().await()

            Log.d(TAG, "reviewSchedules 배치 저장 완료: uid=$uid, count=${schedules.size}")
            Result.success(Unit)
        } catch (e: FirebaseFirestoreException) {
            Log.e(TAG, "Firestore 배치 저장 실패: code=${e.code}, uid=$uid", e)
            val message = when (e.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                    "저장 권한이 없습니다. Firestore 보안 규칙과 로그인 상태를 확인해주세요."
                FirebaseFirestoreException.Code.UNAVAILABLE ->
                    "서버에 연결할 수 없습니다. 네트워크 연결을 확인해주세요."
                else -> "복습 일정 배치 저장 실패: ${e.message}"
            }
            Result.failure(IllegalStateException(message, e))
        } catch (e: Exception) {
            Log.e(TAG, "reviewSchedules 배치 저장 실패: uid=$uid", e)
            Result.failure(e)
        }
    }

    // ──────────────────────────────────────────────
    // DELETE
    // ──────────────────────────────────────────────

    override suspend fun deleteReviewSchedule(
        uid: String,
        scheduleId: String
    ): Result<Unit> {
        return try {
            require(uid.isNotBlank()) { "uid는 비어 있을 수 없습니다." }
            require(scheduleId.isNotBlank()) { "scheduleId는 비어 있을 수 없습니다." }

            reviewScheduleDocument(uid, scheduleId).delete().await()

            Log.d(TAG, "reviewSchedule 삭제 완료: uid=$uid, scheduleId=$scheduleId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "reviewSchedule 삭제 실패: uid=$uid, scheduleId=$scheduleId", e)
            Result.failure(e)
        }
    }

    // ──────────────────────────────────────────────
    // CREATE (Batch) - 배치 저장
    // ──────────────────────────────────────────────

    override suspend fun saveReviewSchedules(
        uid: String,
        schedules: List<ReviewSchedule>
    ): Result<Unit> {
        return try {
            require(uid.isNotBlank()) { "uid는 비어 있을 수 없습니다." }
            require(schedules.isNotEmpty()) { "schedules는 비어 있을 수 없습니다." }
            // Firestore WriteBatch 최대 500건 제한 방어
            require(schedules.size <= 500) { "한 번에 저장 가능한 스케줄은 최대 500개입니다." }

            val batch = firestore.batch()
            schedules.forEach { schedule ->
                val docRef = if (schedule.reviewScheduleId.isBlank()) {
                    reviewSchedulesCollection(uid).document()
                } else {
                    reviewScheduleDocument(uid, schedule.reviewScheduleId)
                }
                batch.set(docRef, buildScheduleMap(schedule))
            }
            batch.commit().await()

            Log.d(TAG, "reviewSchedules 배치 저장 완료: uid=$uid, count=${schedules.size}")
            Result.success(Unit)
        } catch (e: FirebaseFirestoreException) {
            Log.e(TAG, "Firestore 배치 저장 실패: code=${e.code}, uid=$uid", e)
            val message = when (e.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                    "저장 권한이 없습니다. Firestore 보안 규칙을 확인해주세요."
                FirebaseFirestoreException.Code.UNAVAILABLE ->
                    "서버에 연결할 수 없습니다."
                else -> "복습 일정 배치 저장 실패: ${e.message}"
            }
            Result.failure(IllegalStateException(message, e))
        } catch (e: Exception) {
            Log.e(TAG, "배치 저장 실패: uid=$uid", e)
            Result.failure(e)
        }
    }

    // ──────────────────────────────────────────────
    // Private Helpers
    // ──────────────────────────────────────────────

    private fun buildScheduleMap(schedule: ReviewSchedule): Map<String, Any> {
        val now = Timestamp.now()

        fun Long.toTimestamp(): Timestamp =
            Timestamp(this / 1_000, ((this % 1_000) * 1_000_000).toInt())

        val createdAt = if (schedule.createdAt > 0L) schedule.createdAt.toTimestamp() else now
        val updatedAt = now

        return mapOf(
            "originProgressId" to schedule.originProgressId,
            "reviewDate" to schedule.reviewDate, // epoch ms (Long) — 날짜 범위 쿼리용
            "reviewRound" to schedule.reviewRound,
            "isCompleted" to schedule.isCompleted,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt
        )
    }

    /**
     * KST 기준 오늘 자정 epoch ms 반환
     */
    private fun getTodayStartEpochMs(): Long {
        val kst = java.util.Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul")).apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return kst.timeInMillis
    }

    private fun DocumentSnapshot.toReviewSchedule(): ReviewSchedule? {
        val data = data ?: return null
        return ReviewSchedule(
            reviewScheduleId = id,
            originProgressId = data["originProgressId"] as? String ?: "",
            reviewDate = (data["reviewDate"] as? Number)?.toLong() ?: 0L,
            reviewRound = (data["reviewRound"] as? Number)?.toInt() ?: 1,
            isCompleted = data["isCompleted"] as? Boolean ?: false,
            createdAt = getTimestamp("createdAt")?.toDate()?.time ?: 0L,
            updatedAt = getTimestamp("updatedAt")?.toDate()?.time ?: 0L
        )
    }

    companion object {
        private const val TAG = "ReviewScheduleRepository"
    }
}
