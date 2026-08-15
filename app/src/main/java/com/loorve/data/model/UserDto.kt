package com.loorve.data.model

/**
 * Data Layer DTO - Firestore 문서와 1:1 매핑
 * domain 레이어의 User 모델과 분리하여 외부 의존성 격리
 */
data class UserDto(
    val id: String = "",
    val email: String = "",
    val nickname: String? = null,
    val profileImageUrl: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)
