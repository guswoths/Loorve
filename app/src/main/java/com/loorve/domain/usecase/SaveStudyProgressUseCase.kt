package com.loorve.domain.usecase

import com.loorve.domain.model.StudyRecord
import com.loorve.domain.repository.ReviewScheduleItemRepository
import com.loorve.domain.repository.StudyRecordRepository
import com.loorve.domain.review.ReviewScheduler
import com.loorve.domain.review.toLocalDate
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject

data class SaveStudyProgressRequest(
    val uid: String,
    val blockId: String,
    val examId: String,
    val content: String,
    val learningDateMillis: Long,
    val examDateMillis: Long,
    val prepStartDateMillis: Long? = null,
    val dailyCap: Int = 5
)

class SaveStudyProgressUseCase @Inject constructor(
    private val studyRecordRepository: StudyRecordRepository,
    private val scheduleRepository: ReviewScheduleItemRepository
) {
    suspend operator fun invoke(request: SaveStudyProgressRequest): Result<String> =
        runCatching {
            val learningDate = request.learningDateMillis.toLocalDate()
            val examDate = request.examDateMillis.toLocalDate()
            val prepStart = request.prepStartDateMillis?.toLocalDate() ?: learningDate

            require(examDate > learningDate) {
                "시험일은 학습일보다 미래여야 합니다."
            }

            val studyRecordId = UUID.randomUUID().toString()
            val titlePreview = request.content.take(20)
            val scheduleResult = ReviewScheduler.generateSchedule(
                learningDate = learningDate,
                examDate = examDate,
                studyRecordId = studyRecordId,
                title = titlePreview,
                blockId = request.blockId,
                uid = request.uid,
                prepStartDate = prepStart
            )

            val record = StudyRecord(
                id = studyRecordId,
                uid = request.uid,
                blockId = request.blockId,
                examId = request.examId,
                title = titlePreview,
                content = request.content,
                learningDate = request.learningDateMillis,
                examDate = request.examDateMillis,
                prepStartDate = prepStart.atStartOfDay(ZoneId.of("Asia/Seoul"))
                    .toInstant().toEpochMilli(),
                recommendedCompletionDate =
                    scheduleResult.recommendedCompletionDate.atStartOfDay(
                        ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli(),
                plannedReviewCount = scheduleResult.items.size,
                createdAt = System.currentTimeMillis()
            )

            studyRecordRepository.saveStudyRecord(record).getOrThrow()
            scheduleRepository.saveSchedules(
                request.uid, studyRecordId, scheduleResult.items
            ).getOrThrow()

            studyRecordId
        }
}