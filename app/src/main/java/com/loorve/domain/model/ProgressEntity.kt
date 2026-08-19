package com.loorve.domain.model

import com.google.firebase.Timestamp

data class ProgressEntity(
    val progressId: String = "",
    val uid: String = "",
    val examId: String = "",
    val subjectName: String = "",
    val content: String = "",
    val studyDate: String = "",
    val reviewCount: Int = 0,
    val isCompleted: Boolean = false,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)
