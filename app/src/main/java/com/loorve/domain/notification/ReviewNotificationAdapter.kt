package com.loorve.domain.notification

import java.time.ZoneId

data class ReviewNotificationPreferences(
    val timezone: ZoneId = ZoneId.of("Asia/Seoul"),
    val pushEnabled: Boolean = true,
    val quietHoursStart: Int? = null,
    val quietHoursEnd: Int? = null
)

interface ReviewNotificationAdapter {
    suspend fun schedule(
        event: ReviewNotificationOutbox,
        preferences: ReviewNotificationPreferences = ReviewNotificationPreferences()
    ): Result<Unit>
    fun cancel(event: ReviewNotificationOutbox): Result<Unit>
}
