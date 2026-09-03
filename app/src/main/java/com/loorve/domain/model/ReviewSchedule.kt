package com.loorve.domain.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

data class ReviewSchedule(
    @DocumentId
    val scheduleId: String = "",       // scheduleId = reviewScheduleId 역할
    val blockId: String = "",
    @get:PropertyName("uid") @set:PropertyName("uid")
    var userId: String = "",
    val originProgressId: String = "", // ← 추가
    val title: String = "",
    val reviewDate: Long = 0L,
    val reviewDateText: String = "",
    val reviewOrder: Int = 0,          // reviewOrder = reviewRound 역할
    val scheduleType: String = "EBBINGHAUS",
    val isCompleted: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)