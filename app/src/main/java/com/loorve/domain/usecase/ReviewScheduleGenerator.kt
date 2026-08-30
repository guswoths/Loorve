package com.loorve.domain.usecase

import com.loorve.domain.model.ReviewSchedule
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject

class ReviewScheduleGenerator @Inject constructor() {

    private val zoneId = ZoneId.of("Asia/Seoul")

    /**
     * 에빙하우스 기본 복습 간격을 기준으로 복습 일정을 생성합니다.
     *
     * @param userId Firebase Authentication UID
     * @param blockId 이 일정들이 속하는 복습 블록 ID
     * @param title 복습할 시험/학습 항목 이름
     * @param startDate 복습 시작 날짜
     * @param examDate 시험일. 이 날짜를 넘는 일정은 생성하지 않습니다.
     * @param cycleOption 0: 에빙하우스(1, 3, 7, 14, 30일), 1: 기본(1, 3, 7일)
     */
    operator fun invoke(
        userId: String,
        blockId: String,
        title: String,
        startDate: LocalDate,
        examDate: LocalDate,
        cycleOption: Int
    ): List<ReviewSchedule> {
        require(userId.isNotBlank()) {
            "사용자 ID가 비어 있습니다."
        }

        require(blockId.isNotBlank()) {
            "복습 블록 ID가 비어 있습니다."
        }

        require(title.isNotBlank()) {
            "복습 제목이 비어 있습니다."
        }

        require(!examDate.isBefore(startDate)) {
            "시험일은 복습 시작일보다 빠를 수 없습니다."
        }

        val reviewIntervals = when (cycleOption) {
            1 -> listOf(1L, 3L, 7L)
            else -> listOf(1L, 3L, 7L, 14L, 30L)
        }

        val now = System.currentTimeMillis()

        return reviewIntervals
            .map { interval ->
                startDate.plusDays(interval)
            }
            .filter { reviewDate ->
                !reviewDate.isAfter(examDate)
            }
            .distinct()
            .sorted()
            .mapIndexed { index, reviewDate ->
                ReviewSchedule(
                    scheduleId = UUID.randomUUID().toString(),
                    blockId = blockId,
                    userId = userId,
                    title = title.trim(),
                    reviewDate = reviewDate
                        .atStartOfDay(zoneId)
                        .toInstant()
                        .toEpochMilli(),
                    reviewDateText = reviewDate.toString(),
                    reviewOrder = index + 1,
                    scheduleType = if (cycleOption == 0) {
                        "EBBINGHAUS"
                    } else {
                        "CUSTOM"
                    },
                    isCompleted = false,
                    createdAt = now,
                    updatedAt = now
                )
            }
    }
}