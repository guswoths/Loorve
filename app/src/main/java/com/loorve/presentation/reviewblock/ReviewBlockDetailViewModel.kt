package com.loorve.presentation.reviewblock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loorve.domain.model.CompletionResult
import com.loorve.domain.model.ReviewBlock
import com.loorve.domain.model.ReviewScheduleItem
import com.loorve.domain.model.StudyRecord
import com.loorve.domain.repository.ReviewBlockRepository
import com.loorve.domain.repository.ReviewScheduleItemRepository
import com.loorve.domain.repository.StudyRecordRepository
import com.loorve.domain.review.ReviewScheduler
import com.loorve.domain.review.toLocalDate
import com.loorve.domain.usecase.SaveStudyProgressRequest
import com.loorve.domain.usecase.SaveStudyProgressUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class ReviewBlockDetailUiState(
    val isLoading: Boolean = false,
    val studyRecords: List<StudyRecord> = emptyList(),
    val scheduleItems: List<ReviewScheduleItem> = emptyList(),
    val overdueItems: List<ReviewScheduleItem> = emptyList(),
    val recommendedCompletionDate: Long? = null,
    val reviewOverloadWarning: Boolean = false,
    val errorMessage: String? = null,
    val savedSuccess: Boolean = false,
    val reviewBlock: ReviewBlock? = null
)

@HiltViewModel
class ReviewBlockDetailViewModel @Inject constructor(
    private val saveStudyProgressUseCase: SaveStudyProgressUseCase,
    private val studyRecordRepository: StudyRecordRepository,
    private val scheduleRepository: ReviewScheduleItemRepository,
    private val reviewBlockRepository: ReviewBlockRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReviewBlockDetailUiState())
    val uiState: StateFlow<ReviewBlockDetailUiState> = _uiState.asStateFlow()

    fun loadBlockData(uid: String, blockId: String, externalBlock: ReviewBlock? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val resolvedBlock = externalBlock
                ?: reviewBlockRepository.getReviewBlocks(uid)
                    .getOrDefault(emptyList())
                    .firstOrNull { it.blockId == blockId }

            // ✅ 최신순(learningDate 내림차순) 정렬 유지
            val records = studyRecordRepository.getStudyRecords(uid, blockId)
                .getOrDefault(emptyList())
                .sortedByDescending { it.learningDate }

            val allSchedules = records.flatMap { record ->
                scheduleRepository.getSchedulesByStudyRecord(uid, record.id)
                    .getOrDefault(emptyList())
            }

            val today = LocalDate.now()
            val overdueResult = ReviewScheduler.handleOverdue(today, allSchedules)
            val updatedSchedules = overdueResult.updatedItems

            overdueResult.overdueQueue.forEach { item ->
                scheduleRepository.updateScheduleItem(uid, item)
            }

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                reviewBlock = resolvedBlock,
                studyRecords = records,
                scheduleItems = updatedSchedules,
                overdueItems = overdueResult.overdueQueue
            )
        }
    }

    fun saveProgress(
        uid: String,
        blockId: String,
        examId: String,
        title: String,
        content: String,
        completionRate: Float = 1.0f,   // ✅ 추가: UI 슬라이더 값 수신
        learningDateMillis: Long,
        examDateMillis: Long,
        prepStartDateMillis: Long,
        dailyCap: Int = 5
    ) {
        if (_uiState.value.isLoading) return

        if (examDateMillis == 0L) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "블록 정보가 아직 로드되지 않았습니다. 잠시 후 다시 시도해주세요."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            saveStudyProgressUseCase(
                SaveStudyProgressRequest(
                    uid = uid,
                    blockId = blockId,
                    examId = examId,
                    title = title,
                    content = content,
                    completionRate = completionRate.coerceIn(0f, 1f),  // ✅ clamp 보장
                    learningDateMillis = learningDateMillis,
                    examDateMillis = examDateMillis,
                    prepStartDateMillis = prepStartDateMillis,
                    dailyCap = dailyCap
                )
            ).onSuccess {
                loadBlockData(uid, blockId)
                _uiState.value = _uiState.value.copy(savedSuccess = true)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "저장에 실패했습니다."
                )
            }
        }
    }

    fun completeReview(
        uid: String,
        item: ReviewScheduleItem,
        result: CompletionResult,
        examDateMillis: Long
    ) {
        viewModelScope.launch {
            val today = LocalDate.now()
            val examDate = examDateMillis.toLocalDate()

            val completeResult = ReviewScheduler.completeReview(
                item = item, result = result,
                today = today, examDate = examDate
            )

            scheduleRepository.updateScheduleItem(uid, completeResult.updatedItem)

            completeResult.nextReviewDate?.let { nextDate ->
                val nextItem = item.copy(
                    id = "${item.studyRecordId}_r${item.reviewOrder + 1}",
                    reviewDate = nextDate.atStartOfDay(
                        java.time.ZoneId.of("Asia/Seoul")
                    ).toInstant().toEpochMilli(),
                    originalReviewDate = nextDate.atStartOfDay(
                        java.time.ZoneId.of("Asia/Seoul")
                    ).toInstant().toEpochMilli(),
                    reviewOrder = item.reviewOrder + 1,
                    status = com.loorve.domain.model.ReviewStatus.PENDING,
                    previousGapDays = completeResult.nextGapDays,
                    overdueDays = 0L,
                    completionResult = null,
                    completedAt = null
                )
                scheduleRepository.saveSchedules(uid, item.studyRecordId, listOf(nextItem))
            }

            loadBlockData(uid, item.blockId)
        }
    }

    fun resetSavedSuccess() {
        _uiState.value = _uiState.value.copy(savedSuccess = false)
    }
}