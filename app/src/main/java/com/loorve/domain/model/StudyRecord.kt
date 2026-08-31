package com.loorve.domain.model

data class StudyRecord(
    val id: String = "",
    val uid: String = "",
    val blockId: String = "",
    val examId: String = "",
    val title: String = "",        // 학습 진도 내용 (앞 20자를 제목으로 활용)
    val content: String = "",
    val learningDate: Long = 0L,   // epoch ms, KST 자정 기준
    val examDate: Long = 0L,       // epoch ms, KST 자정 기준
    val prepStartDate: Long = 0L,  // 시험 준비 시작일 (전체 준비기간 계산용)
    val recommendedCompletionDate: Long = 0L, // 최소 1회독 권장 완료일
    val stage: Int = 0,
    val successCount: Int = 0,
    val stability: Double = 1.0,
    val completionRate: Double = 0.0,
    val plannedReviewCount: Int = 0,
    val completedReviewCount: Int = 0,
    val isAtRisk: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)