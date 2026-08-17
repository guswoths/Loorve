package com.loorve.domain.usecase

import com.loorve.domain.model.Exam
import com.loorve.domain.repository.ExamRepository
import kotlinx.coroutines.flow.Flow

/**
 * 전체 시험 목록을 조회하는 UseCase
 *
 * 클린 아키텍처 원칙에 따라 domain 레이어에 위치하며,
 * Firebase, Android 프레임워크 등 어떠한 외부 라이브러리에도 직접 의존하지 않습니다.
 * 외부 의존성은 오직 [ExamRepository] 인터페이스를 통해 역전(Inversion of Control)됩니다.
 *
 * @param examRepository 시험 관련 데이터 작업을 처리하는 [ExamRepository] 구현체 (DI 주입)
 */
class GetExamsUseCase(
    private val examRepository: ExamRepository
) {

    /**
     * 전체 시험 목록을 실시간으로 관찰합니다.
     *
     * [ExamRepository.getExamList]를 호출하여 반환된 [Flow]를 그대로 전달합니다.
     * 데이터가 변경될 때마다 최신 목록이 자동으로 emit되며,
     * 목록이 비어 있으면 빈 리스트가 emit됩니다.
     *
     * @return 시험 목록([List<Exam>])을 방출하는 [Flow].
     *         오류 발생 시 Flow 내부에서 예외가 방출될 수 있으므로
     *         호출자(ViewModel 등)에서 catch 처리를 권장합니다.
     */
    operator fun invoke(): Flow<List<Exam>> {
        return examRepository.getExamList()
    }
}
