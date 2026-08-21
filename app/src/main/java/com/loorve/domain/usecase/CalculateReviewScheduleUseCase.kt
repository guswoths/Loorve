package com.loorve.domain.usecase

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.max
import kotlin.math.roundToInt
import javax.inject.Inject

/**
 * 망각곡선(Forgetting Curve) 기반 복습 일정 자동 계산 UseCase.
 *
 * - 시험일까지 30일 이상: 표준 간격(1, 3, 7, 14, 30일) 그대로 적용
 * - 시험일까지 30일 미만: scaleFactor = remainingDays / 30.0 으로 비례 압축
 * - 시험일을 초과하는 복습일은 자동 제외
 * - studyEndDate가 주어진 경우 해당 날짜를 초과하는 복습 회차도 자동 제외
 *
 * 외부 의존성 없음 — 순수 도메인 계산 함수.
 */
class CalculateReviewScheduleUseCase @Inject constructor() {

    private val standardIntervals = listOf(1L, 3L, 7L, 14L, 30L)

    /**
     * 복습 일정을 계산한다.
     *
     * @param progressDate  학습(진도) 완료일
     * @param examDate      시험일
     * @param studyEndDate  학습 종료 희망일 (null이면 무시, examDate 기준으로만 필터)
     * @return 복습 예정일 목록 (시험일 미포함 가능, 오름차순)
     * @throws InvalidScheduleException progressDate >= examDate 인 경우
     */
    fun execute(
        progressDate: LocalDate,
        examDate: LocalDate,
        studyEndDate: LocalDate? = null
    ): List<LocalDate> {
        if (!progressDate.isBefore(examDate)) {
            throw InvalidScheduleException(
                "진도 작성일($progressDate)은 시험일($examDate) 이전이어야 합니다. " +
                        "시험일 이후 또는 같은 날은 유효하지 않습니다. (invalid)"
            )
        }

        // studyEndDate와 examDate 중 더 이른 날짜를 실질적 cutoff로 사용
        val cutoffDate: LocalDate = if (studyEndDate != null && studyEndDate.isBefore(examDate)) {
            studyEndDate
        } else {
            examDate
        }

        val remainingDays = ChronoUnit.DAYS.between(progressDate, examDate)

        return if (remainingDays >= 30) {
            // 표준 간격 그대로 적용
            standardIntervals
                .map { interval -> progressDate.plusDays(interval) }
                .filter { !it.isAfter(cutoffDate) }
        } else {
            // 비례 압축 적용
            val scaleFactor = remainingDays / 30.0
            standardIntervals
                .map { interval ->
                    val compressed = max(1, (interval * scaleFactor).roundToInt()).toLong()
                    progressDate.plusDays(compressed)
                }
                .filter { !it.isAfter(cutoffDate) }
                .distinct()
                .sorted()
        }
    }
}

class InvalidScheduleException(message: String) : IllegalArgumentException(message)