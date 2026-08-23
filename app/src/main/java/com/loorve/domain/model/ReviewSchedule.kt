package com.loorve.domain.model

/**
 * 망각곡선 기반 복습 스케줄 도메인 모델.
 *
 * [CalculateReviewScheduleUseCase]에 의해 생성되며,
 * 특정 학습 진도([originProgressId])에 연결된 복습 일정을 나타낸다.
 *
 * Firestore 역직렬화 호환을 위해 모든 필드에 기본값을 지정한다.
 */
data class ReviewSchedule(

    /** 복습 스케줄 고유 ID */
    val reviewScheduleId: String = "",

    /**
     * 이 복습이 연결된 원본 진도의 ID.
     * [Progress.progressId]와 1:N 관계.
     */
    val originProgressId: String = "",

    /**
     * 복습 예정일 (epoch ms, KST 당일 자정 기준).
     * MANUAL 모드에서는 사용자가 선택한 시/분까지 포함한 epoch ms를 그대로 저장.
     * ex) 2026-08-20 00:00:00 KST → 1755622800000L
     */
    val reviewDate: Long = 0L,

    /**
     * 복습 회차 (1-based).
     * 망각곡선 인터벌 계산 시 [CalculateReviewScheduleUseCase]에서 참조.
     * ex 1회차: +1일, 2회차: +3일, 3회차: +7일 ...
     * MANUAL 모드로 직접 입력 시에는 1로 고정.
     */
    val reviewRound: Int = 0,

    /** 복습 완료 여부. 사용자가 해당 회차 복습을 마치면 true로 갱신된다. */
    val isCompleted: Boolean = false,

    /** 레코드 생성 시각 (epoch ms). */
    val createdAt: Long = 0L,

    /** 레코드 최종 수정 시각 (epoch ms). isCompleted 변경 시 갱신된다. */
    val updatedAt: Long = 0L,

    /**
     * 스케줄 생성 방식.
     * - "AUTO"   : 망각곡선 자동 설정 (기본값, Firestore 기존 문서 호환)
     * - "MANUAL" : 사용자가 직접 일시를 지정한 단일 알람
     */
    val scheduleType: String = "AUTO"
)