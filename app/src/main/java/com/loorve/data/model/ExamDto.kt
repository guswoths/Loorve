package com.loorve.data.model

import com.loorve.domain.model.Exam

data class ExamDto(
    val id: String = "",
    val subjectName: String = "",
    val examDate: Long = 0L
) {
    fun toDomain(): Exam = Exam(subjectName = subjectName, examDate = examDate)
}
