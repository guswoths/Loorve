package com.loorve.domain.usecase

import com.loorve.domain.model.Exam
import com.loorve.domain.repository.ExamRepository
import com.loorve.domain.repository.ReviewScheduleItemRepository
import com.loorve.domain.repository.StudyRecordRepository
import com.loorve.domain.review.ReviewSchedulingEngine
import com.loorve.domain.review.SchedulingMessages
import com.loorve.domain.review.ValidationResult
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.first

data class SaveExamRequest(
    val examName: String,
    val examDate: LocalDate,
    val timezone: String = "Asia/Seoul",
    val finalReviewBufferDays: Int? = 1,
    val maxDailyReviewMinutes: Int? = null,
    val examId: String? = null
)

data class SaveExamResult(
    val canSaveExamDate: Boolean,
    val primaryMessage: String,
    val helperMessages: List<String>,
    val recommendedEarliestExamDate: LocalDate?,
    val warnings: List<String>,
    val cramRequiredCount: Int = 0
)

class SaveExamWithSchedulingUseCase @Inject constructor(
    private val examRepository: ExamRepository,
    private val studyRecordRepository: StudyRecordRepository,
    private val scheduleRepository: ReviewScheduleItemRepository
) {
    suspend operator fun invoke(
        uid: String,
        request: SaveExamRequest,
        today: LocalDate = LocalDate.now(ZoneId.of("Asia/Seoul"))
    ): Result<SaveExamResult> = runCatching {
        require(uid.isNotBlank()) { "로그인이 필요합니다." }
        require(request.examName.isNotBlank()) { "시험명은 비어 있을 수 없습니다." }
        val buffer = request.finalReviewBufferDays ?: 1
        require(buffer >= 0) { "시험 전 버퍼 일수는 0 이상이어야 합니다." }
        require(request.maxDailyReviewMinutes == null || request.maxDailyReviewMinutes > 0) {
            "하루 최대 복습 시간은 양수여야 합니다."
        }
        val validation = ReviewSchedulingEngine.validateExamDate(today, request.examDate, buffer)
        if (validation is ValidationResult.Blocked) {
            return@runCatching SaveExamResult(
                canSaveExamDate = false,
                primaryMessage = validation.message,
                helperMessages = listOf(
                    "최소 3개의 정규 학습·복습 가능 날짜와 시험 전 버퍼가 필요합니다."
                ),
                recommendedEarliestExamDate = validation.recommendedEarliestExamDate,
                warnings = emptyList()
            )
        }
        val current = request.examId?.let { examRepository.getExamById(it).first() }
        if (current != null && current.createdBy.isNotBlank() && current.createdBy != uid) {
            throw SecurityException("본인의 시험만 수정할 수 있습니다.")
        }
        val saveResult = examRepository.addExam(
            Exam(
                id = request.examId.orEmpty(),
                subjectName = request.examName.trim(),
                examDate = request.examDate.atStartOfDay(ZoneId.of(request.timezone))
                    .toInstant().toEpochMilli(),
                createdBy = uid,
                timezone = request.timezone,
                finalReviewBufferDays = buffer,
                maxDailyReviewMinutes = request.maxDailyReviewMinutes
            )
        )
        saveResult.getOrThrow()
        val cramCount = if (request.examId.isNullOrBlank()) {
            0
        } else {
            val recordIds = studyRecordRepository.getAllStudyRecords(uid).getOrDefault(emptyList())
                .filter { it.examId == request.examId }
                .map { it.id }
                .toSet()
            scheduleRepository.getAllScheduleItems(uid).getOrDefault(emptyList())
                .filter {
                    it.studyRecordId in recordIds &&
                        it.planStatus == com.loorve.domain.review.ReviewPlanStatus.CRAM_MODE_REQUIRED
                }
                .map { it.studyRecordId }
                .distinct()
                .size
        }
        SaveExamResult(
            canSaveExamDate = true,
            primaryMessage = "시험일을 저장했습니다.",
            helperMessages = emptyList(),
            recommendedEarliestExamDate = null,
            warnings = if (cramCount > 0) listOf(
                "등록된 학습기록 중 ${cramCount}개 항목은 시험일까지 정규 분산복습 기준을 충족하지 못합니다. 해당 항목은 압축 복습으로 표시됩니다.",
                "시험일을 늦추면 더 안정적인 복습 일정을 만들 수 있습니다."
            ) else emptyList(),
            cramRequiredCount = cramCount
        )
    }
}
