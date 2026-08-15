package com.loorve.data.mapper

import com.loorve.data.model.UserDto  // Data Layer DTO (별도 생성 필요)
import com.loorve.domain.model.User
import com.google.firebase.auth.FirebaseUser

/**
 * Data Layer ↔ Domain Layer 변환 매퍼
 * Mapper 함수는 확장 함수 형태로 작성하여 호출 가독성을 높입니다.
 */

/** UserDto → User (Domain) */
fun UserDto.toDomain(firebaseUser: FirebaseUser? = null): User = User(
    id = this.id,
    email = firebaseUser?.email ?: this.email ?: "",  // ✅ firebaseUser 우선, fallback은 DTO
    nickname = this.nickname ?: "",
    profileImageUrl = this.profileImageUrl ?: "",
    createdAt = this.createdAt,
    updatedAt = this.updatedAt
)

/** User (Domain) → UserDto */
fun User.toDto(): UserDto = UserDto(
    id = this.id,
    email = this.email,
    nickname = this.nickname,
    profileImageUrl = this.profileImageUrl,
    createdAt = this.createdAt,
    updatedAt = this.updatedAt
)
