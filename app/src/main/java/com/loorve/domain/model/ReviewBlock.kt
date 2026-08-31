package com.loorve.domain.model

data class ReviewBlock(
    val blockId: String = "",
    val uid: String = "",
    val date: String = "",
    val title: String = "",
    val description: String = "",
    val isCompleted: Boolean = false,
    // ── 신규 추가 필드 ──
    val examDate: Long = 0L,          // 시험일 epoch ms
    val prepStartDate: Long = 0L,     // 준비 시작일 epoch ms (= 블록 생성일 기본값)
    val dailyCap: Int = 5,            // 하루 최대 복습 노출 수
    val examName: String = "",        // 시험명 (기존 title 이중화 방지용)
    // ──────────────────
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)