package com.loorve.domain.usecase

import com.loorve.domain.model.Exam
import com.loorve.domain.repository.ExamRepository
import javax.inject.Inject

class AddExamUseCase @Inject constructor(
    private val examRepository: ExamRepository
) {
    suspend operator fun invoke(exam: Exam): Result<Unit> {
        if (exam.subjectName.isBlank()) {
            return Result.failure(IllegalArgumentException("과목명은 비어있을 수 없습니다."))
        }
        if (exam.examDate <= 0L) {
            return Result.failure(IllegalArgumentException("시험일이 올바르지 않습니다."))
        }
        // studyEndDate가 설정된 경우, 시험일보다 이전이어야 함
        if (exam.studyEndDate > 0L && exam.studyEndDate >= exam.examDate) {
            return Result.failure(
                IllegalArgumentException("학습 종료일은 시험일보다 이전이어야 합니다.")
            )
        }
        if (exam.finalReviewBufferDays < 0) {
            return Result.failure(
                IllegalArgumentException("시험 전 버퍼 일수는 0 이상이어야 합니다.")
            )
        }
        if (exam.maxDailyReviewMinutes != null && exam.maxDailyReviewMinutes <= 0) {
            return Result.failure(
                IllegalArgumentException("하루 최대 복습 시간은 양수여야 합니다.")
            )
        }
        return try {
            examRepository.addExam(exam)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}