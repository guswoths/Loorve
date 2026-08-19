// 경로: app/src/main/java/com/loorve/data/model/ExamDto.kt
package com.loorve.data.model

import com.google.firebase.Timestamp
import com.loorve.domain.model.Exam

data class ExamDto(
    val id: String = "",
    val subjectName: String = "",
    val examDate: Long = 0L,
    val createdAt: Timestamp? = null
) {
    fun toDomain(): Exam = Exam(
        id          = id,          // ✅ id 추가
        subjectName = subjectName,
        examDate    = examDate
    )
}
