package com.loorve.domain.review

import java.time.LocalDate
import kotlin.math.roundToInt

data class ReviewCompletionSchedule(
    val id: String,
    val dueDate: LocalDate,
    val isCompleted: Boolean,
    val sourceId: String = "",
    val reviewOrder: Int = 0
)

data class DailyReviewCompletionStat(
    val date: LocalDate,
    val dueCount: Int,
    val completedCount: Int
) {
    val completionRatePercent: Int?
        get() = if (dueCount <= 0) {
            null
        } else {
            ((completedCount.coerceAtMost(dueCount).toDouble() / dueCount) * 100)
                .roundToInt()
                .coerceIn(0, 100)
        }
}

fun buildRecentReviewCompletionStats(
    schedules: List<ReviewCompletionSchedule>,
    today: LocalDate,
    dayCount: Int = 7
): List<DailyReviewCompletionStat> {
    require(dayCount > 0) { "통계 기간은 양수여야 합니다." }
    val dates = (dayCount - 1 downTo 0).map { today.minusDays(it.toLong()) }
    val schedulesByDate = schedules
        .distinctBy { schedule ->
            if (schedule.id.isNotBlank()) {
                "id:${schedule.id}"
            } else {
                "fallback:${schedule.dueDate}|${schedule.sourceId}|${schedule.reviewOrder}"
            }
        }
        .groupBy { it.dueDate }
    return dates.map { date ->
        val dueSchedules = schedulesByDate[date].orEmpty()
        DailyReviewCompletionStat(
            date = date,
            dueCount = dueSchedules.size,
            completedCount = dueSchedules.count { it.isCompleted }
        )
    }
}

fun splitValidReviewCompletionStatSegments(
    stats: List<DailyReviewCompletionStat>
): List<List<Pair<Int, DailyReviewCompletionStat>>> {
    val validPoints = stats.mapIndexedNotNull { index, stat ->
        if (stat.completionRatePercent == null) null else index to stat
    }
    if (validPoints.isEmpty()) return emptyList()

    val segments = mutableListOf<MutableList<Pair<Int, DailyReviewCompletionStat>>>()
    validPoints.forEach { point ->
        val current = segments.lastOrNull()
        if (current == null || point.first != current.last().first + 1) {
            segments += mutableListOf(point)
        } else {
            current += point
        }
    }
    return segments
}
