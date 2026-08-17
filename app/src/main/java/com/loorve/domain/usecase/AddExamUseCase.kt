package com.loorve.domain.usecase

import com.loorve.domain.model.Exam
import com.loorve.domain.repository.ExamRepository

/**
 * 새로운 시험을 추가하는 UseCase
 *
 * 클린 아키텍처 원칙에 따라 domain 레이어에 위치하며,
 * Firebase, Android 프레임워크 등 어떠한 외부 라이브러리에도 직접 의존하지 않습니다.
 * 외부 의존성은 오직 [ExamRepository] 인터페이스를 통해 역전(Inversion of Control)됩니다.
 *
 * @param examRepository 시험 관련 데이터 작업을 처리하는 [ExamRepository] 구현체 (DI 주입)
 */
class AddExamUseCase(
    private val examRepository: ExamRepository
) {

    /**
     * 새로운 시험을 저장합니다.
     *
     * 입력값 유효성 검사를 수행한 후 [ExamRepository.addExam]을 호출합니다.
     * 발생 가능한 모든 예외를 [Result.failure]로 래핑하여 호출자에게 전달합니다.
     *
     * @param exam 저장할 [Exam] 객체.
     *             [Exam.subjectName]이 blank이거나 [Exam.examDate]가 0 이하이면
     *             [IllegalArgumentException]으로 실패합니다.
     *
     * ⚠️ 보안 주의사항:
     *  - exam 데이터를 로그(Logcat 등)에 출력하지 마세요.
     *  - examDate는 클라이언트에서 임의로 조작될 수 있으므로,
     *    구현체(서버 또는 보안 규칙)에서 반드시 유효성을 재검증하세요.
     *
     * @return 저장 성공 시 [Result.success(Unit)],
     *         실패 시 [Result.failure]와 함께 발생한 예외를 반환합니다.
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
