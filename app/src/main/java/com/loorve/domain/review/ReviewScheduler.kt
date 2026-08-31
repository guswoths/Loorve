package com.loorve.domain.review

import com.loorve.domain.model.CompletionResult
import com.loorve.domain.model.ReviewScheduleItem
import com.loorve.domain.model.ReviewStatus
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

private val KST = ZoneId.of("Asia/Seoul")

// ── 날짜 변환 유틸 ──────────────────────────────────────────
fun LocalDate.toEpochMillis(): Long =
    atStartOfDay(KST).toInstant().toEpochMilli()

fun Long.toLocalDate(): LocalDate =
    java.time.Instant.ofEpochMilli(this)
        .atZone(KST)
        .toLocalDate()
// ─────────────────────────────────────────────────────────────

object ReviewScheduler {

    private const val FIRST_REVIEW_RATIO = 0.15
    private const val EXPANDING_RATIO = 0.20
    private const val BUFFER_RATIO = 0.07
    private const val MIN_BUFFER_DAYS = 3L
    private const val MIN_GAP_DAYS = 1L
    private const val DEFAULT_MIN_REVIEWS = 5

    // ──────────────────────────────────────────────────────
    // A. 기본 일정 생성
    // ──────────────────────────────────────────────────────
    data class ScheduleResult(
        val items: List<ReviewScheduleItem>,
        val recommendedCompletionDate: LocalDate,
        val deadlineBufferDays: Long,
        val compressedReview: Boolean = false
    )

    /**
     * @param learningDate  학습 등록일
     * @param examDate      시험 목표일
     * @param studyRecordId 학습기록 ID
     * @param title         복습 일정 제목에 사용할 학습 내용 제목
     * @param blockId       복습 블록 ID
     * @param uid           사용자 UID
     * @param prepStartDate 준비 시작일 (전체 준비기간 계산용; null이면 learningDate 사용)
     */
    fun generateSchedule(
        learningDate: LocalDate,
        examDate: LocalDate,
        studyRecordId: String,
        title: String,
        blockId: String,
        uid: String,
        prepStartDate: LocalDate? = null
    ): ScheduleResult {
        require(examDate > learningDate) {
            "시험일(${examDate})은 학습일(${learningDate})보다 미래여야 합니다."
        }

        val totalPeriod = ChronoUnit.DAYS.between(prepStartDate ?: learningDate, examDate)
        val deadlineBufferDays = max(MIN_BUFFER_DAYS, (totalPeriod * BUFFER_RATIO).roundToLong())
        val latestReviewDate = examDate.minusDays(deadlineBufferDays)

        val riDays = ChronoUnit.DAYS.between(learningDate, examDate)

        val scheduleDates = mutableListOf<LocalDate>()
        var currentDate = learningDate
        var remainingRI = riDays

        // 최소 DEFAULT_MIN_REVIEWS 회차 생성 시도
        repeat(DEFAULT_MIN_REVIEWS) {
            val gap = max(MIN_GAP_DAYS, (remainingRI * FIRST_REVIEW_RATIO).roundToLong())
                .let { if (it == 0L) MIN_GAP_DAYS else it }
            val nextDate = currentDate.plusDays(gap)
            // 시험일 이후 또는 최신 복습일 초과 시 중단
            if (nextDate >= examDate) return@repeat
            // 날짜 중복 방지
            if (!scheduleDates.contains(nextDate)) {
                scheduleDates.add(nextDate)
            }
            currentDate = nextDate
            remainingRI = ChronoUnit.DAYS.between(currentDate, examDate)
            if (remainingRI <= 0) return@repeat
        }

        // 마지막 복습 마감 기준 적용
        var compressedReview = false
        if (scheduleDates.isNotEmpty()) {
            val lastIdx = scheduleDates.lastIndex
            if (scheduleDates[lastIdx] > latestReviewDate) {
                scheduleDates[lastIdx] = latestReviewDate
                compressedReview = true
            }
        }

        val nowMillis = System.currentTimeMillis()
        val items = scheduleDates.mapIndexed { index, date ->
            val isLast = index == scheduleDates.lastIndex
            ReviewScheduleItem(
                id = "${studyRecordId}_r${index + 1}",
                studyRecordId = studyRecordId,
                blockId = blockId,
                uid = uid,
                title = buildTitle(index + 1, title, isLast, false),
                reviewDate = date.toEpochMillis(),
                originalReviewDate = date.toEpochMillis(),
                stage = index,
                reviewOrder = index + 1,
                status = ReviewStatus.PENDING,
                previousGapDays = if (index == 0) {
                    ChronoUnit.DAYS.between(learningDate, date)
                } else {
                    ChronoUnit.DAYS.between(scheduleDates[index - 1], date)
                }.coerceAtLeast(1L),
                overdueDays = 0L,       // ← 추가
                compressedReview = if (isLast) compressedReview else false,
                createdAt = nowMillis,
                updatedAt = nowMillis
            )
        }

        return ScheduleResult(
            items = items,
            recommendedCompletionDate = latestReviewDate,
            deadlineBufferDays = deadlineBufferDays,
            compressedReview = compressedReview
        )
    }

    // ──────────────────────────────────────────────────────
    // A-7. 신규 학습기록 등록 시 미완료 일정 재계산
    // ──────────────────────────────────────────────────────
    fun rescaleOnNewEntry(
        today: LocalDate,
        examDate: LocalDate,
        pendingItems: List<ReviewScheduleItem>
    ): List<ReviewScheduleItem> {
        if (examDate <= today) return pendingItems
        val remainingRI = ChronoUnit.DAYS.between(today, examDate)
        val count = pendingItems.size
        if (count == 0) return emptyList()

        // 균등 expanding 재배치
        val nowMillis = System.currentTimeMillis()
        val result = mutableListOf<ReviewScheduleItem>()
        var currentDate = today
        var remRI = remainingRI

        pendingItems.forEachIndexed { index, item ->
            val ratio = if (index == 0) FIRST_REVIEW_RATIO else EXPANDING_RATIO
            val gap = max(MIN_GAP_DAYS, (remRI * ratio).roundToLong())
            val newDate = currentDate.plusDays(gap)
                .let { if (it >= examDate) examDate.minusDays(1) else it }

            result.add(
                item.copy(
                    reviewDate = newDate.toEpochMillis(),
                    previousGapDays = gap.coerceAtLeast(1L),
                    updatedAt = nowMillis
                )
            )
            currentDate = newDate
            remRI = ChronoUnit.DAYS.between(currentDate, examDate)
            if (remRI <= 0) {
                // 남은 항목은 examDate - 1로 몰기
                for (i in index + 1 until count) {
                    result.add(
                        pendingItems[i].copy(
                            reviewDate = examDate.minusDays(1).toEpochMillis(),
                            compressedReview = true,
                            updatedAt = nowMillis
                        )
                    )
                }
                return result
            }
        }
        return result
    }

    // ──────────────────────────────────────────────────────
    // B. 미완료(OVERDUE) 처리
    // ──────────────────────────────────────────────────────
    data class OverdueResult(
        val updatedItems: List<ReviewScheduleItem>,
        val overdueQueue: List<ReviewScheduleItem> // oldest-first
    )

    fun handleOverdue(
        today: LocalDate,
        items: List<ReviewScheduleItem>
    ): OverdueResult {
        val nowMillis = System.currentTimeMillis()
        val updated = items.map { item ->
            if (item.status == ReviewStatus.PENDING &&
                item.reviewDate.toLocalDate() < today
            ) {
                val overdueDays = ChronoUnit.DAYS.between(
                    item.reviewDate.toLocalDate(), today
                ).coerceAtLeast(0L)
                item.copy(
                    status = ReviewStatus.OVERDUE,
                    overdueDays = overdueDays,
                    updatedAt = nowMillis
                )
            } else item
        }

        // oldest-first (reviewDate 오름차순)
        val overdueQueue = updated
            .filter { it.status == ReviewStatus.OVERDUE }
            .sortedBy { it.reviewDate }

        return OverdueResult(updatedItems = updated, overdueQueue = overdueQueue)
    }

    // ──────────────────────────────────────────────────────
    // B-9~10. 복습 완료 처리 (REMEMBERED / FORGOT)
    // ──────────────────────────────────────────────────────
    data class CompleteReviewResult(
        val updatedItem: ReviewScheduleItem,
        val nextReviewDate: LocalDate?,
        val nextGapDays: Long
    )

    fun completeReview(
        item: ReviewScheduleItem,
        result: CompletionResult,
        today: LocalDate,
        examDate: LocalDate
    ): CompleteReviewResult {
        val nowMillis = System.currentTimeMillis()
        val overdueDays = item.overdueDays.coerceAtLeast(0L)

        val (nextGap, newStage) = when (result) {
            CompletionResult.REMEMBERED -> {
                val prevGap = item.previousGapDays.coerceAtLeast(1L)
                val rawGap = prevGap * (1.0 + min(overdueDays.toDouble() / prevGap, 1.0))
                val cappedGap = min(rawGap.roundToLong(), prevGap * 2)
                cappedGap.coerceAtLeast(1L) to item.stage
            }
            CompletionResult.FORGOT -> {
                1L to max(0, item.stage - 1)
            }
        }

        val nextDate = today.plusDays(nextGap)
            .let { if (it >= examDate) null else it } // 시험일 이후면 다음 일정 없음

        val completed = item.copy(
            status = ReviewStatus.COMPLETED,
            completionResult = result,
            completedAt = nowMillis,
            stage = newStage,
            updatedAt = nowMillis
        )

        return CompleteReviewResult(
            updatedItem = completed,
            nextReviewDate = nextDate,
            nextGapDays = nextGap
        )
    }

    // ──────────────────────────────────────────────────────
    // B-11. 시험 임박 시 긴급 압축
    // ──────────────────────────────────────────────────────
    data class CompressResult(
        val urgentItems: List<ReviewScheduleItem>,
        val reviewOverloadWarning: Boolean
    )

    fun compressBeforeExam(
        examDate: LocalDate,
        today: LocalDate,
        overdueList: List<ReviewScheduleItem>,
        prepStartDate: LocalDate,
        dailyCap: Int
    ): CompressResult {
        if (dailyCap <= 0) return CompressResult(emptyList(), overdueList.isNotEmpty())

        val totalPeriod = ChronoUnit.DAYS.between(prepStartDate, examDate)
        val deadlineBuffer = max(MIN_BUFFER_DAYS, (totalPeriod * BUFFER_RATIO).roundToLong())
        val remainingDays = ChronoUnit.DAYS.between(today, examDate)

        if (remainingDays > deadlineBuffer) {
            return CompressResult(overdueList, false)
        }

        // 가용 슬롯: remainingDays - 1 일 × dailyCap
        val availableSlots = ((remainingDays - 1) * dailyCap).coerceAtLeast(0L)
        val overloadWarning = overdueList.size > availableSlots

        // 우선순위: 최근 학습 → stability 낮은 순
        val sorted = overdueList.sortedWith(
            compareByDescending<ReviewScheduleItem> { it.studyRecordId }
                .thenBy { it.stage }
        )

        val nowMillis = System.currentTimeMillis()
        val result = mutableListOf<ReviewScheduleItem>()
        var dayOffset = 0L
        var dailyCount = 0

        for (item in sorted) {
            if (dayOffset >= remainingDays - 1) break
            if (dailyCount >= dailyCap) {
                dayOffset++
                dailyCount = 0
            }
            val assignedDate = today.plusDays(dayOffset + 1)
            result.add(
                item.copy(
                    reviewDate = assignedDate.toEpochMillis(),
                    status = ReviewStatus.FINAL_URGENT_REVIEW,
                    title = buildTitle(item.reviewOrder, item.title, false, true),
                    compressedReview = true,
                    updatedAt = nowMillis
                )
            )
            dailyCount++
        }

        return CompressResult(urgentItems = result, reviewOverloadWarning = overloadWarning)
    }

    // ──────────────────────────────────────────────────────
    // B-7. Retrievability 추정
    // ──────────────────────────────────────────────────────
    fun retrievabilityEstimate(overdueDays: Long, successCount: Int): Double {
        val stability = max(1.0, successCount.toDouble() * 2.0) // 최소 1.0 보장
        return exp(-overdueDays.toDouble() / stability)
    }

    // ──────────────────────────────────────────────────────
    // B-12~13. 완료율 계산 및 AT_RISK 판정
    // ──────────────────────────────────────────────────────
    fun completionRate(totalCount: Int, completedCount: Int): Double {
        if (totalCount <= 0) return 0.0
        return completedCount.toDouble() / totalCount.toDouble()
    }

    fun isAtRisk(daysUntilExam: Int, overdueCount: Int): Boolean =
        overdueCount > 0 && daysUntilExam < overdueCount

    // ──────────────────────────────────────────────────────
    // 내부 유틸
    // ──────────────────────────────────────────────────────
    private fun buildTitle(
        order: Int,
        content: String,
        isLast: Boolean,
        isUrgent: Boolean
    ): String {
        val truncated = content.take(20)
        return when {
            isUrgent -> "[긴급 복습] $truncated"
            isLast -> "[마지막 복습] $truncated"
            else -> "[복습 ${order}회차] $truncated"
        }
    }

    fun calcDeadlineBufferDays(prepStartDate: LocalDate, examDate: LocalDate): Long {
        val totalPeriod = ChronoUnit.DAYS.between(prepStartDate, examDate)
        return max(MIN_BUFFER_DAYS, (totalPeriod * BUFFER_RATIO).roundToLong())
    }
}