package com.loorve.presentation.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.loorve.domain.model.ReviewScheduleItem
import com.loorve.domain.repository.ExamRepository
import com.loorve.domain.repository.StudyRecordRepository
import com.loorve.domain.review.ReviewPlanStatus
import com.loorve.domain.usecase.CompleteReviewWithReschedulingUseCase
import com.loorve.domain.usecase.ReviewCompletionOutcome
import com.loorve.domain.usecase.ReviewScheduleQueriesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TodayReviewItemUi(
    val item: ReviewScheduleItem,
    val subjectName: String,
    val daysUntilExam: Long?
)

data class TodayReviewsUiState(
    val isLoading: Boolean = true,
    val items: List<TodayReviewItemUi> = emptyList(),
    val overloadedCount: Int = 0,
    val completingId: String? = null,
    val message: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class TodayReviewsViewModel @Inject constructor(
    private val queries: ReviewScheduleQueriesUseCase,
    private val completeReview: CompleteReviewWithReschedulingUseCase,
    private val studyRecordRepository: StudyRecordRepository,
    private val examRepository: ExamRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(TodayReviewsUiState())
    val uiState: StateFlow<TodayReviewsUiState> = _uiState.asStateFlow()
    private val zone = ZoneId.of("Asia/Seoul")

    init {
        refresh()
    }

    fun refresh() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrBlank()) {
            _uiState.update { it.copy(isLoading = false, errorMessage = "로그인 정보를 찾을 수 없습니다.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                val records = studyRecordRepository.getAllStudyRecords(uid).getOrThrow()
                val exams = examRepository.getExamList().first()
                val examById = exams.associateBy { it.id }
                val recordById = records.associateBy { it.id }
                val items = queries.today(uid).getOrThrow()
                    .filter { it.status != com.loorve.domain.model.ReviewStatus.COMPLETED }
                    .sortedWith(
                        compareByDescending<ReviewScheduleItem> { it.priorityScore }
                            .thenBy { it.reviewDate }
                            .thenBy { it.id }
                    )
                    .map { item ->
                        val exam = recordById[item.studyRecordId]?.let { examById[it.examId] }
                        TodayReviewItemUi(
                            item = item,
                            subjectName = exam?.subjectName.orEmpty(),
                            daysUntilExam = exam?.examDate?.let {
                                java.time.Instant.ofEpochMilli(it).atZone(zone).toLocalDate()
                            }?.let { java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(zone), it) }
                        )
                    }
                _uiState.value = TodayReviewsUiState(
                    isLoading = false,
                    items = items,
                    overloadedCount = items.count {
                        it.item.planStatus == ReviewPlanStatus.OVERLOADED_UNRESOLVED
                    }
                )
            }.onFailure { error ->
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "오늘의 복습을 불러오지 못했습니다.")
                }
            }
        }
    }

    fun complete(reviewId: String, outcome: ReviewCompletionOutcome) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        if (_uiState.value.completingId != null) return
        viewModelScope.launch {
            _uiState.update { it.copy(completingId = reviewId, errorMessage = null) }
            completeReview(uid, reviewId, outcome)
                .onSuccess { result ->
                    val dateText = result.nextReviewDate?.toString()
                    _uiState.update {
                        it.copy(
                            completingId = null,
                            message = if (dateText != null) {
                                "다음 일정이 조정되었습니다: $dateText"
                            } else {
                                "복습 결과가 저장되었습니다."
                            }
                        )
                    }
                    refresh()
                }
                .onFailure {
                    _uiState.update {
                        it.copy(completingId = null, errorMessage = "복습 결과를 저장하지 못했습니다.")
                    }
                }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null, errorMessage = null) }
    }
}
