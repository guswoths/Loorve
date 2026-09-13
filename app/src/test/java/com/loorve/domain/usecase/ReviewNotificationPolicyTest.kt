package com.loorve.domain.usecase

import com.loorve.domain.model.ReviewScheduleItem
import com.loorve.domain.review.ReviewPlanStatus
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewNotificationPolicyTest {
    private val zone = ZoneId.of("Asia/Seoul")

    @Test
    fun `정규 복습은 오전과 미완료 알림을 생성한다`() {
        val date = LocalDate.of(2026, 10, 2)
        val item = ReviewScheduleItem(
            id = "r1",
            studyRecordId = "record",
            reviewDate = date.atStartOfDay(zone).toInstant().toEpochMilli()
        )

        val events = item.notificationEvents("user", zone)

        assertEquals(2, events.size)
        assertTrue(events.all { it.scheduledDate == date })
        assertEquals(9, events.first().triggerAtMillis.let {
            java.time.Instant.ofEpochMilli(it).atZone(zone).hour
        })
    }

    @Test
    fun `시험 버퍼 상태 표식에는 자동 알림을 생성하지 않는다`() {
        val item = ReviewScheduleItem(
            id = "status",
            studyRecordId = "record",
            reviewDate = LocalDate.of(2026, 10, 1).atStartOfDay(zone).toInstant().toEpochMilli(),
            planStatus = ReviewPlanStatus.CRAM_MODE_REQUIRED
        )

        assertTrue(item.notificationEvents("user", zone).isEmpty())
    }
}
