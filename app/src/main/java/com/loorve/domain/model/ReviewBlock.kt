package com.loorve.domain.model

import com.google.firebase.firestore.DocumentId

/**
 * 특정 날짜에 표시할 복습 블록입니다.
 * Firestore 역직렬화를 위해 모든 필드에 기본값을 지정합니다.
 */
data class ReviewBlock(
    @DocumentId
    val blockId: String = "",
    val uid: String = "",
    val date: String = "",
    val title: String = "",
    val description: String = "",
    val isCompleted: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)