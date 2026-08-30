// app/src/main/java/com/loorve/domain/model/ReviewSchedule.kt
package com.loorve.domain.model

import com.google.firebase.firestore.DocumentId

/**
 * 복습 일정 도메인 모델
 * Firestore 역직렬화를 위해 기본 생성자(no-arg) 필요 → 모든 필드에 기본값 부여
 */
data class ReviewSchedule(
    @DocumentId
    val reviewScheduleId: String = "",
    val uid: String = "",
    val originProgressId: String = "",
    val reviewDate: Long = 0L,         // Unix epoch ms (Asia/Seoul 기준)
    val scheduledDate: String = "",    // ISO 8601 "YYYY-MM-DD"
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val reviewRound: Int = 1,          // 1차, 2차, 3차 복습 회차
    val title: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,          // ✅ SaveProgressAndScheduleUseCase에서 사용
    val scheduleType: String = "FORGETTING_CURVE"  // ✅ 추가: "FORGETTING_CURVE" | "MANUAL"
)