package com.loorve.domain.usecase

import android.util.Log
import com.loorve.data.notification.ReviewAlarmScheduler
import com.loorve.domain.model.Progress
import com.loorve.domain.model.ReviewSchedule
import com.loorve.domain.repository.ExamRepository
import com.loorve.domain.repository.ReviewScheduleRepository
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
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
        // 1단계: 진도 저장
        val progressResult = addProgressUseCase(uid, progress)
        if (progressResult.isFailure) return progressResult

        // 2단계: Exam 조회 — ✅ firstOrNull() + catch로 예외 안전하게 처리
        val exam = try {
            examRepository.getExamById(progress.examId)
                .catch { e ->
                    Log.w(TAG, "Exam 조회 오류 (복습 일정 생성 스킵): ${e.message}")
                    // emit 없이 종료 → firstOrNull()이 null 반환
                }
                .firstOrNull()
        } catch (e: Exception) {
            Log.w(TAG, "Exam 조회 실패 (복습 일정 생성 스킵): ${e.message}")
            null
        }

        // ✅ exam 없으면 조용히 성공 반환 (진도는 이미 저장됨)
        if (exam == null) {
            Log.w(TAG, "examId=${progress.examId} 에 해당하는 Exam 없음 — 복습 일정 생성 스킵")
            return Result.success(Unit)
        }

        // 3단계: 복습 일정 계산 및 저장
        return try {
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

            // ✅ reviewDates가 비어 있으면 InvalidScheduleException 없이 진도만 성공
            if (reviewDates.isEmpty()) {
                Log.d(TAG, "계산된 복습 일정 없음 (시험일이 너무 가까움) — 진도만 저장")
                return Result.success(Unit)
            }

            val now = System.currentTimeMillis()
            reviewDates.forEachIndexed { index, localDate ->
                val reviewDateMs = localDate
                    .atStartOfDay(KST)
                    .toInstant()
                    .toEpochMilli()

                val schedule = ReviewSchedule(
                    scheduleId       = UUID.randomUUID().toString(),
                    userId           = uid,
                    blockId          = "",
                    originProgressId = progress.progressId,
                    title            = progress.content.trim(),   // ✅ title을 content로 채워서 저장
                    reviewDate       = reviewDateMs,
                    reviewDateText   = localDate.toString(),      // ✅ reviewDateText 채움
                    reviewOrder      = index + 1,
                    scheduleType     = "EBBINGHAUS",
                    isCompleted      = false,
                    createdAt        = now,
                    updatedAt        = now
                )

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
                        Log.w(TAG, "알람 예약 실패: id=${schedule.scheduleId}")
                    ReviewAlarmScheduler.ScheduleResult.FALLBACK_INEXACT ->
                        Log.w(TAG, "알람 비정확 폴백: id=${schedule.scheduleId}")
                    ReviewAlarmScheduler.ScheduleResult.EXACT -> Unit
                }
            }

            Result.success(Unit)

        } catch (e: InvalidScheduleException) {
            Log.w(TAG, "복습 일정 skip (진도 저장은 유지됨): ${e.message}")
            Result.success(Unit)   // ✅ 진도는 이미 저장됐으므로 성공 반환
        } catch (e: Exception) {
            Log.e(TAG, "복습 일정 등록 중 오류 (진도 저장은 유지됨): ${e.message}", e)
            Result.success(Unit)   // ✅ 복습 일정 실패가 진도 저장 성공을 가리지 않도록
        }
    }
}