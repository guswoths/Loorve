package com.loorve.domain.usecase

import java.time.LocalDate

/**
 * 망각곡선 기반 복습 스케줄 계산 UseCase
 * TDD: 이 파일은 테스트 컴파일을 위한 stub입니다.
 * 실제 구현은 테스트 통과 후 작성하세요.
 */
class CalculateReviewScheduleUseCase {
    fun execute(progressDate: LocalDate, examDate: LocalDate): List<LocalDate> {
        TODO("구현 필요: 테스트 작성 후 구현할 것")
    }
}

/** 커스텀 도메인 예외: 작성일이 시험일과 같거나 이후인 경우 */
class InvalidScheduleException(message: String) : IllegalArgumentException(message)
