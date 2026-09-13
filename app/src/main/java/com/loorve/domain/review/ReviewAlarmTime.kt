package com.loorve.domain.review

import com.loorve.domain.model.ReviewScheduleItem
import java.time.Instant
import java.time.ZoneId

fun ReviewScheduleItem.effectiveAlarmTime(defaultAlarmTime: Pair<Int, Int>): Pair<Int, Int> =
    customAlarmTime ?: defaultAlarmTime

fun ReviewScheduleItem.alarmTriggerAtMillis(
    defaultAlarmTime: Pair<Int, Int>,
    zoneId: ZoneId = ZoneId.systemDefault()
): Long = alarmTriggerAtMillis(reviewDate, effectiveAlarmTime(defaultAlarmTime), zoneId)

fun alarmTriggerAtMillis(
    reviewDate: Long,
    alarmTime: Pair<Int, Int>,
    zoneId: ZoneId = ZoneId.systemDefault()
): Long {
    val (hour, minute) = alarmTime
    return Instant.ofEpochMilli(reviewDate)
        .atZone(zoneId)
        .toLocalDate()
        .atTime(hour, minute)
        .atZone(zoneId)
        .toInstant()
        .toEpochMilli()
}

fun nextAlarmTriggerAtMillis(
    reviewDate: Long,
    alarmTime: Pair<Int, Int>,
    now: Long = System.currentTimeMillis(),
    zoneId: ZoneId = ZoneId.systemDefault()
): Long {
    var date = Instant.ofEpochMilli(reviewDate).atZone(zoneId).toLocalDate()
    var trigger = date.atTime(alarmTime.first, alarmTime.second)
        .atZone(zoneId).toInstant().toEpochMilli()
    while (trigger <= now) {
        date = date.plusDays(1)
        trigger = date.atTime(alarmTime.first, alarmTime.second)
            .atZone(zoneId).toInstant().toEpochMilli()
    }
    return trigger
}
