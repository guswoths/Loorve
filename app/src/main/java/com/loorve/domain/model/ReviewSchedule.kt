// app/src/main/java/com/loorve/domain/model/ReviewSchedule.kt
package com.loorve.domain.model

import com.google.firebase.firestore.PropertyName

data class ReviewSchedule(
    val reviewScheduleId: String = "",
    val originProgressId: String = "",
    val reviewDate: Long = 0L,
    val reviewRound: Int = 1,
    val isCompleted: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val scheduleType: String = "FORGETTING_CURVE",
    val uid: String = ""   // ✅ 추가: Firestore rules의 uid 필드 검증 통과를 위해 필수
)