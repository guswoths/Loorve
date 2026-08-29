// app/src/main/java/com/loorve/domain/model/ReviewBlock.kt
package com.loorve.domain.model

import com.google.firebase.firestore.PropertyName

data class ReviewBlock(
    val blockId: String = "",
    val title: String = "",
    val content: String = "",
    // ✅ 핵심: Firestore rules의 request.resource.data.uid 검증을 통과하기 위해 필수
    val uid: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val targetExamId: String = "",
    val reviewRound: Int = 1
)