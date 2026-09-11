package com.loorve.domain.review

import java.time.LocalDate

object SchedulingMessages {
    fun blockedExamDate(
        availableDays: Long,
        finalReviewBufferDays: Int,
        earliestExamDate: LocalDate
    ): String =
        "현재 설정에서는 시험 전 정규 복습에 사용할 수 있는 기간이 ${availableDays}일뿐입니다. " +
            "최소 3일의 유효 학습기간과 시험 전 ${finalReviewBufferDays}일 버퍼가 필요합니다. " +
            "시험일을 ${earliestExamDate} 이후로 설정해 주세요."

    const val overloaded =
        "해당 날짜에 복습량이 과도합니다. 하루 최대 복습 시간을 늘리거나 시험일을 조정해 주세요."

    fun insufficientWindow(generated: Int, target: Int): String =
        "사용 가능한 날짜가 부족하여 ${target}회 중 ${generated}회의 복습만 배정했습니다."

    fun cramMode(studyRecordId: String): String =
        "학습기록 ${studyRecordId}은 정규 복습 기간이 3일보다 짧아 압축 학습이 필요합니다."

    fun registrationSummary(
        generated: Int,
        firstDate: LocalDate?,
        lastDate: LocalDate?,
        daysUntilExam: Long,
        compressed: Boolean,
        cramMode: Boolean
    ): String =
        "복습 ${generated}회가 생성되었습니다. 첫 복습일: ${firstDate ?: "-"}, " +
            "마지막 복습일: ${lastDate ?: "-"}, 시험까지 ${daysUntilExam}일 남았습니다. " +
            "압축 일정: ${if (compressed) "예" else "아니오"}, " +
            "압축 학습 모드: ${if (cramMode) "필요" else "아니오"}."
}
