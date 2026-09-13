package com.loorve.domain.review

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ReviewCompletionStatisticsTest {

    private val today = LocalDate.of(2026, 9, 14)

    @Test
    fun `최근 7일을 오래된 날짜부터 오늘까지 생성한다`() {
        val stats = buildRecentReviewCompletionStats(emptyList(), today)

        assertEquals(today.minusDays(6), stats.first().date)
        assertEquals(today, stats.last().date)
        assertEquals(7, stats.size)
    }

    @Test
    fun `완료율은 100 75 40 퍼센트로 계산한다`() {
        val schedules = listOf(
            schedules(today.minusDays(2), due = 4, completed = 4),
            schedules(today.minusDays(1), due = 8, completed = 6),
            schedules(today, due = 5, completed = 2)
        ).flatten()

        val stats = buildRecentReviewCompletionStats(schedules, today)

        assertEquals(100, stats[4].completionRatePercent)
        assertEquals(75, stats[5].completionRatePercent)
        assertEquals(40, stats[6].completionRatePercent)
    }

    @Test
    fun `일정이 없는 날은 완료율이 null이다`() {
        val stat = buildRecentReviewCompletionStats(emptyList(), today).last()

        assertEquals(0, stat.dueCount)
        assertEquals(0, stat.completedCount)
        assertNull(stat.completionRatePercent)
    }

    @Test
    fun `오늘 일정 유무에 따라 오늘 통계를 계산한다`() {
        val withSchedule = buildRecentReviewCompletionStats(
            schedules(today, due = 1, completed = 1),
            today
        ).last()
        val withoutSchedule = buildRecentReviewCompletionStats(emptyList(), today).last()

        assertEquals(100, withSchedule.completionRatePercent)
        assertNull(withoutSchedule.completionRatePercent)
    }

    @Test
    fun `전체 7일에 일정이 없으면 모든 완료율이 null이다`() {
        val stats = buildRecentReviewCompletionStats(emptyList(), today)

        assertTrue(stats.all { it.dueCount == 0 && it.completionRatePercent == null })
    }

    @Test
    fun `완료 수가 예정 수를 초과해도 완료율은 100으로 제한한다`() {
        val stat = DailyReviewCompletionStat(today, dueCount = 2, completedCount = 3)

        assertEquals(100, stat.completionRatePercent)
    }

    @Test
    fun `일정이 없는 날에는 유효한 선분이 끊긴다`() {
        val stats = buildRecentReviewCompletionStats(
            schedules = schedules(today.minusDays(6), 1, 1) +
                schedules(today.minusDays(4), 1, 1) +
                schedules(today.minusDays(3), 1, 1),
            today = today
        )

        val segments = splitValidReviewCompletionStatSegments(stats)

        assertEquals(
            listOf(
                listOf(0),
                listOf(2, 3)
            ),
            segments.map { segment -> segment.map { it.first } }
        )
    }

    @Test
    fun `사용자 지정 간격으로 생성된 일정도 날짜와 완료 상태만으로 포함한다`() {
        val stat = buildRecentReviewCompletionStats(
            listOf(
                ReviewCompletionSchedule(
                    id = "custom-7-day",
                    dueDate = today,
                    isCompleted = true,
                    sourceId = "custom-block",
                    reviewOrder = 1
                )
            ),
            today
        ).last()

        assertEquals(1, stat.dueCount)
        assertEquals(1, stat.completedCount)
        assertEquals(100, stat.completionRatePercent)
    }

    @Test
    fun `remaining count is due count minus completed count and cannot be negative`() {
        val normal = DailyReviewCompletionStat(today, dueCount = 5, completedCount = 3)
        val invalid = DailyReviewCompletionStat(today, dueCount = 2, completedCount = 4)

        assertEquals(2, remainingCount(normal))
        assertEquals(0, remainingCount(invalid))
    }

    @Test
    fun `maximum due count in the recent seven days can be used as the chart height reference`() {
        val stats = listOf(
            DailyReviewCompletionStat(today.minusDays(2), 3, 1),
            DailyReviewCompletionStat(today.minusDays(1), 10, 5),
            DailyReviewCompletionStat(today, 5, 5)
        )

        assertEquals(10, stats.maxOf { it.dueCount.coerceAtLeast(0) }.coerceAtLeast(1))
    }

    @Test
    fun `a day with no schedule has zero remaining reviews`() {
        val stat = DailyReviewCompletionStat(today, dueCount = 0, completedCount = 0)

        assertEquals(0, remainingCount(stat))
    }

    @Test
    fun `clamped completed count plus remaining count equals due count`() {
        val stats = listOf(
            DailyReviewCompletionStat(today.minusDays(1), dueCount = 5, completedCount = 3),
            DailyReviewCompletionStat(today, dueCount = 2, completedCount = 4)
        )

        stats.forEach { stat ->
            val completed = stat.completedCount.coerceIn(0, stat.dueCount)
            assertEquals(stat.dueCount, completed + remainingCount(stat))
        }
    }

    @Test
    fun `같은 영속 일정 ID가 중복으로 전달되어도 한 번만 집계한다`() {
        val duplicate = ReviewCompletionSchedule(
            id = "persisted-schedule",
            dueDate = today,
            isCompleted = true
        )

        val stat = buildRecentReviewCompletionStats(
            schedules = listOf(duplicate, duplicate.copy(isCompleted = false)),
            today = today
        ).last()

        assertEquals(1, stat.dueCount)
        assertEquals(1, stat.completedCount)
    }

    private fun remainingCount(stat: DailyReviewCompletionStat): Int {
        val completed = stat.completedCount.coerceIn(0, stat.dueCount)
        return (stat.dueCount - completed).coerceAtLeast(0)
    }

    private fun schedules(
        date: LocalDate,
        due: Int,
        completed: Int
    ): List<ReviewCompletionSchedule> =
        (0 until due).map { index ->
            ReviewCompletionSchedule(
                id = "$date-$index",
                dueDate = date,
                isCompleted = index < completed,
                sourceId = date.toString(),
                reviewOrder = index
            )
        }
}
