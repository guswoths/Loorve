package com.loorve.domain.usecase

import android.util.Log
import com.loorve.data.notification.ReviewAlarmScheduler
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

class SaveProgressAndScheduleUseCase @Inject constructor(
    private val addProgressUseCase: AddProgressUseCase,
    private val calculateReviewScheduleUseCase: CalculateReviewScheduleUseCase,
    private val reviewScheduleRepository: ReviewScheduleRepository,
    private val examRepository: ExamRepository,
    private val alarmScheduler: ReviewAlarmScheduler
) {
    companion object {
        private const val TAG = "SaveProgressAndSchedule"
        private val KST = ZoneId.of("Asia/Seoul")
    }

    suspend operator fun invoke(uid: String, progress: Progress): Result<Unit> {
        val progressResult = addProgressUseCase(uid, progress)
        if (progressResult.isFailure) return progressResult

        try {
            val exam = examRepository.getExamById(progress.examId).first()

            val progressDate = Instant.ofEpochMilli(
                if (progress.createdAt > 0L) progress.createdAt else System.currentTimeMillis()
            ).atZone(KST).toLocalDate()

            val examDate = Instant.ofEpochMilli(exam.examDate)
                .atZone(KST)
                .toLocalDate()

            val studyEndDate: LocalDate? = if (exam.studyEndDate > 0L) {
                Instant.ofEpochMilli(exam.studyEndDate).atZone(KST).toLocalDate()
            } else null

            val reviewDates = calculateReviewScheduleUseCase.execute(
                progressDate = progressDate,
                examDate     = examDate,
                studyEndDate = studyEndDate
            )

            val now = System.currentTimeMillis()
            reviewDates.forEachIndexed { index, localDate ->
                val reviewDateMs = localDate
                    .atStartOfDay(KST)
                    .toInstant()
                    .toEpochMilli()

                // ✅ 수정: 실제 ReviewSchedule 필드명으로 통일
                val schedule = ReviewSchedule(
                    scheduleId       = UUID.randomUUID().toString(),
                    userId           = uid,
                    originProgressId = progress.progressId,
                    reviewDate       = reviewDateMs,
                    reviewOrder      = index + 1,
                    isCompleted      = false,
                    createdAt        = now,
                    updatedAt        = now
                )

                // ✅ 수정: 인터페이스에 있는 메서드명으로 변경
                val scheduleResult = reviewScheduleRepository.saveReviewSchedule(uid, schedule)
                if (scheduleResult.isFailure) {
                    Log.w(TAG, "복습 일정 저장 실패 (round=${index + 1}): " +
                            "${scheduleResult.exceptionOrNull()?.message}")
                    return@forEachIndexed
                }

                val alarmResult = alarmScheduler.scheduleReviewAlarm(
                    reviewScheduleId = schedule.scheduleId,
                    triggerAtMillis  = reviewDateMs
                )
                when (alarmResult) {
                    ReviewAlarmScheduler.ScheduleResult.FAILED ->
                        Log.w(TAG, "알람 예약 실패 (FAILED): id=${schedule.scheduleId}")
                    ReviewAlarmScheduler.ScheduleResult.FALLBACK_INEXACT ->
                        Log.w(TAG, "알람 비정확 폴백 (FALLBACK_INEXACT): id=${schedule.scheduleId}")
                    ReviewAlarmScheduler.ScheduleResult.EXACT ->
                        Unit
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