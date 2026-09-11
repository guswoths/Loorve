package com.loorve.domain.review

import java.time.LocalDate
import java.time.ZoneId

enum class ReviewDifficulty { EASY, MEDIUM, HARD }
enum class ReviewImportance { LOW, NORMAL, HIGH }

enum class ReviewPlanStatus {
    SCHEDULED,
    COMPLETED,
    MISSED,
    RESCHEDULED,
    CRAM_MODE_REQUIRED,
    INSUFFICIENT_WINDOW,
    OVERLOADED_UNRESOLVED
}

enum class ExistingRecordInsufficiencyPolicy {
    ALLOW_WITH_CRAM_WARNING,
    BLOCK
}

enum class ReviewOutcome {
    SUCCESS,
    EASY,
    DIFFICULT,
    FAILURE
}

data class SchedulerExam(
    val examId: String = "",
    val examName: String = "",
    val examDate: LocalDate,
    val timezone: ZoneId = ZoneId.of("Asia/Seoul"),
    // 기존 옵션과 관계없이 시험일은 항상 자동 정규 복습에서 제외합니다.
    val allowReviewOnDayBeforeExam: Boolean = false,
    val maxDailyReviewItems: Int? = null,
    val maxDailyReviewMinutes: Int? = null,
    val finalReviewBufferDays: Int = 1
)

data class SchedulerStudyRecord(
    val studyRecordId: String,
    val studiedAtDate: LocalDate,
    val subjectId: String = "",
    val title: String = "",
    val content: String = "",
    val difficulty: ReviewDifficulty = ReviewDifficulty.MEDIUM,
    val importance: ReviewImportance = ReviewImportance.NORMAL,
    val estimatedReviewMinutes: Int = 15,
    val isCompleted: Boolean = true,
    val initialMastery: Int? = null,
    val optionalMinReviewCount: Int? = null
)

data class SchedulerConfig(
    val baseIntervals: List<Int> = listOf(1, 3, 7, 14, 30, 60, 120),
    val finalReviewBufferDays: Int = 1,
    val existingRecordInsufficiencyPolicy: ExistingRecordInsufficiencyPolicy =
        ExistingRecordInsufficiencyPolicy.ALLOW_WITH_CRAM_WARNING,
    val difficultIntervalFactor: Double = 0.5,
    val failureIntervalFactor: Double = 0.35,
    val successIntervalFactor: Double = 1.25,
    val easyIntervalFactor: Double = 1.5,
    val maxRebalanceSearchDays: Int = 2
) {
    init {
        require(finalReviewBufferDays >= 0) { "시험 전 버퍼 일수는 0 이상이어야 합니다." }
        require(difficultIntervalFactor > 0.0) { "어려운 복습 간격 계수는 양수여야 합니다." }
        require(failureIntervalFactor > 0.0) { "실패 복습 간격 계수는 양수여야 합니다." }
        require(successIntervalFactor > 0.0) { "성공 복습 간격 계수는 양수여야 합니다." }
        require(easyIntervalFactor > 0.0) { "쉬운 복습 간격 계수는 양수여야 합니다." }
        require(maxRebalanceSearchDays >= 2) { "재배치 우선 탐색 범위는 2일 이상이어야 합니다." }
    }
}

data class ReviewNotificationPlan(
    val defaultNotificationHour: Int = 9,
    val defaultNotificationMinute: Int = 0,
    val incompleteReminderHour: Int = 19,
    val incompleteReminderMinute: Int = 0,
    val includeFinalEveningReminder: Boolean = false,
    val timezone: ZoneId = ZoneId.of("Asia/Seoul")
)

data class ReviewScheduleEntry(
    val reviewId: String,
    val studyRecordId: String,
    val reviewIndex: Int,
    val scheduledDate: LocalDate,
    val status: ReviewPlanStatus = ReviewPlanStatus.SCHEDULED,
    val priorityScore: Double = 0.0,
    val estimatedReviewMinutes: Int = 15,
    val recommendedMethod: String = "",
    val notificationPlan: ReviewNotificationPlan = ReviewNotificationPlan(),
    val dueDaysBeforeExam: Long = 0L,
    val isFinalReview: Boolean = false,
    val rescheduleReason: String? = null,
    val difficulty: ReviewDifficulty = ReviewDifficulty.MEDIUM,
    val importance: ReviewImportance = ReviewImportance.NORMAL,
    val initialMastery: Int? = null,
    val originalScheduledDate: LocalDate = scheduledDate
)

sealed class ValidationResult {
    data object Valid : ValidationResult()
    data class Blocked(
        val message: String,
        val availableDaysForNewLearning: Long,
        val recommendedEarliestExamDate: LocalDate
    ) : ValidationResult()
}

data class SchedulingResult(
    val schedules: List<ReviewScheduleEntry>,
    val status: ReviewPlanStatus,
    val warningMessage: String? = null,
    val lastReviewDate: LocalDate,
    val effectiveStudyDays: Long,
    val targetReviewCount: Int,
    val generatedReviewCount: Int,
    val compressed: Boolean,
    val availableDaysForNewLearning: Long
)

data class RebalanceResult(
    val schedules: List<ReviewScheduleEntry>,
    val status: ReviewPlanStatus,
    val warningMessage: String? = null
)

data class OutcomeRescheduleResult(
    val schedules: List<ReviewScheduleEntry>,
    val applied: Boolean,
    val reason: String
)
