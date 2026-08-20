package com.loorve.domain.usecase

import android.util.Log
import com.loorve.domain.model.Progress
import com.loorve.domain.model.ReviewSchedule
import com.loorve.domain.repository.ExamRepository
import com.loorve.domain.repository.ReviewScheduleRepository
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject

/**
 * 진도 저장 + 망각곡선 기반 복습 일정 자동 등록 통합 UseCase.
 *
 * 단일 책임 원칙(SRP)에 따라 [AddProgressUseCase]는 수정하지 않고,
 * 이 UseCase에서 진도 저장 → 복습 일정 계산 → Firestore 등록 흐름을 조율한다.
 *
 * 진도 저장 성공 후 복습 등록에 실패하더라도 진도 저장은 유지한다.
 */
class SaveProgressAndScheduleUseCase @Inject constructor(
    private val addProgressUseCase: AddProgressUseCase,
    private val calculateReviewScheduleUseCase: CalculateReviewScheduleUseCase,
    private val reviewScheduleRepository: ReviewScheduleRepository,
    private val examRepository: ExamRepository
) {

    companion object {
        private const val TAG = "SaveProgressAndSchedule"
        private val KST = ZoneId.of("Asia/Seoul")
    }

    /**
     * 진도를 저장하고, 성공 시 복습 일정을 자동 등록한다.
     *
     * @param uid      인증된 사용자 UID
     * @param progress 저장할 Progress 객체 (examId 필드 필수)
     * @return 진도 저장 성공 시 [Result.success(Unit)], 진도 저장 실패 시 [Result.failure]
     *         복습 등록 실패는 Result에 영향 없이 로그만 기록됨
     */
    suspend operator fun invoke(uid: String, progress: Progress): Result<Unit> {
        // 1. 진도 저장
        val progressResult = addProgressUseCase(uid, progress)
        if (progressResult.isFailure) {
            return progressResult
        }

        // 2. 복습 일정 등록 (실패해도 진도 저장 성공 유지)
        try {
            // 시험 정보 조회 (Flow → 단일 값 추출)
            val exam = examRepository.getExamById(progress.examId).first()

            // epoch ms → LocalDate (KST 기준)
            val progressDate = Instant.ofEpochMilli(
                // AddProgressUseCase가 오늘 자정으로 세팅하지만, 방어적으로 재계산
                if (progress.createdAt > 0L) progress.createdAt
                else System.currentTimeMillis()
            ).atZone(KST).toLocalDate()

            val examDate = Instant.ofEpochMilli(exam.examDate)
                .atZone(KST)
                .toLocalDate()

            // 3. 복습 예정일 계산
            val reviewDates = calculateReviewScheduleUseCase.execute(progressDate, examDate)

            // 4. 각 날짜를 ReviewSchedule로 생성 후 Firestore 저장
            val now = System.currentTimeMillis()
            reviewDates.forEachIndexed { index, localDate ->
                val reviewDateMs = localDate
                    .atStartOfDay(KST)
                    .toInstant()
                    .toEpochMilli()

                val schedule = ReviewSchedule(
                    reviewScheduleId = UUID.randomUUID().toString(),
                    originProgressId = progress.progressId,
                    reviewDate       = reviewDateMs,
                    reviewRound      = index + 1,
                    isCompleted      = false,
                    createdAt        = now,
                    updatedAt        = now
                )

                val scheduleResult = reviewScheduleRepository.createReviewSchedule(uid, schedule)
                if (scheduleResult.isFailure) {
                    Log.w(TAG, "복습 일정 저장 실패 (round=${index + 1}): " +
                        "${scheduleResult.exceptionOrNull()?.message}")
                }
            }

        } catch (e: InvalidScheduleException) {
            // 시험일이 진도일 이전/같은 경우 → 복습 skip, 진도 저장은 성공
            Log.w(TAG, "복습 일정 skip: ${e.message}")
        } catch (e: Exception) {
            // 시험 조회 실패, 네트워크 오류 등 → 복습 skip, 진도 저장은 성공
            Log.e(TAG, "복습 일정 등록 중 오류 (진도 저장은 유지됨): ${e.message}", e)
        }

        return Result.success(Unit)
    }
}
