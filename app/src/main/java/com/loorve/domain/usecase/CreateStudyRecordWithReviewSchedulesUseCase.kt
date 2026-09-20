package com.loorve.domain.usecase

import com.loorve.domain.model.ReviewScheduleItem
import com.loorve.domain.model.ReviewStatus
import com.loorve.domain.model.StudyRecord
import com.loorve.domain.notification.ReviewNotificationKind
import com.loorve.domain.notification.ReviewNotificationOutbox
import com.loorve.domain.notification.ReviewNotificationAdapter
import com.loorve.domain.repository.ReviewScheduleItemRepository
import com.loorve.domain.repository.ReviewBlockRepository
import com.loorve.domain.repository.ReviewSchedulingRepository
import com.loorve.domain.repository.StudyRecordRepository
import com.loorve.domain.review.*
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject

data class CreateStudyRecordRequest(
    val examId: String,
    val title: String,
    val content: String,
    val studiedAt: LocalDate,
    val difficulty: ReviewDifficulty = ReviewDifficulty.MEDIUM,
    val importance: ReviewImportance = ReviewImportance.NORMAL,
    val initialMastery: Int? = null,
    val estimatedReviewMinutes: Int = 15,
    val optionalMinReviewCount: Int? = null,
    val blockId: String = ""
)

data class CreateStudyRecordResult(
    val studyRecordId: String,
    val schedules: List<ReviewScheduleItem>,
    val status: ReviewPlanStatus,
    val userMessage: String,
    val lastReviewDate: LocalDate,
    val compressed: Boolean = false,
    val generationOutcome: ScheduleGenerationOutcome = ScheduleGenerationOutcome.FULL
)

class CreateStudyRecordWithReviewSchedulesUseCase @Inject constructor(
    private val reviewBlockRepository: ReviewBlockRepository,
    private val scheduleRepository: ReviewScheduleItemRepository,
    private val studyRecordRepository: StudyRecordRepository,
    private val schedulingRepository: ReviewSchedulingRepository,
    private val notificationAdapter: ReviewNotificationAdapter
) {
    suspend operator fun invoke(
        uid: String,
        request: CreateStudyRecordRequest,
        today: LocalDate = LocalDate.now(ZoneId.of("Asia/Seoul"))
    ): Result<CreateStudyRecordResult> = runCatching {
        require(uid.isNotBlank()) { "로그인이 필요합니다." }
        require(request.content.isNotBlank()) { "학습 내용은 비어 있을 수 없습니다." }
        require(request.content.length <= 20_000) { "학습 내용은 20,000자 이내여야 합니다." }
        require(request.estimatedReviewMinutes > 0) { "예상 복습 시간은 양수여야 합니다." }
        val block = reviewBlockRepository.getReviewBlock(uid, request.blockId).getOrThrow()
            ?: error("복습 블록을 찾을 수 없습니다.")
        require(block.uid == uid) { "본인의 복습 블록에만 학습기록을 추가할 수 있습니다." }
        val zone = ZoneId.of("Asia/Seoul")
        val examDate = if (block.examDate > 0L) {
            java.time.Instant.ofEpochMilli(block.examDate).atZone(zone).toLocalDate()
        } else {
            request.studiedAt
        }
        val hasLinkedExam = request.examId.isNotBlank() || block.examDate > 0L
        val hasExamDate = block.examDate > 0L
        val schedulerExam = SchedulerExam(
            examId = request.examId,
            examName = block.examName.ifBlank { block.title },
            examDate = examDate,
            timezone = zone,
            maxDailyReviewMinutes = null,
            finalReviewBufferDays = 0
        )
        val recordId = UUID.randomUUID().toString()
        val record = SchedulerStudyRecord(
            studyRecordId = recordId,
            studiedAtDate = request.studiedAt,
            title = request.title.trim(),
            content = request.content,
            difficulty = request.difficulty,
            importance = request.importance,
            estimatedReviewMinutes = request.estimatedReviewMinutes,
            initialMastery = request.initialMastery,
            optionalMinReviewCount = request.optionalMinReviewCount
        )
        val generated = if (!hasLinkedExam) {
            SchedulingResult(
                schedules = emptyList(),
                status = ReviewPlanStatus.INSUFFICIENT_WINDOW,
                warningMessage = "생성불가! 이 학습기록에 연결된 시험이 없습니다.",
                outcome = ScheduleGenerationOutcome.NOT_GENERATED,
                lastReviewDate = request.studiedAt,
                effectiveStudyDays = 0,
                targetReviewCount = 0,
                generatedReviewCount = 0,
                compressed = false,
                availableDaysForNewLearning = 0
            )
        } else if (!hasExamDate) {
            SchedulingResult(
                schedules = emptyList(),
                status = ReviewPlanStatus.INSUFFICIENT_WINDOW,
                warningMessage = "생성불가! 연결된 시험일이 설정되지 않았습니다.",
                outcome = ScheduleGenerationOutcome.NOT_GENERATED,
                lastReviewDate = request.studiedAt,
                effectiveStudyDays = 0,
                targetReviewCount = 0,
                generatedReviewCount = 0,
                compressed = false,
                availableDaysForNewLearning = 0
            )
        } else {
            ReviewSchedulingEngine.createReviewSchedules(
                record, schedulerExam, today,
                SchedulerConfig(finalReviewBufferDays = 0),
                ReviewNotificationPlan(timezone = zone),
                customIntervalDays = block.customIntervalDays
            )
        }
        val generationWarning = generated.warningMessage
            ?: generated.schedules.takeIf { it.isEmpty() }?.let {
                "복습 일정 생성 불가: 현재 학습일, 시험일, 복습 설정으로 생성 가능한 날짜가 없습니다. " +
                    "시험일과 복습 간격을 확인해주세요."
            }
        val generatedEntries = generated.schedules.map {
            it.toScheduleItem(uid, request.blockId, zone, request.title.ifBlank { request.content.take(20) })
        }
        val existingRecordIds = studyRecordRepositoryFor(uid, request.examId)
        val existingItems = scheduleRepository.getAllScheduleItems(uid).getOrThrow()
            .filter { it.studyRecordId in existingRecordIds }
        val existingEntries = existingItems.map { it.toEntry(zone) }
        val rebalanced = ReviewSchedulingEngine.rebalanceDailyLoad(
            schedules = existingEntries + generatedEntries.map { it.toEntry(zone) },
            exam = schedulerExam,
            reviewStartDate = today.plusDays(1),
            config = SchedulerConfig(finalReviewBufferDays = 0)
        )
        val finalItems = if (generatedEntries.isEmpty()) {
            existingItems
        } else {
            rebalanced.schedules.mapNotNull { entry ->
                existingItems.firstOrNull { it.id == entry.reviewId }?.let { old ->
                    entry.toScheduleItem(old, uid, zone)
                } ?: generatedEntries.firstOrNull { it.id == entry.reviewId }?.let { generated ->
                    entry.toScheduleItem(generated, uid, zone)
                }
            }
        }
        val studyRecord = StudyRecord(
            id = recordId, uid = uid, blockId = request.blockId, examId = request.examId,
            title = request.title.trim().ifBlank { request.content.take(20) },
            content = request.content,
            learningDate = request.studiedAt.atStartOfDay(zone).toInstant().toEpochMilli(),
            examDate = block.examDate, plannedReviewCount = generated.generatedReviewCount,
            recommendedCompletionDate = generated.lastReviewDate.atStartOfDay(zone).toInstant().toEpochMilli(),
            difficulty = request.difficulty, importance = request.importance,
            initialMastery = request.initialMastery,
            estimatedReviewMinutes = request.estimatedReviewMinutes,
            optionalMinReviewCount = request.optionalMinReviewCount,
            createdAt = System.currentTimeMillis()
        )
        val newItems = finalItems.filter { it.studyRecordId == recordId }
        val movedExistingItems = finalItems.filter { it.studyRecordId != recordId }
        val events = newItems.flatMap { it.notificationEvents(uid, zone, examDate) }.toMutableList()
        movedExistingItems.forEach { moved ->
            val old = existingItems.firstOrNull { it.id == moved.id }
            if (old != null && old.reviewDate != moved.reviewDate) {
                events += ReviewNotificationOutbox(
                    id = "${moved.id}_CANCEL_${old.reviewDate}",
                    uid = uid, reviewId = moved.id, studyRecordId = moved.studyRecordId,
                    kind = ReviewNotificationKind.CANCEL, scheduledDate = old.reviewDate.toLocalDate(zone),
                    triggerAtMillis = 0L
                )
                events += moved.notificationEvents(uid, zone, examDate)
            }
        }
        val resultStatus = if (rebalanced.status == ReviewPlanStatus.OVERLOADED_UNRESOLVED) {
            ReviewPlanStatus.OVERLOADED_UNRESOLVED
        } else {
            generated.status
        }
        schedulingRepository.saveStudyRecordWithSchedules(
            uid, studyRecord, finalItems, events
        ).getOrThrow()
        events.forEach {
            if (it.kind == ReviewNotificationKind.CANCEL) {
                notificationAdapter.cancel(it)
            } else {
                notificationAdapter.schedule(it)
            }
        }
        val message = rebalanced.warningMessage ?: generationWarning
            ?: "복습 ${generated.generatedReviewCount}회가 생성되었습니다. 첫 복습일은 ${generated.schedules.firstOrNull()?.scheduledDate ?: "-"}이고 마지막 복습일은 ${generated.schedules.lastOrNull()?.scheduledDate ?: generated.lastReviewDate}입니다."
        CreateStudyRecordResult(
            recordId,
            newItems,
            resultStatus,
            message,
            generated.lastReviewDate,
            compressed = generated.compressed || rebalanced.schedules.any {
                it.status == ReviewPlanStatus.RESCHEDULED
            },
            generationOutcome = generated.outcome
        )
    }

    private suspend fun studyRecordRepositoryFor(uid: String, examId: String): Set<String> =
        emptySet<String>() + studyRecordRepository.getAllStudyRecords(uid).getOrThrow()
            .filter { it.examId == examId }.map { it.id }
}

private fun ReviewScheduleEntry.toScheduleItem(
    uid: String,
    blockId: String,
    zone: ZoneId,
    title: String
) =
    ReviewScheduleItem(
        id = reviewId, studyRecordId = studyRecordId, blockId = blockId, uid = uid,
        title = title, reviewDate = scheduledDate.atStartOfDay(zone).toInstant().toEpochMilli(),
        originalReviewDate = originalScheduledDate.atStartOfDay(zone).toInstant().toEpochMilli(),
        reviewOrder = reviewIndex, status = ReviewStatus.PENDING, planStatus = status,
        priorityScore = priorityScore, estimatedReviewMinutes = estimatedReviewMinutes,
        recommendedMethod = recommendedMethod, isFinalReview = isFinalReview
    )

private fun ReviewScheduleEntry.toScheduleItem(
    old: ReviewScheduleItem,
    uid: String,
    zone: ZoneId
) = old.copy(
    uid = uid,
    reviewDate = scheduledDate.atStartOfDay(zone).toInstant().toEpochMilli(),
    status = old.status,
    planStatus = status,
    priorityScore = priorityScore,
    estimatedReviewMinutes = estimatedReviewMinutes,
    recommendedMethod = recommendedMethod,
    isFinalReview = isFinalReview,
    rescheduleReason = rescheduleReason,
    updatedAt = System.currentTimeMillis()
)

private fun ReviewScheduleItem.toEntry(zone: ZoneId) = ReviewScheduleEntry(
    reviewId = id,
    studyRecordId = studyRecordId,
    reviewIndex = reviewOrder,
    scheduledDate = reviewDate.toLocalDate(zone),
    status = planStatus,
    priorityScore = priorityScore,
    estimatedReviewMinutes = estimatedReviewMinutes,
    recommendedMethod = recommendedMethod,
    isFinalReview = isFinalReview,
    originalScheduledDate = originalReviewDate.toLocalDate(zone)
)

fun ReviewScheduleItem.notificationEvents(
    uid: String,
    zone: ZoneId,
    examDate: LocalDate
): List<ReviewNotificationOutbox> {
    if (reviewDate.toLocalDate(zone) >= examDate) return emptyList()
    if (planStatus == ReviewPlanStatus.CRAM_MODE_REQUIRED ||
        planStatus == ReviewPlanStatus.INSUFFICIENT_WINDOW ||
        planStatus == ReviewPlanStatus.OVERLOADED_UNRESOLVED
    ) return emptyList()
    fun event(kind: ReviewNotificationKind, hour: Int, minute: Int) =
        ReviewNotificationOutbox(
            id = "${id}_${kind.name}", uid = uid, reviewId = id, studyRecordId = studyRecordId,
            kind = kind, scheduledDate = reviewDate.toLocalDate(zone),
            triggerAtMillis = reviewDate.toLocalDate(zone).atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()
        )
    return if (isFinalReview) listOf(
        event(ReviewNotificationKind.FINAL_REVIEW_MORNING, 9, 0),
        event(ReviewNotificationKind.FINAL_REVIEW_EVENING, 18, 0)
    ) else listOf(
        event(ReviewNotificationKind.NORMAL_REVIEW, 9, 0),
        event(ReviewNotificationKind.INCOMPLETE_REVIEW, 19, 0)
    )
}

private fun Long.toLocalDate(zone: ZoneId): LocalDate =
    java.time.Instant.ofEpochMilli(this).atZone(zone).toLocalDate()
