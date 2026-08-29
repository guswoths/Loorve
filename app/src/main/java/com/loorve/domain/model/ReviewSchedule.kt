// app/src/main/java/com/loorve/domain/model/ReviewSchedule.kt
package com.loorve.domain.model

import androidx.annotation.Keep

/**
 * Firestore toObject() 역직렬화를 위해 모든 필드에 기본값을 부여하고,
 * @Keep으로 ProGuard/R8 난독화 방지
 */
@Keep
data class ReviewSchedule(
    val reviewScheduleId: String = "",
    val originProgressId: String = "",
    val reviewDate: Long = 0L,          // Firestore timestamp는 Long(epoch ms)으로 통일
    val reviewRound: Int = 0,           // 복습 회차 (1부터 시작)
    val isCompleted: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,           // 업데이트 시각
    val uid: String = "",               // 소유자 UID (Firestore 보안 규칙용)
    val scheduledDate: String = ""      // ISO 8601 문자열 필드 (있을 경우)
)