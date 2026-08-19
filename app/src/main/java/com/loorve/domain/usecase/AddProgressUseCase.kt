package com.loorve.domain.usecase

import com.loorve.domain.model.Progress
import com.loorve.domain.repository.ProgressRepository
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/**
 * 새로운 진도를 추가하는 UseCase
 *
 * 클린 아키텍처 원칙에 따라 domain 레이어에 위치하며,
 * Firebase, Android 프레임워크 등 어떠한 외부 라이브러리에도 직접 의존하지 않습니다.
 * 외부 의존성은 오직 [ProgressRepository] 인터페이스를 통해 역전(Inversion of Control)됩니다.
 *
 * @param progressRepository 진도 관련 데이터 작업을 처리하는 [ProgressRepository] 구현체 (DI 주입)
 */
class AddProgressUseCase @Inject constructor(
    private val progressRepository: ProgressRepository
) {

    /**
     * 새로운 진도를 저장합니다.
     *
     * @param uid 현재 사용자의 ID.
     * @param progress 저장할 [Progress] 객체.
     * @return 저장 성공 시 [Result.success(Unit)], 실패 시 [Result.failure].
     */
    suspend operator fun invoke(uid: String, progress: Progress): Result<Unit> {
        if (uid.isBlank()) {
            return Result.failure(IllegalArgumentException("사용자 ID가 유효하지 않습니다."))
        }
        if (progress.content.isBlank()) {
            return Result.failure(IllegalArgumentException("진도 내용이 비어있을 수 없습니다."))
        }
        if (progress.examId.isBlank()) {
            return Result.failure(IllegalArgumentException("시험 ID가 유효하지 않습니다."))
        }

        val todayStartEpochMs = LocalDate
            .now(ZoneId.of("Asia/Seoul"))
            .atStartOfDay(ZoneId.of("Asia/Seoul"))
            .toInstant()
            .toEpochMilli()

        val progressWithToday = progress.copy(createdAt = todayStartEpochMs)

        return try {
            progressRepository.saveProgress(uid, progressWithToday)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
