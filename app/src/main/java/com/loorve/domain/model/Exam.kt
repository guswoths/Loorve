package com.loorve.domain.model

data class Exam(
    val id: String = "",
    val subjectName: String = "",
    val examDate: Long = 0L,
    val createdBy: String = ""   // Firestore 보안 규칙용 소유자 uid
)
