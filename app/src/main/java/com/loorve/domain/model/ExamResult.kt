package com.loorve.domain.model

/**
 * 시험 결과 도메인 모델.
 * Firestore users/{uid}/exams 서브컬렉션에 저장되는 데이터 구조.
 */
data class ExamResult(
    val id: String = "",                // Firestore 문서 ID (auto-id)
    val userId: String = "",            // 소유 사용자 uid
    val examId: String = "",            // 연관된 시험 ID
    val score: Float = 0f,              // 점수 (소수점 지원)
    val createdAt: Long = 0L            // 생성 시각 (epoch ms, 서버 타임스탬프 변환 후 저장)
)
