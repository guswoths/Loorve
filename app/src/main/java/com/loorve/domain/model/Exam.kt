package com.loorve.domain.model

data class Exam(
    val id: String = "",
    val subjectName: String = "",
    val examDate: Long = 0L,
    val studyEndDate: Long = 0L,   // 학습 종료일 (epoch ms, KST 자정 기준). 0이면 미설정
    val createdBy: String = ""
)