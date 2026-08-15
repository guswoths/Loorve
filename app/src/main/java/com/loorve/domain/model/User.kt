// 파일: app/src/main/java/com/loorve/domain/model/User.kt
package com.loorve.domain.model

/**
 * 레이어: Domain Layer (Clean Architecture)
 *
 * 순수 Kotlin 도메인 모델. Android 프레임워크 및 외부 라이브러리(Room, Retrofit 등)
 * 의존성 없이 작성되었습니다.
 *
 * @property id              UUID 기반 고유 식별자
 * @property email           로그인용 이메일
 * @property nickname        앱 내 표시 이름
 * @property profileImageUrl 프로필 이미지 URL (없을 경우 null)
 * @property createdAt       계정 생성 타임스탬프 (epoch millis)
 * @property updatedAt       최근 수정 타임스탬프 (epoch millis)
 */
data class User(
    val id: String,
    val email: String,
    val nickname: String,
    val profileImageUrl: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
) {
    companion object {
        /**
         * 새 사용자 생성 시 편의 팩토리 함수.
         * createdAt과 updatedAt을 현재 시간으로 자동 설정합니다.
         */
        fun create(
            id: String,
            email: String,
            nickname: String,
            profileImageUrl: String? = null,
            now: Long = System.currentTimeMillis(),
        ): User = User(
            id = id,
            email = email,
            nickname = nickname,
            profileImageUrl = profileImageUrl,
            createdAt = now,
            updatedAt = now,
        )
    }
}
