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

    // ── 기존 단일 파라미터 오버로드 (변경 없음) ──────────────────────────
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

            val scheduleData = buildScheduleMap(reviewSchedule)

            firestore
                .collection("users")
                .document(reviewSchedule.userId)
                .collection("reviewSchedules")
                .document(reviewSchedule.scheduleId)
                .set(scheduleData)
                .await()
        }
    }

    // ── 추가: uid 명시 오버로드 (SaveProgressAndScheduleUseCase 지원) ──
    override suspend fun saveReviewSchedule(
        uid: String,
        reviewSchedule: ReviewSchedule
    ): Result<Unit> {
        return runCatching {
            require(uid.isNotBlank()) { "사용자 ID가 비어 있습니다." }
            require(reviewSchedule.scheduleId.isNotBlank()) { "복습 일정 ID가 비어 있습니다." }

            // uid가 명시적으로 전달되므로 해당 경로로 저장
            val scheduleData = buildScheduleMap(reviewSchedule.copy(userId = uid))

            firestore
                .collection("users")
                .document(uid)
                .collection("reviewSchedules")
                .document(reviewSchedule.scheduleId)
                .set(scheduleData)
                .await()
        }
    }

    // ── 추가: 미완료 예정 일정 목록 조회 (SignOutUseCase / BootCompletedReceiver 지원) ──
    override suspend fun getUpcomingIncompleteSchedules(
        uid: String,
        fromMillis: Long
    ): Result<List<ReviewSchedule>> {
        return runCatching {
            require(uid.isNotBlank()) { "사용자 ID가 비어 있습니다." }

            val snapshot = firestore
                .collection("users")
                .document(uid)
                .collection("reviewSchedules")
                .whereEqualTo("isCompleted", false)
                .whereGreaterThanOrEqualTo("reviewDate", fromMillis)
                .orderBy("reviewDate", Query.Direction.ASCENDING)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                doc.toObject(ReviewSchedule::class.java)
            }
        }
    }

    // ── 추가: 단건 조회 (UpdateReviewCompletionUseCase 지원) ─────────────
    override suspend fun getReviewScheduleById(
        uid: String,
        scheduleId: String
    ): Result<ReviewSchedule?> {
        return runCatching {
            require(uid.isNotBlank()) { "사용자 ID가 비어 있습니다." }
            require(scheduleId.isNotBlank()) { "복습 일정 ID가 비어 있습니다." }

            val doc = firestore
                .collection("users")
                .document(uid)
                .collection("reviewSchedules")
                .document(scheduleId)
                .get()
                .await()

            doc.toObject(ReviewSchedule::class.java)
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

    // ── private 헬퍼 ─────────────────────────────────────────────────────

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

    // ── 중복 제거: Firestore 저장용 Map을 한 곳에서 관리 ─────────────────
    private fun buildScheduleMap(schedule: ReviewSchedule): HashMap<String, Any?> {
        return hashMapOf(
            "scheduleId"      to schedule.scheduleId,
            "blockId"         to schedule.blockId,
            "userId"          to schedule.userId,
            "originProgressId" to schedule.originProgressId,
            "title"           to schedule.title,
            "reviewDate"      to schedule.reviewDate,
            "reviewDateText"  to schedule.reviewDateText,
            "reviewOrder"     to schedule.reviewOrder,
            "scheduleType"    to schedule.scheduleType,
            "isCompleted"     to schedule.isCompleted,
            "createdAt"       to schedule.createdAt,
            "updatedAt"       to schedule.updatedAt
        )
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