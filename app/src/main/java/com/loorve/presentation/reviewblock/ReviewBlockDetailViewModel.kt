package com.loorve.presentation.reviewblock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.loorve.domain.model.CompletionResult
import com.loorve.domain.model.ReviewBlock
import com.loorve.domain.model.ReviewScheduleItem
import com.loorve.domain.model.StudyRecord
import com.loorve.domain.repository.ReviewBlockRepository
import com.loorve.domain.repository.ReviewScheduleItemRepository
import com.loorve.domain.repository.StudyRecordRepository
import com.loorve.domain.review.ReviewScheduler
import com.loorve.domain.review.alarmTriggerAtMillis
import com.loorve.domain.review.toLocalDate
import com.loorve.data.local.NotificationTimePreferences
import com.loorve.data.notification.ReviewAlarmScheduler
import com.loorve.domain.usecase.CreateStudyRecordRequest
import com.loorve.domain.usecase.CreateStudyRecordWithReviewSchedulesUseCase
import com.loorve.domain.usecase.CompleteReviewWithReschedulingUseCase
import com.loorve.domain.usecase.ReviewCompletionOutcome
import com.loorve.domain.usecase.CreateStudyRecordResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject
import com.loorve.util.CalendarRefreshBus
import com.loorve.domain.subscription.ReviewBlockAccessPolicy
import com.loorve.domain.subscription.SubscriptionRepository

enum class ReviewBlockTab {
    STUDY_RECORD,
    REVIEW_RECORD
}

data class ReviewBlockDetailUiState(
    val isLoading: Boolean = false,
    val studyRecords: List<StudyRecord> = emptyList(),
    val reviewScheduleRecords: List<ReviewScheduleItem> = emptyList(),
    val scheduleItems: List<ReviewScheduleItem> = emptyList(),
    val overdueItems: List<ReviewScheduleItem> = emptyList(),
    val recommendedCompletionDate: Long? = null,
    val reviewOverloadWarning: Boolean = false,
    val errorMessage: String? = null,
    val savedSuccess: Boolean = false,
    val reviewBlock: ReviewBlock? = null,
    val deleteSuccess: Boolean = false,
    val showDeleteConfirm: Boolean = false,
    val recordToDelete: StudyRecord? = null,
    val selectedTab: ReviewBlockTab = ReviewBlockTab.STUDY_RECORD,
    val defaultAlarmTime: Pair<Int, Int> = 9 to 0
    ,val lastCreationResult: CreateStudyRecordResult? = null,
    val requiresPro: Boolean = false
)

@HiltViewModel
class ReviewBlockDetailViewModel @Inject constructor(
    private val createStudyRecordUseCase: CreateStudyRecordWithReviewSchedulesUseCase,
    private val completeReviewUseCase: CompleteReviewWithReschedulingUseCase,
    private val studyRecordRepository: StudyRecordRepository,
    private val scheduleRepository: ReviewScheduleItemRepository,
    private val reviewBlockRepository: ReviewBlockRepository,
    private val calendarRefreshBus: CalendarRefreshBus,
    private val notificationTimePreferences: NotificationTimePreferences,
    private val reviewAlarmScheduler: ReviewAlarmScheduler,
    private val subscriptionRepository: SubscriptionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReviewBlockDetailUiState())
    val uiState: StateFlow<ReviewBlockDetailUiState> = _uiState.asStateFlow()

    fun saveCustomAlarmTime(
        uid: String,
        item: ReviewScheduleItem,
        hour: Int,
        minute: Int
    ) {
        if (hour !in 0..23 || minute !in 0..59) {
            _uiState.value = _uiState.value.copy(errorMessage = "알림 시간이 올바르지 않습니다.")
            return
        }

        viewModelScope.launch {
            val updatedItem = item.copy(customAlarmTime = hour to minute)
            scheduleRepository.updateScheduleItem(uid, updatedItem)
                .onSuccess {
                    val triggerAtMillis = updatedItem.alarmTriggerAtMillis(
                        _uiState.value.defaultAlarmTime
                    )
                    if (updatedItem.status == com.loorve.domain.model.ReviewStatus.COMPLETED ||
                        triggerAtMillis <= System.currentTimeMillis()
                    ) {
                        reviewAlarmScheduler.cancelReviewAlarm(updatedItem.id)
                    } else {
                        reviewAlarmScheduler.cancelReviewAlarm(updatedItem.id)
                        reviewAlarmScheduler.scheduleReviewAlarm(updatedItem.id, triggerAtMillis)
                    }
                    _uiState.value = _uiState.value.copy(
                        reviewScheduleRecords = _uiState.value.reviewScheduleRecords.map {
                            if (it.id == updatedItem.id) updatedItem else it
                        }
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = error.message ?: "알림 시간 저장에 실패했습니다."
                    )
                }
        }
    }

    init {
        viewModelScope.launch {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
            notificationTimePreferences.notificationTime(uid).collect { time ->
                _uiState.value = _uiState.value.copy(defaultAlarmTime = time)
            }
        }
    }

    private val _selectedTab = MutableStateFlow(ReviewBlockTab.STUDY_RECORD)
    val selectedTab: StateFlow<ReviewBlockTab> = _selectedTab.asStateFlow()

    fun selectTab(tab: ReviewBlockTab, uid: String = "", blockId: String = "") {
        _selectedTab.value = tab
        _uiState.value = _uiState.value.copy(selectedTab = tab)
        if (tab == ReviewBlockTab.REVIEW_RECORD && uid.isNotBlank() && blockId.isNotBlank()) {
            loadReviewRecords(uid, blockId)
        }
    }

    fun loadReviewRecords(uid: String, blockId: String) {
        viewModelScope.launch {
            val records = studyRecordRepository.getStudyRecords(uid, blockId)
                .getOrDefault(emptyList())
            val allSchedules = records.flatMap { record ->
                scheduleRepository.getSchedulesByStudyRecord(uid, record.id)
                    .getOrDefault(emptyList())
            }.sortedBy { it.reviewDate }
            _uiState.value = _uiState.value.copy(reviewScheduleRecords = allSchedules)
        }
    }

    fun loadBlockData(uid: String, blockId: String, externalBlock: ReviewBlock? = null) {
        // externalBlock이 있으면 isLoading = true와 동시에 reviewBlock도 함께 세팅
        // → UI가 로딩 중에도 examDate를 올바르게 읽을 수 있음
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            reviewBlock = externalBlock ?: _uiState.value.reviewBlock,
            errorMessage = null  // 이전 에러 초기화
        )

        viewModelScope.launch {
            val resolvedBlock = externalBlock
                ?: reviewBlockRepository.getReviewBlocks(uid)
                    .getOrDefault(emptyList())
                    .firstOrNull { it.blockId == blockId }

            if (resolvedBlock == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "블록 정보를 불러올 수 없습니다."
                )
                return@launch
            }

            val allBlocks = reviewBlockRepository.getReviewBlocks(uid).getOrDefault(emptyList())
            val entitlement = subscriptionRepository.state
                .first { it.entitlement !is com.loorve.domain.subscription.SubscriptionEntitlement.Loading }
                .entitlement
            val accessibleIds = ReviewBlockAccessPolicy.accessibleBlockIds(
                allBlocks,
                entitlement
            )
            if (resolvedBlock.blockId !in accessibleIds) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    reviewBlock = null,
                    requiresPro = true
                )
                return@launch
            }

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
                reviewScheduleRecords = updatedSchedules,
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
        learningDateMillis: Long,
        dailyCap: Int = 5,
        difficulty: com.loorve.domain.review.ReviewDifficulty =
            com.loorve.domain.review.ReviewDifficulty.MEDIUM,
        importance: com.loorve.domain.review.ReviewImportance =
            com.loorve.domain.review.ReviewImportance.NORMAL,
        initialMastery: Int? = null,
        estimatedReviewMinutes: Int = 15
    ) {
        if (_uiState.value.isLoading) return

        val currentBlock = _uiState.value.reviewBlock
        if (currentBlock == null) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "블록 정보를 아직 불러오지 못했습니다. 잠시 후 다시 시도해주세요."
            )
            return
        }

        val examDateMillis = currentBlock.examDate
        val prepStartDateMillis = currentBlock.prepStartDate

        if (examDateMillis == 0L) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "시험 날짜가 설정되지 않은 블록입니다. 블록을 수정해주세요."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            createStudyRecordUseCase(
                uid = uid,
                request = CreateStudyRecordRequest(
                    examId = examId,
                    title = title,
                    content = content,
                    studiedAt = learningDateMillis.toLocalDate(),
                    blockId = blockId,
                    difficulty = difficulty,
                    importance = importance,
                    initialMastery = initialMastery,
                    estimatedReviewMinutes = estimatedReviewMinutes
                )
            ).onSuccess {
                val result = it
                loadBlockData(uid, blockId)
                _uiState.value = _uiState.value.copy(
                    savedSuccess = true,
                    lastCreationResult = result
                )
                calendarRefreshBus.notifyRefresh()
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
            completeReviewUseCase(
                uid = uid,
                reviewId = item.id,
                outcome = if (result == CompletionResult.REMEMBERED) {
                    ReviewCompletionOutcome.SUCCESS
                } else {
                    ReviewCompletionOutcome.FAILED
                }
            ).onSuccess {
                loadBlockData(uid, item.blockId)
                calendarRefreshBus.notifyRefresh()
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = error.message ?: "복습 결과 저장에 실패했습니다."
                )
            }
        }
    }

    fun resetSavedSuccess() {
        _uiState.value = _uiState.value.copy(savedSuccess = false)
    }

    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun setShowDeleteConfirm(show: Boolean) {
        _uiState.value = _uiState.value.copy(showDeleteConfirm = show)
    }

    fun resetDeleteSuccess() {
        _uiState.value = _uiState.value.copy(deleteSuccess = false)
    }

    fun deleteBlock(uid: String, blockId: String) {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            reviewBlockRepository.deleteReviewBlock(uid, blockId)
                .onSuccess {
                    calendarRefreshBus.notifyRefresh()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        deleteSuccess = true,
                        showDeleteConfirm = false
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "블록 삭제에 실패했습니다.",
                        showDeleteConfirm = false
                    )
                }
        }
    }

    fun setRecordToDelete(record: StudyRecord?) {
        _uiState.value = _uiState.value.copy(recordToDelete = record)
    }

    fun deleteStudyRecord(uid: String, blockId: String, record: StudyRecord) {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            studyRecordRepository.deleteStudyRecord(uid, record)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(recordToDelete = null)
                    loadBlockData(uid, blockId)
                    calendarRefreshBus.notifyRefresh()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "학습기록 삭제에 실패했습니다.",
                        recordToDelete = null
                    )
                }
        }
    }
}