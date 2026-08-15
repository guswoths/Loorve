// 파일: app/src/main/java/com/loorve/data/mapper/UserMapper.kt
package com.loorve.data.mapper

import com.loorve.data.model.UserDto          // 예: Retrofit 응답 DTO
import com.loorve.data.local.entity.UserEntity // 예: Room Entity
import com.loorve.domain.model.User

// ── DTO → Domain ──────────────────────────────────────────
fun UserDto.toDomain(): User = User(
    id = id,
    email = email,
    nickname = nickname,
    profileImageUrl = profileImageUrl,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

// ── Domain → DTO (필요 시) ────────────────────────────────
fun User.toDto(): UserDto = UserDto(
    id = id,
    email = email,
    nickname = nickname,
    profileImageUrl = profileImageUrl,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

// ── Room Entity → Domain ──────────────────────────────────
fun UserEntity.toDomain(): User = User(
    id = id,
    email = email,
    nickname = nickname,
    profileImageUrl = profileImageUrl,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

// ── Domain → Room Entity ──────────────────────────────────
fun User.toEntity(): UserEntity = UserEntity(
    id = id,
    email = email,
    nickname = nickname,
    profileImageUrl = profileImageUrl,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
