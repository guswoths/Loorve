package com.loorve.domain.usecase

import com.loorve.domain.model.Exam
import com.loorve.domain.repository.ExamRepository
import javax.inject.Inject

/**
 * 새로운 시험을 추가하는 UseCase
 *
 * 클린 아키텍처 원칙에 따라 domain 레이어에 위치하며,
 * Firebase, Android 프레임워크 등 어떠한 외부 라이브러리에도 직접 의존하지 않습니다.
 * 외부 의존성은 오직 [ExamRepository] 인터페이스를 통해 역전(Inversion of Control)됩니다.
 *
 * @param examRepository 시험 관련 데이터 작업을 처리하는 [ExamRepository] 구현체 (DI 주입)
 */
class AddExamUseCase @Inject constructor(
    private val examRepository: ExamRepository
) {

    /**
     * 새로운 시험을 저장합니다.
     *
     * @param exam 저장할 [Exam] 객체.
     * @return 저장 성공 시 [Result.success(Unit)], 실패 시 [Result.failure].
     */
    suspend operator fun invoke(exam: Exam): Result<Unit> {
        if (exam.subjectName.isBlank()) {
            return Result.failure(IllegalArgumentException("과목명은 비어있을 수 없습니다."))
        }
        if (exam.examDate <= 0L) {
            return Result.failure(IllegalArgumentException("시험일이 올바르지 않습니다."))
        }
        return try {
            examRepository.addExam(exam)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
