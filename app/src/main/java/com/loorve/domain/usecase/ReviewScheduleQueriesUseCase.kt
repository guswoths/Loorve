package com.loorve.domain.usecase

import com.loorve.domain.model.ReviewScheduleItem
import com.loorve.domain.model.ReviewStatus
import com.loorve.domain.repository.ReviewScheduleItemRepository
import com.loorve.domain.repository.StudyRecordRepository
import com.loorve.domain.review.ReviewPlanStatus
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class ReviewCalendarResult(
    val examDate: LocalDate,
    val daysRemaining: Long,
    val lastScheduledReviewDate: LocalDate?,
    val schedules: List<ReviewScheduleItem>
)

class ReviewScheduleQueriesUseCase @Inject constructor(
    private val scheduleRepository: ReviewScheduleItemRepository,
    private val studyRecordRepository: StudyRecordRepository
) {
    suspend fun today(
        uid: String,
        today: LocalDate = LocalDate.now(ZoneId.of("Asia/Seoul"))
    ): Result<List<ReviewScheduleItem>> = all(uid).map {
        it.filter { item -> item.reviewDate.toLocalDate() == today && item.status != ReviewStatus.COMPLETED }
    }

    suspend fun examCalendar(
        uid: String,
        examId: String,
        examDate: LocalDate
    ): Result<ReviewCalendarResult> = runCatching {
        val records = studyRecordRepository.getAllStudyRecords(uid).getOrThrow()
            .filter { it.examId == examId }
            .map { it.id }
            .toSet()
        val schedules = all(uid).getOrThrow().filter { it.studyRecordId in records }
        ReviewCalendarResult(
            examDate = examDate,
            daysRemaining = java.time.temporal.ChronoUnit.DAYS.between(
                LocalDate.now(ZoneId.of("Asia/Seoul")), examDate
            ),
            lastScheduledReviewDate = schedules
                .filter { it.status != ReviewStatus.COMPLETED }
                .maxOfOrNull { it.reviewDate.toLocalDate() },
            schedules = schedules.sortedBy { it.reviewDate }
        )
    }

    suspend fun timeline(uid: String, studyRecordId: String): Result<List<ReviewScheduleItem>> =
        scheduleRepository.getSchedulesByStudyRecord(uid, studyRecordId)
            .map { it.sortedBy { item -> item.reviewDate } }

    suspend fun byStatus(
        uid: String,
        status: ReviewPlanStatus
    ): Result<List<ReviewScheduleItem>> = all(uid).map {
        it.filter { item -> item.planStatus == status }
    }

    private suspend fun all(uid: String): Result<List<ReviewScheduleItem>> =
        scheduleRepository.getAllScheduleItems(uid)

    private fun Long.toLocalDate(): LocalDate =
        java.time.Instant.ofEpochMilli(this).atZone(ZoneId.of("Asia/Seoul")).toLocalDate()
}
