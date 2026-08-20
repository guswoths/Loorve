package com.loorve.domain.usecase

import java.time.LocalDate

/**
 * 에빙하우스 망각곡선 기반 고정 간격 복습 스케줄러.
 * 외부 의존성 없는 순수 함수 객체 — TDD 테스트 용이.
 *
 * 간격: 1, 3, 7, 14, 30일 (총 5회차)
 */
object ForgettingCurveScheduler {

    val INTERVALS = listOf(1, 3, 7, 14, 30)

    /**
     * studyDate 기준 5개의 복습 예정일 리스트 반환.
     */
    fun generateReviewDates(studyDate: LocalDate): List<LocalDate> =
        INTERVALS.map { days -> studyDate.plusDays(days.toLong()) }

    /**
     * reviewRound(1-based) 기준 해당 회차 intervalDays 반환.
     * 범위 초과 시 null (복습 종료).
     */
    fun getIntervalForRound(round: Int): Int? =
        INTERVALS.getOrNull(round - 1)

    /**
     * 복습 완료 시 다음 단계 intervalDays 반환.
     * isCompleted=true → 다음 회차 간격 / false → 현재 회차 간격 재반환 (재스케줄)
     */
    fun nextInterval(currentRound: Int, isCompleted: Boolean): Int? {
        return if (isCompleted) {
            getIntervalForRound(currentRound + 1) // 5회차 이후 null
        } else {
            getIntervalForRound(currentRound)     // 미완료: 동일 간격 재반환
        }
    }
}
