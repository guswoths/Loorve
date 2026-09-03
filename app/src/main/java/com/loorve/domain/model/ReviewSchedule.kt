package com.loorve.domain.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

data class ReviewSchedule(
    @DocumentId
    var scheduleId: String = "",        // ✅ val → var (@DocumentId 자동 주입 필수)
    val blockId: String = "",
    @get:PropertyName("uid") @set:PropertyName("uid")
    var userId: String = "",
    val originProgressId: String = "",
    val title: String = "",
    val reviewDate: Long = 0L,
    val reviewDateText: String = "",
    val reviewOrder: Int = 0,
    val scheduleType: String = "EBBINGHAUS",
    val isCompleted: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)