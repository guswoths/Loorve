package com.loorve.domain.review

import com.loorve.data.model.ReviewScheduleItemDto
import com.loorve.domain.model.ReviewScheduleItem
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class ReviewAlarmTimeTest {

    private val zone = ZoneId.of("Asia/Seoul")
    private val reviewDate = LocalDate.of(2026, 9, 20)
        .atStartOfDay(zone)
        .toInstant()
        .toEpochMilli()

    @Test
    fun `record without override uses current settings default`() {
        val item = ReviewScheduleItem(reviewDate = reviewDate)

        assertEquals(9 to 0, item.effectiveAlarmTime(9 to 0))
        assertEquals(
            LocalDate.of(2026, 9, 20).atTime(9, 0).atZone(zone).toInstant().toEpochMilli(),
            item.alarmTriggerAtMillis(9 to 0, zone)
        )
    }

    @Test
    fun `explicit override is persisted and takes precedence`() {
        val overridden = ReviewScheduleItemDto(
            id = "record_r1",
            reviewDate = reviewDate,
            customAlarmHour = 14,
            customAlarmMinute = 30
        ).toDomain()

        assertEquals(14 to 30, overridden.effectiveAlarmTime(9 to 0))
        assertEquals(
            LocalDate.of(2026, 9, 20).atTime(14, 30).atZone(zone).toInstant().toEpochMilli(),
            overridden.alarmTriggerAtMillis(9 to 0, zone)
        )
    }

    @Test
    fun `changing default affects only records without overrides`() {
        val inherited = ReviewScheduleItem(reviewDate = reviewDate)
        val overridden = inherited.copy(customAlarmTime = 14 to 30)

        assertEquals(18 to 0, inherited.effectiveAlarmTime(18 to 0))
        assertEquals(14 to 30, overridden.effectiveAlarmTime(18 to 0))
    }

    @Test
    fun `legacy dto without alarm fields falls back safely`() {
        val item = ReviewScheduleItemDto(
            id = "legacy",
            reviewDate = reviewDate
        ).toDomain()

        assertEquals(null, item.customAlarmTime)
        assertEquals(9 to 0, item.effectiveAlarmTime(9 to 0))
    }

    @Test
    fun `rescheduling keeps the same review record identity and trigger is deterministic`() {
        val item = ReviewScheduleItem(id = "record_r1", reviewDate = reviewDate)

        val first = item.alarmTriggerAtMillis(9 to 0, zone)
        val second = item.copy(customAlarmTime = 16 to 0)
            .alarmTriggerAtMillis(9 to 0, zone)

        assertEquals(item.id, item.copy(customAlarmTime = 16 to 0).id)
        assertEquals(
            LocalDate.of(2026, 9, 20).atTime(16, 0).atZone(zone).toInstant().toEpochMilli(),
            second
        )
        assertEquals(false, first == second)
    }
}
