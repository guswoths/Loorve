// app/src/main/java/com/loorve/domain/model/ReviewBlock.kt
package com.loorve.domain.model

import com.google.firebase.firestore.DocumentId

/**
 * Domain Layer - 복습 블록 데이터 모델
 *
 * 사용자가 특정 날짜에 직접 추가한 복습 블록을 나타냅니다.
 * Firebase Firestore 직렬화를 위해 기본값이 있는 data class로 정의합니다.
 */
data class ReviewBlock(
    @DocumentId
    val reviewBlockId: String = "",
    val uid: String = "",
    val title: String = "",
    val description: String = "",
    val date: String = "",          // ISO 8601 형식: "yyyy-MM-dd"
    val startTimeMinutes: Int = 0,  // 자정으로부터의 분 (예: 9시 = 540)
    val endTimeMinutes: Int = 0,    // 자정으로부터의 분
    val colorHex: String = "#4CAF50",
    val isCompleted: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)