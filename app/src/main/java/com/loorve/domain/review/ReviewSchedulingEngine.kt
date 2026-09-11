package com.loorve.domain.review

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * 시험일 역산 기반의 순수 복습 일정 엔진입니다.
 *
 * 제품 정책은 시험 전 버퍼, 최소 유효 학습기간, 하루 복습량 제한으로 표현합니다.
 * 학습과학 측면에서는 분산학습과 인출 연습을 기본 원칙으로 사용하지만,
 * 에빙하우스 곡선을 개인별 망각률의 정확한 예측식으로 주장하지 않습니다.
 * 학습자·과목·난이도 차이는 난이도, 중요도, 초기 숙련도, 복습 결과를 통해
 * 향후 조정할 수 있도록 입력과 결과에 명시합니다.
 *
 * 일정 계산은 LocalDate만 사용하여 시간대와 서머타임에 따른 날짜 오차를 피합니다.
 * 기본 간격의 비율을 보존하면서 첫 복습을 +1일에 두고 마지막 복습을 버퍼 직전 날짜에
 * 고정하는 하이브리드 방식입니다. 단순 비율 스케일링은 짧은 기간에 초기 간격을
 * 과도하게 뭉칠 수 있고, 정규화 백분위 보간은 원래 간격의 의미를 약화할 수 있으므로
 * 두 방법을 절충해 중간 날짜에 기본 간격의 상대 구조를 사용합니다. 날짜 보정은
 * 일정 개수에 대해 선형으로 수행됩니다.
 */
object ReviewSchedulingEngine {

    fun lastReviewDate(
        examDate: LocalDate,
        finalReviewBufferDays: Int
    ): LocalDate {
        require(finalReviewBufferDays >= 0) { "시험 전 버퍼 일수는 0 이상이어야 합니다." }
        return examDate.minusDays(finalReviewBufferDays.toLong() + 1L)
    }

    fun validateExamDate(
        today: LocalDate,
        examDate: LocalDate,
        finalReviewBufferDays: Int
    ): ValidationResult {
        require(finalReviewBufferDays >= 0) { "시험 전 버퍼 일수는 0 이상이어야 합니다." }
        val availableDays = ChronoUnit.DAYS.between(today, examDate) -
            finalReviewBufferDays - 1L
        val earliest = today.plusDays(3L + finalReviewBufferDays + 1L)
        return if (examDate <= today || availableDays < 3L) {
            ValidationResult.Blocked(
                message = SchedulingMessages.blockedExamDate(
                    availableDays = availableDays.coerceAtLeast(0L),
                    finalReviewBufferDays = finalReviewBufferDays,
                    earliestExamDate = earliest
                ),
                availableDaysForNewLearning = availableDays,
                recommendedEarliestExamDate = earliest
            )
        } else {
            ValidationResult.Valid
        }
    }

    fun getMinimumReviewCount(effectiveStudyDays: Long): Int = when {
        effectiveStudyDays < 3L -> 0
        effectiveStudyDays <= 6L -> 2
        effectiveStudyDays <= 13L -> 3
        effectiveStudyDays <= 29L -> 4
        effectiveStudyDays <= 59L -> 5
        effectiveStudyDays <= 119L -> 6
        else -> 7
    }

    fun getTargetReviewCount(
        effectiveStudyDays: Long,
        difficulty: ReviewDifficulty,
        initialMastery: Int?,
        optionalMinReviewCount: Int? = null
    ): Int {
        require(initialMastery == null || initialMastery in 1..5) {
            "초기 숙련도는 1에서 5 사이여야 합니다."
        }
        require(optionalMinReviewCount == null || optionalMinReviewCount > 0) {
            "최소 복습 횟수는 양수여야 합니다."
        }
        val systemMinimum = getMinimumReviewCount(effectiveStudyDays)
        val preferredAdjustment =
            (if (difficulty == ReviewDifficulty.HARD) 1 else 0) +
                (if (initialMastery != null && initialMastery <= 2) 1 else 0)
        return max(systemMinimum, optionalMinReviewCount ?: 0) + preferredAdjustment
    }

    fun generateScaledReviewDates(
        studyDate: LocalDate,
        examDate: LocalDate,
        finalReviewBufferDays: Int,
        targetReviewCount: Int,
        baseIntervals: List<Int> = listOf(1, 3, 7, 14, 30, 60, 120)
    ): List<LocalDate> {
        require(targetReviewCount >= 0) { "목표 복습 횟수는 0 이상이어야 합니다." }
        require(baseIntervals.isNotEmpty() && baseIntervals.first() == 1) {
            "기본 간격은 1일로 시작해야 합니다."
        }
        require(baseIntervals.zipWithNext().all { it.first < it.second }) {
            "기본 간격은 오름차순이어야 합니다."
        }
        val first = studyDate.plusDays(1L)
        val last = lastReviewDate(examDate, finalReviewBufferDays)
        if (targetReviewCount == 0 || first.isAfter(last)) return emptyList()

        val usableDays = ChronoUnit.DAYS.between(first, last).toInt() + 1
        val count = min(targetReviewCount, usableDays)
        val selectedIntervals = (0 until count).map { index ->
            if (index < baseIntervals.size) {
                baseIntervals[index].toDouble()
            } else {
                baseIntervals.last().toDouble() *
                    (2.0.pow(index - baseIntervals.lastIndex))
            }
        }
        val baseLast = selectedIntervals.last()
        val span = ChronoUnit.DAYS.between(first, last).toInt()
        val rawOffsets = if (count == 1) {
            listOf(0)
        } else {
            (0 until count).map { index ->
                if (index == 0) 0
                else if (index == count - 1) span
                else (selectedIntervals[index] / baseLast * span).roundToInt()
            }
        }
        val offsets = enforceStrictOffsets(rawOffsets, span, count)
        return offsets.map { first.plusDays(it.toLong()) }.distinct()
    }

    fun createReviewSchedules(
        record: SchedulerStudyRecord,
        exam: SchedulerExam,
        today: LocalDate,
        config: SchedulerConfig = SchedulerConfig(),
        notificationPlan: ReviewNotificationPlan = ReviewNotificationPlan()
    ): SchedulingResult {
        require(record.estimatedReviewMinutes > 0) { "예상 복습 시간은 양수여야 합니다." }
        require(config.finalReviewBufferDays >= 0) { "시험 전 버퍼 일수는 0 이상이어야 합니다." }
        require(record.initialMastery == null || record.initialMastery in 1..5) {
            "초기 숙련도는 1에서 5 사이여야 합니다."
        }
        if (!record.isCompleted) {
            return emptyResult(
                record,
                exam,
                today,
                config.finalReviewBufferDays,
                ReviewPlanStatus.INSUFFICIENT_WINDOW,
                "완료되지 않은 학습기록은 복습 일정을 만들 수 없습니다."
            )
        }

        val last = lastReviewDate(exam.examDate, config.finalReviewBufferDays)
        val effectiveDays = ChronoUnit.DAYS.between(record.studiedAtDate, last).coerceAtLeast(0L)
        val target = getTargetReviewCount(
            effectiveDays,
            record.difficulty,
            record.initialMastery,
            record.optionalMinReviewCount
        )
        if (effectiveDays < 3L) {
            val status = if (config.existingRecordInsufficiencyPolicy ==
                ExistingRecordInsufficiencyPolicy.BLOCK
            ) ReviewPlanStatus.INSUFFICIENT_WINDOW else ReviewPlanStatus.CRAM_MODE_REQUIRED
            return emptyResult(
                record,
                exam,
                today,
                config.finalReviewBufferDays,
                status,
                SchedulingMessages.cramMode(record.studyRecordId)
            )
                .copy(targetReviewCount = target, effectiveStudyDays = effectiveDays)
        }

        val dates = generateScaledReviewDates(
            studyDate = record.studiedAtDate,
            examDate = exam.examDate,
            finalReviewBufferDays = config.finalReviewBufferDays,
            targetReviewCount = target,
            baseIntervals = config.baseIntervals
        )
        val compressed = dates.lastOrNull() != null &&
            dates.last() != record.studiedAtDate.plusDays(config.baseIntervals.last().toLong())
        val status = if (dates.size < target) ReviewPlanStatus.INSUFFICIENT_WINDOW
        else ReviewPlanStatus.SCHEDULED
        val warning = if (status == ReviewPlanStatus.INSUFFICIENT_WINDOW) {
            SchedulingMessages.insufficientWindow(dates.size, target)
        } else null
        val schedules = dates.mapIndexed { index, date ->
            createEntry(record, exam, notificationPlan, date, index, dates.last(), status)
        }
        return SchedulingResult(
            schedules = schedules,
            status = status,
            warningMessage = warning,
            lastReviewDate = last,
            effectiveStudyDays = effectiveDays,
            targetReviewCount = target,
            generatedReviewCount = schedules.size,
            compressed = compressed,
            availableDaysForNewLearning = ChronoUnit.DAYS.between(today, exam.examDate) -
                config.finalReviewBufferDays - 1L
        )
    }

    fun rebalanceDailyLoad(
        schedules: List<ReviewScheduleEntry>,
        exam: SchedulerExam,
        reviewStartDate: LocalDate,
        config: SchedulerConfig = SchedulerConfig()
    ): RebalanceResult {
        val minuteLimit = exam.maxDailyReviewMinutes
        val itemLimit = exam.maxDailyReviewItems
        if (minuteLimit == null && itemLimit == null) {
            return RebalanceResult(schedules, ReviewPlanStatus.SCHEDULED)
        }
        require(minuteLimit == null || minuteLimit > 0) { "하루 최대 복습 시간은 양수여야 합니다." }
        require(itemLimit == null || itemLimit > 0) { "하루 최대 복습 개수는 양수여야 합니다." }
        val last = lastReviewDate(exam.examDate, config.finalReviewBufferDays)
        val result = schedules.toMutableList()
        var unresolved = false
        val dates = result.map { it.scheduledDate }.distinct().sorted()
        for (date in dates) {
            while (
                minutesOn(result, date) > (minuteLimit ?: Int.MAX_VALUE) ||
                countOn(result, date) > (itemLimit ?: Int.MAX_VALUE)
            ) {
                val candidate = result
                    .filter { it.scheduledDate == date && !it.isFinalReview }
                    .sortedWith(compareBy<ReviewScheduleEntry> { it.priorityScore }.thenByDescending { it.estimatedReviewMinutes })
                    .firstOrNull()
                if (candidate == null) {
                    unresolved = true
                    break
                }
                val destination = candidateDates(candidate, date, reviewStartDate, last, config.maxRebalanceSearchDays)
                    .firstOrNull { target ->
                        minutesOn(result, target) + candidate.estimatedReviewMinutes <=
                            (minuteLimit ?: Int.MAX_VALUE) &&
                            countOn(result, target) + 1 <= (itemLimit ?: Int.MAX_VALUE) &&
                            preservesSpacing(candidate, target, result)
                    }
                if (destination == null) {
                    unresolved = true
                    break
                }
                val index = result.indexOfFirst { it.reviewId == candidate.reviewId }
                result[index] = candidate.copy(
                    scheduledDate = destination,
                    status = ReviewPlanStatus.RESCHEDULED,
                    rescheduleReason = "복습량 분산을 위해 ${date}에서 ${destination}로 이동했습니다."
                )
            }
        }
        return RebalanceResult(
            schedules = result,
            status = if (unresolved) ReviewPlanStatus.OVERLOADED_UNRESOLVED else ReviewPlanStatus.SCHEDULED,
            warningMessage = if (unresolved) SchedulingMessages.overloaded else null
        )
    }

    fun rescheduleAfterReviewOutcome(
        schedules: List<ReviewScheduleEntry>,
        completedReviewId: String,
        outcome: ReviewOutcome,
        today: LocalDate,
        exam: SchedulerExam,
        config: SchedulerConfig = SchedulerConfig()
    ): OutcomeRescheduleResult {
        val completed = schedules.firstOrNull { it.reviewId == completedReviewId }
            ?: return OutcomeRescheduleResult(schedules, false, "대상 복습 일정을 찾을 수 없습니다.")
        val next = schedules
            .filter { it.studyRecordId == completed.studyRecordId && it.scheduledDate.isAfter(today) }
            .minByOrNull { it.scheduledDate }
            ?: return OutcomeRescheduleResult(schedules, false, "이후 복습 일정이 없습니다.")
        if (next.isFinalReview || next.status == ReviewPlanStatus.COMPLETED ||
            next.status == ReviewPlanStatus.MISSED
        ) {
            return OutcomeRescheduleResult(schedules, false, "마지막 복습 또는 변경할 수 없는 일정입니다.")
        }
        val factor = when (outcome) {
            ReviewOutcome.DIFFICULT -> config.difficultIntervalFactor
            ReviewOutcome.FAILURE -> config.failureIntervalFactor
            ReviewOutcome.SUCCESS -> config.successIntervalFactor
            ReviewOutcome.EASY -> config.easyIntervalFactor
        }
        val gap = ChronoUnit.DAYS.between(completed.scheduledDate, next.scheduledDate)
        val proposed = completed.scheduledDate.plusDays(max(1L, (gap * factor).roundToInt().toLong()))
        val last = lastReviewDate(exam.examDate, config.finalReviewBufferDays)
        val prior = schedules.filter { it.studyRecordId == next.studyRecordId && it.scheduledDate.isBefore(next.scheduledDate) }
            .maxByOrNull { it.scheduledDate }?.scheduledDate
        val following = schedules.filter { it.studyRecordId == next.studyRecordId && it.scheduledDate.isAfter(next.scheduledDate) }
            .minByOrNull { it.scheduledDate }?.scheduledDate
        val upperBound = if (following != null && following.minusDays(1).isBefore(last)) {
            following.minusDays(1)
        } else {
            last
        }
        val lowerBound = prior?.plusDays(1) ?: today.plusDays(1)
        val adjusted = proposed.coerceIn(
            lowerBound,
            upperBound
        )
        if (!adjusted.isAfter(today) || adjusted == next.scheduledDate) {
            return OutcomeRescheduleResult(schedules, false, "간격과 시험 전 버퍼를 유지할 수 없어 일정을 변경하지 않았습니다.")
        }
        val updated = schedules.map {
            if (it.reviewId == next.reviewId) {
                it.copy(
                    scheduledDate = adjusted,
                    status = ReviewPlanStatus.RESCHEDULED,
                    rescheduleReason = "복습 결과 ${outcome.name}에 따라 ${next.scheduledDate}에서 ${adjusted}로 조정했습니다."
                )
            } else it
        }
        return OutcomeRescheduleResult(updated, true, "복습 결과에 따라 다음 일정을 조정했습니다.")
    }

    fun priorityScore(
        daysUntilExam: Long,
        importance: ReviewImportance,
        difficulty: ReviewDifficulty,
        initialMastery: Int?,
        isFinalReview: Boolean
    ): Double {
        val urgency = 100.0 / (daysUntilExam.coerceAtLeast(0L) + 1L)
        val importanceScore = if (importance == ReviewImportance.HIGH) 20.0 else if (importance == ReviewImportance.NORMAL) 10.0 else 0.0
        val difficultyScore = if (difficulty == ReviewDifficulty.HARD) 15.0 else if (difficulty == ReviewDifficulty.MEDIUM) 7.0 else 0.0
        val masteryScore = initialMastery?.let { (5 - it).coerceAtLeast(0) * 5.0 } ?: 0.0
        return urgency + importanceScore + difficultyScore + masteryScore + if (isFinalReview) 30.0 else 0.0
    }

    private fun createEntry(
        record: SchedulerStudyRecord,
        exam: SchedulerExam,
        plan: ReviewNotificationPlan,
        date: LocalDate,
        index: Int,
        lastDate: LocalDate,
        status: ReviewPlanStatus
    ): ReviewScheduleEntry {
        val final = date == lastDate
        val daysBeforeExam = ChronoUnit.DAYS.between(date, exam.examDate)
        return ReviewScheduleEntry(
            reviewId = "${record.studyRecordId}_r${index + 1}",
            studyRecordId = record.studyRecordId,
            reviewIndex = index + 1,
            scheduledDate = date,
            status = if (status == ReviewPlanStatus.SCHEDULED) ReviewPlanStatus.SCHEDULED else status,
            priorityScore = priorityScore(daysBeforeExam, record.importance, record.difficulty, record.initialMastery, final),
            estimatedReviewMinutes = record.estimatedReviewMinutes,
            recommendedMethod = when {
                index == 0 -> "노트 없이 핵심 개념 3~5개를 먼저 회상한 뒤, 틀린 부분만 확인하세요."
                index == 1 -> "플래시카드 또는 짧은 퀴즈로 인출 연습을 하세요."
                final -> "시험 범위의 핵심 구조와 오답 포인트를 인출 점검하세요. 새 내용 학습은 피하세요."
                else -> "문제풀이·서술형 회상·오답 설명 중 하나를 수행하세요."
            },
            notificationPlan = plan,
            dueDaysBeforeExam = daysBeforeExam,
            isFinalReview = final,
            difficulty = record.difficulty,
            importance = record.importance,
            initialMastery = record.initialMastery
        )
    }

    private fun emptyResult(
        record: SchedulerStudyRecord,
        exam: SchedulerExam,
        today: LocalDate,
        finalReviewBufferDays: Int,
        status: ReviewPlanStatus,
        warning: String
    ) = SchedulingResult(
        schedules = emptyList(),
        status = status,
        warningMessage = warning,
        lastReviewDate = lastReviewDate(exam.examDate, finalReviewBufferDays),
        effectiveStudyDays = ChronoUnit.DAYS.between(
            record.studiedAtDate,
            lastReviewDate(exam.examDate, finalReviewBufferDays)
        ).coerceAtLeast(0L),
        targetReviewCount = 0,
        generatedReviewCount = 0,
        compressed = true,
        availableDaysForNewLearning = ChronoUnit.DAYS.between(
            today,
            exam.examDate
        ) - finalReviewBufferDays - 1L
    )

    private fun enforceStrictOffsets(raw: List<Int>, span: Int, count: Int): List<Int> {
        if (count <= 0) return emptyList()
        val result = raw.toMutableList()
        result[0] = 0
        result[count - 1] = span
        for (i in 1 until count) {
            result[i] = max(result[i], result[i - 1] + 1)
        }
        for (i in count - 2 downTo 0) {
            result[i] = min(result[i], result[i + 1] - 1)
        }
        return result.map { it.coerceIn(0, span) }
    }

    private fun minutesOn(items: List<ReviewScheduleEntry>, date: LocalDate): Int =
        items.filter { it.scheduledDate == date }.sumOf { it.estimatedReviewMinutes }

    private fun countOn(items: List<ReviewScheduleEntry>, date: LocalDate): Int =
        items.count { it.scheduledDate == date }

    private fun candidateDates(
        item: ReviewScheduleEntry,
        original: LocalDate,
        start: LocalDate,
        last: LocalDate,
        radius: Int
    ): List<LocalDate> {
        val maxDistance = max(
            radius,
            max(
                abs(ChronoUnit.DAYS.between(start, original).toInt()),
                abs(ChronoUnit.DAYS.between(original, last).toInt())
            )
        )
        return (1..maxDistance).flatMap { distance ->
            listOf(original.minusDays(distance.toLong()), original.plusDays(distance.toLong()))
        }.filter { it in start..last }
    }

    private fun preservesSpacing(
        item: ReviewScheduleEntry,
        target: LocalDate,
        all: List<ReviewScheduleEntry>
    ): Boolean {
        val same = all.filter { it.studyRecordId == item.studyRecordId && it.reviewId != item.reviewId }
        val previous = same.filter { it.scheduledDate.isBefore(target) }.maxByOrNull { it.scheduledDate }
        val following = same.filter { it.scheduledDate.isAfter(target) }.minByOrNull { it.scheduledDate }
        return (previous == null || ChronoUnit.DAYS.between(previous.scheduledDate, target) >= 1) &&
            (following == null || ChronoUnit.DAYS.between(target, following.scheduledDate) >= 1)
    }
}
