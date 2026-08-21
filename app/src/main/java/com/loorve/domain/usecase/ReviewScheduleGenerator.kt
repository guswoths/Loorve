package com.loorve.domain.usecase

import com.loorve.domain.model.ReviewSchedule
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject

/**
 * 진도 입력(studyDate) 기반 ReviewSchedule 객체 생성 UseCase.
 * ForgettingCurveScheduler를 사용하는 조합 레이어.
 */
class ReviewScheduleGenerator @Inject constructor() {

    companion object {
        private val KST = ZoneId.of("Asia/Seoul")
    }

    /**
     * @param originProgressId  원본 진도 ID
     * @param studyDate         학습 완료일 (LocalDate)
     * @param studyEndDate      학습 종료일 (null이면 기존 동작 유지, 최대 5회차 생성)
     * @return ReviewSchedule 리스트 (studyEndDate 적용 시 5개 미만 가능)
     */
    fun generate(
        originProgressId: String,
        studyDate: LocalDate,
        studyEndDate: LocalDate? = null      // 추가
    ): List<ReviewSchedule> {
        require(originProgressId.isNotBlank()) { "originProgressId는 비어 있을 수 없습니다." }

        val now = System.currentTimeMillis()
        return ForgettingCurveScheduler.generateReviewDates(studyDate, studyEndDate)
            .mapIndexed { index, reviewDate ->
                val reviewDateMs = reviewDate
                    .atStartOfDay(KST)
                    .toInstant()
                    .toEpochMilli()
                ReviewSchedule(
                    reviewScheduleId = UUID.randomUUID().toString(),
                    originProgressId = originProgressId,
                    reviewDate       = reviewDateMs,
                    reviewRound      = index + 1,
                    isCompleted      = false,
                    createdAt        = now,
                    updatedAt        = now
                )
            }
    }
}