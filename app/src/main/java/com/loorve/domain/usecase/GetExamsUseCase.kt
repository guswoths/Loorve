package com.loorve.domain.usecase

import com.loorve.domain.model.Exam
import com.loorve.domain.repository.ExamRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 전체 시험 목록을 조회하는 UseCase
 */
class GetExamsUseCase @Inject constructor(
    private val examRepository: ExamRepository
) {

    operator fun invoke(): Flow<List<Exam>> {
        return examRepository.getExamList()
    }
}
