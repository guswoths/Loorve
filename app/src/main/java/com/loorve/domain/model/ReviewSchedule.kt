package com.loorve.domain.model

import com.google.firebase.firestore.DocumentId

data class ReviewSchedule(
    @DocumentId
    val scheduleId: String = "",
    val blockId: String = "",
    val userId: String = "",
    val title: String = "",
    val reviewDate: Long = 0L,
    val reviewDateText: String = "",
    val reviewOrder: Int = 0,
    val scheduleType: String = "EBBINGHAUS",
    val isCompleted: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)