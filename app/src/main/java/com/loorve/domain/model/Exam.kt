package com.loorve.domain.model

import java.time.LocalDate

/**
 * Domain Layer - Exam 도메인 모델
 *
 * 클린 아키텍처 원칙에 따라 순수 Kotlin으로 작성된 불변 데이터 클래스.
 * Android 프레임워크 또는 외부 라이브러리(Room, Retrofit 등)에 의존하지 않습니다.
 *
 * ※ Room Entity가 필요한 경우: data 레이어에 별도 ExamEntity.kt를 생성하고
 *    Domain ↔ Data 간 Mapper를 통해 변환하는 것을 권장합니다.
 */
data class Exam(
    /** 과목명 */
    val subjectName: String,

    /** 시험일 (java.time.LocalDate, minSdk 26 이상이므로 desugaring 불필요) */
    val examDate: Long
)
