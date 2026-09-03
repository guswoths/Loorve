package com.loorve.domain.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

data class ReviewSchedule(
    @DocumentId
    var scheduleId: String = "",       // ✅ val → var 로 변경 (@DocumentId 자동 주입 위해 필수)
    val blockId: String = "",
    @get:PropertyName("uid") @set:PropertyName("uid")
    var userId: String = "",
    val originProgressId: String = "",
    val title: String = "",
    val reviewDateText: String = "",
    val reviewOrder: Int = 0,
    val scheduleType: String = "EBBINGHAUS",
    val isCompleted: Boolean = false,
    val reviewDate: Long = 0L,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)