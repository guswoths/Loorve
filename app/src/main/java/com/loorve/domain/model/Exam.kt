package com.loorve.domain.model

/**
 * Domain Layer - Exam 도메인 모델
 *
 * 클린 아키텍처 원칙에 따라 순수 Kotlin으로 작성된 불변 데이터 클래스.
 * Android 프레임워크 또는 외부 라이브러리(Room, Retrofit 등)에 의존하지 않습니다.
 */
data class Exam(
    /** 고유 식별자 */
    val id: String,

    /** 과목명 */
    val subjectName: String,

    /** 시험일 (Unix timestamp ms) */
    val examDate: Long
)
