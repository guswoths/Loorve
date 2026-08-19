package com.loorve.domain.model

/**
 * 학습 진도 도메인 모델.
 * Firestore users/{uid}/progresses 서브컬렉션에 저장되는 데이터 구조.
 */
data class Progress(
    val id: String = "",                // Firestore 문서 ID (auto-id)
    val examId: String = "",            // 연결된 시험 ID (Exam 도메인 모델의 식별자)
    val content: String = "",           // 학습 진도 텍스트 내용
    val createdAt: Long = 0L            // 작성일 (epoch ms, 서버 타임스탬프 변환 후 저장)
)
