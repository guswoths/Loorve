package com.loorve.domain.model

/**
 * Domain Layer - User 도메인 모델
 *
 * 클린 아키텍처 원칙에 따라 순수 Kotlin으로 작성된 불변 데이터 클래스.
 * Android 프레임워크 또는 외부 라이브러리(Room, Retrofit 등)에 의존하지 않습니다.
 */
data class User(
    /** UUID 기반 고유 식별자 */
    val id: String,

    /** 로그인용 이메일 주소 */
    val email: String,

    /** 앱 내 표시 이름 */
    val nickname: String,

    /** 프로필 이미지 URL (없을 경우 null) */
    val profileImageUrl: String? = null,

    /** 계정 생성 타임스탬프 (epoch milliseconds) */
    val createdAt: Long,

    /** 최근 수정 타임스탬프 (epoch milliseconds) */
    val updatedAt: Long
)
