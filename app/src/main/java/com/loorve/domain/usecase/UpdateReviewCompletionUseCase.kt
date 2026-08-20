package com.loorve.domain.usecase

import com.loorve.domain.repository.ReviewScheduleRepository
import javax.inject.Inject

/**
 * 복습 일정의 완료 여부를 토글(업데이트)하는 UseCase.
 *
 * @param repository ReviewScheduleRepository
 */
class UpdateReviewCompletionUseCase @Inject constructor(
    private val repository: ReviewScheduleRepository
) {
    /**
     * @param uid 현재 인증된 사용자 UID
     * @param scheduleId 변경할 복습 일정 ID
     * @param isCompleted 변경할 완료 여부
     */
    suspend operator fun invoke(
        uid: String,
        scheduleId: String,
        isCompleted: Boolean
    ): Result<Unit> {
        require(uid.isNotBlank()) { "uid는 비어 있을 수 없습니다." }
        require(scheduleId.isNotBlank()) { "scheduleId는 비어 있을 수 없습니다." }
        return repository.updateReviewCompletion(uid, scheduleId, isCompleted)
    }
}
