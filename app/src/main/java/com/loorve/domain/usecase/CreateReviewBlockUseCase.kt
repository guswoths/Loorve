package com.loorve.domain.usecase

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.WriteBatch
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.inject.Inject

data class CreateReviewBlockRequest(
    val uid: String,
    val examName: String,
    val examDateMillis: Long,
    val cycleOption: Int
)

class CreateReviewBlockUseCase @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    suspend operator fun invoke(request: CreateReviewBlockRequest): Result<Unit> {
        return runCatching {
            require(request.uid.isNotBlank()) {
                "로그인 정보를 찾을 수 없습니다."
            }
            require(request.examName.isNotBlank()) {
                "시험 이름을 입력해주세요."
            }

            val zoneId = ZoneId.of("Asia/Seoul")
            val today = LocalDate.now(zoneId)
            val examDate = Instant.ofEpochMilli(request.examDateMillis)
                .atZone(zoneId)
                .toLocalDate()

            require(!examDate.isBefore(today)) {
                "시험 종료일은 오늘 이후 날짜를 선택해주세요."
            }

            val blockId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()

            val blockRef = firestore
                .collection("users")
                .document(request.uid)
                .collection("reviewBlocks")
                .document(blockId)

            val reviewDates = createReviewDates(
                today = today,
                examDate = examDate,
                cycleOption = request.cycleOption
            )

            val batch = firestore.batch()

            batch.set(
                blockRef,
                hashMapOf(
                    "blockId" to blockId,
                    "uid" to request.uid,
                    "date" to examDate.toString(),
                    "title" to request.examName.trim(),
                    "description" to createCycleDescription(request.cycleOption),
                    "isCompleted" to false,
                    "createdAt" to now,
                    "updatedAt" to now
                )
            )

            reviewDates.forEachIndexed { index, reviewDate ->
                val scheduleId = UUID.randomUUID().toString()

                val scheduleRef = firestore
                    .collection("users")
                    .document(request.uid)
                    .collection("reviewSchedules")
                    .document(scheduleId)

                batch.set(
                    scheduleRef,
                    createScheduleData(
                        scheduleId = scheduleId,
                        blockId = blockId,
                        uid = request.uid,
                        examName = request.examName.trim(),
                        reviewDate = reviewDate,
                        reviewOrder = index + 1,
                        cycleOption = request.cycleOption,
                        createdAt = now
                    )
                )
            }

            batch.commit().await()
        }
    }

    private fun createReviewDates(
        today: LocalDate,
        examDate: LocalDate,
        cycleOption: Int
    ): List<LocalDate> {
        val standardIntervals = when (cycleOption) {
            1 -> listOf(1L, 3L, 7L)
            else -> listOf(1L, 3L, 7L, 14L, 30L)
        }

        val daysUntilExam = ChronoUnit.DAYS.between(today, examDate)

        return standardIntervals
            .map { interval ->
                val scaledInterval = if (daysUntilExam >= 30L) {
                    interval
                } else {
                    val ratio = daysUntilExam.toDouble() / 30.0
                    (interval * ratio).toLong().coerceAtLeast(1L)
                }

                today.plusDays(scaledInterval).coerceAtMost(examDate)
            }
            .distinct()
            .sorted()
    }

    private fun createCycleDescription(cycleOption: Int): String {
        return when (cycleOption) {
            1 -> "직접 세팅 복습 주기"
            else -> "에빙하우스 복습 주기"
        }
    }

    private fun createScheduleData(
        scheduleId: String,
        blockId: String,
        uid: String,
        examName: String,
        reviewDate: LocalDate,
        reviewOrder: Int,
        cycleOption: Int,
        createdAt: Long
    ): HashMap<String, Any> {
        return hashMapOf(
            "scheduleId" to scheduleId,
            "blockId" to blockId,
            "userId" to uid,
            "title" to examName,
            "reviewDate" to reviewDate
                .atStartOfDay(ZoneId.of("Asia/Seoul"))
                .toInstant()
                .toEpochMilli(),
            "reviewDateText" to reviewDate.toString(),
            "reviewOrder" to reviewOrder,
            "scheduleType" to if (cycleOption == 0) "EBBINGHAUS" else "CUSTOM",
            "isCompleted" to false,
            "createdAt" to createdAt,
            "updatedAt" to createdAt
        )
    }
}