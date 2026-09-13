package com.loorve.domain.usecase

import com.loorve.domain.model.CompletionResult
import com.loorve.domain.model.ReviewScheduleItem
import com.loorve.domain.model.ReviewStatus
import com.loorve.domain.notification.ReviewNotificationAdapter
import com.loorve.domain.notification.ReviewNotificationKind
import com.loorve.domain.notification.ReviewNotificationOutbox
import com.loorve.domain.repository.ExamRepository
import com.loorve.domain.repository.ReviewScheduleItemRepository
import com.loorve.domain.repository.ReviewSchedulingRepository
import com.loorve.domain.repository.StudyRecordRepository
import com.loorve.domain.review.*
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.first

enum class ReviewCompletionOutcome { EASY, SUCCESS, HARD, FAILED }

data class CompleteReviewResult(
    val applied: Boolean,
    val userMessage: String,
    val nextReviewDate: LocalDate? = null
)

class CompleteReviewWithReschedulingUseCase @Inject constructor(
    private val scheduleRepository: ReviewScheduleItemRepository,
    private val studyRecordRepository: StudyRecordRepository,
    private val examRepository: ExamRepository,
    private val schedulingRepository: ReviewSchedulingRepository,
    private val notificationAdapter: ReviewNotificationAdapter
) {
    suspend operator fun invoke(
        uid: String,
        reviewId: String,
        outcome: ReviewCompletionOutcome,
        today: LocalDate = LocalDate.now(ZoneId.of("Asia/Seoul"))
    ): Result<CompleteReviewResult> = runCatching {
        require(uid.isNotBlank() && reviewId.isNotBlank()) { "복습 식별자가 올바르지 않습니다." }
        val all = scheduleRepository.getAllScheduleItems(uid).getOrThrow()
        val current = all.firstOrNull { it.id == reviewId }
            ?: throw NoSuchElementException("복습 일정을 찾을 수 없습니다.")
        require(current.uid == uid) { "본인의 복습 일정만 완료할 수 있습니다." }
        require(current.status != ReviewStatus.COMPLETED) { "이미 완료된 복습 일정입니다." }
        val record = studyRecordRepository.getAllStudyRecords(uid).getOrThrow()
            .firstOrNull { it.id == current.studyRecordId }
            ?: throw NoSuchElementException("학습기록을 찾을 수 없습니다.")
        require(record.uid == uid) { "본인의 학습기록만 사용할 수 있습니다." }
        val exam = examRepository.getExamById(record.examId).first()
        require(exam.createdBy == uid) { "본인의 시험만 사용할 수 있습니다." }
        val zone = ZoneId.of(exam.timezone.ifBlank { "Asia/Seoul" })
        val examDate = java.time.Instant.ofEpochMilli(exam.examDate).atZone(zone).toLocalDate()
        val entries = all.map { it.toEntry(zone) }
        val completed = current.copy(
            status = ReviewStatus.COMPLETED,
            completionResult = if (outcome == ReviewCompletionOutcome.EASY ||
                outcome == ReviewCompletionOutcome.SUCCESS
            ) CompletionResult.REMEMBERED else CompletionResult.FORGOT,
            completedAt = System.currentTimeMillis(),
            outcome = outcome.name,
            planStatus = ReviewPlanStatus.COMPLETED
        )
        val rescheduled = ReviewSchedulingEngine.rescheduleAfterReviewOutcome(
            schedules = entries,
            completedReviewId = reviewId,
            outcome = outcome.toDomainOutcome(),
            today = today,
            exam = SchedulerExam(
                examId = exam.id, examName = exam.subjectName, examDate = examDate,
                timezone = zone, maxDailyReviewMinutes = exam.maxDailyReviewMinutes,
                finalReviewBufferDays = exam.finalReviewBufferDays
            )
        )
        val changed = rescheduled.schedules
            .filter { it.reviewId != reviewId }
            .mapNotNull { entry -> all.firstOrNull { it.id == entry.reviewId }?.let { old ->
                entry.toScheduleItem(old, uid, zone)
            } }
        val oldNext = all.filter { it.studyRecordId == current.studyRecordId && it.reviewDate > current.reviewDate }
            .minByOrNull { it.reviewDate }
        val newNext = changed.firstOrNull { it.id == oldNext?.id }
        val notifications = mutableListOf<ReviewNotificationOutbox>()
        if (oldNext != null && newNext != null && oldNext.reviewDate != newNext.reviewDate) {
            notifications += ReviewNotificationOutbox(
                id = "${oldNext.id}_CANCEL_${oldNext.reviewDate}",
                uid = uid, reviewId = oldNext.id, studyRecordId = oldNext.studyRecordId,
                kind = ReviewNotificationKind.CANCEL, scheduledDate = oldNext.date(zone),
                triggerAtMillis = 0L
            )
            notifications += newNext.notificationEvents(uid, zone, examDate)
        }
        schedulingRepository.completeReviewAndUpdateSchedules(
            uid, completed, changed, notifications
        ).getOrThrow()
        notifications.forEach {
            if (it.kind == ReviewNotificationKind.CANCEL) {
                notificationAdapter.cancel(it)
            } else {
                notificationAdapter.schedule(it)
            }
        }
        CompleteReviewResult(
            applied = rescheduled.applied,
            userMessage = rescheduled.reason,
            nextReviewDate = changed
                .firstOrNull { it.id == oldNext?.id }
                ?.reviewDate
                ?.let { it.toLocalDate(zone) }
        )
    }
}

private fun ReviewCompletionOutcome.toDomainOutcome() = when (this) {
    ReviewCompletionOutcome.EASY -> ReviewOutcome.EASY
    ReviewCompletionOutcome.SUCCESS -> ReviewOutcome.SUCCESS
    ReviewCompletionOutcome.HARD -> ReviewOutcome.DIFFICULT
    ReviewCompletionOutcome.FAILED -> ReviewOutcome.FAILURE
}

private fun ReviewScheduleItem.toEntry(zone: ZoneId) = ReviewScheduleEntry(
    reviewId = id, studyRecordId = studyRecordId, reviewIndex = reviewOrder,
    scheduledDate = date(zone), status = planStatus, priorityScore = priorityScore,
    estimatedReviewMinutes = estimatedReviewMinutes, recommendedMethod = recommendedMethod,
    isFinalReview = isFinalReview, originalScheduledDate = originalReviewDate.toLocalDate(zone)
)

private fun ReviewScheduleEntry.toScheduleItem(
    old: ReviewScheduleItem,
    uid: String,
    zone: ZoneId
) = old.copy(
    uid = uid,
    reviewDate = scheduledDate.atStartOfDay(zone).toInstant().toEpochMilli(),
    status = ReviewStatus.PENDING,
    planStatus = status,
    rescheduleReason = rescheduleReason,
    updatedAt = System.currentTimeMillis()
)

private fun ReviewScheduleItem.date(zone: ZoneId) = reviewDate.toLocalDate(zone)

private fun Long.toLocalDate(zone: ZoneId): LocalDate =
    java.time.Instant.ofEpochMilli(this).atZone(zone).toLocalDate()
