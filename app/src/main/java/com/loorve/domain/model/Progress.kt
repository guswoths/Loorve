package com.loorve.domain.model

data class Progress(
    val progressId: String = "",
    val examId: String = "",
    val content: String = "",
    val completedCount: Int = 0,
    val totalCount: Int = 0,
    val isCompleted: Boolean = false,
    val createdAt: Long = 0L   // epoch ms (KST 당일 자정)
)
