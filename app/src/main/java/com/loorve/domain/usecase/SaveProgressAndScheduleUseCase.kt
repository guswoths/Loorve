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
 * studyEndDate가 설정된 경우 해당 날짜를 초과하는 복습 회차는 자동 제외됨.
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

    suspend operator fun invoke(uid: String, progress: Progress): Result<Unit> {
        // 1. 진도 저장
        val progressResult = addProgressUseCase(uid, progress)
        if (progressResult.isFailure) return progressResult

        // 2. 복습 일정 등록 (실패해도 진도 저장 성공 유지)
        try {
            val exam = examRepository.getExamById(progress.examId).first()

            val progressDate = Instant.ofEpochMilli(
                if (progress.createdAt > 0L) progress.createdAt else System.currentTimeMillis()
            ).atZone(KST).toLocalDate()

            val examDate = Instant.ofEpochMilli(exam.examDate)
                .atZone(KST)
                .toLocalDate()

            // studyEndDate: 0L이면 null 처리 (기존 동작 유지)
            val studyEndDate: LocalDate? = if (exam.studyEndDate > 0L) {
                Instant.ofEpochMilli(exam.studyEndDate)
                    .atZone(KST)
                    .toLocalDate()
            } else {
                null
            }

            // 3. 복습 예정일 계산 (studyEndDate 전달)
            val reviewDates = calculateReviewScheduleUseCase.execute(
                progressDate = progressDate,
                examDate     = examDate,
                studyEndDate = studyEndDate   // 추가
            )

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
            Log.w(TAG, "복습 일정 skip: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "복습 일정 등록 중 오류 (진도 저장은 유지됨): ${e.message}", e)
        }

        return Result.success(Unit)
    }
}