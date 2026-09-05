// 경로: app/src/main/java/com/loorve/presentation/calendar/ReviewCalendarViewModel.kt
package com.loorve.presentation.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.loorve.domain.model.ReviewSchedule
import com.loorve.domain.repository.ReviewScheduleRepository
import com.loorve.domain.usecase.UpdateReviewCompletionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import com.loorve.domain.model.ReviewBlock
import com.loorve.domain.repository.ReviewBlockRepository
import com.loorve.util.CalendarRefreshBus

data class ReviewCalendarUiState(
    val displayYearMonth: YearMonth = YearMonth.now(),
    val schedulesMap: Map<LocalDate, List<ReviewSchedule>> = emptyMap(),
    val selectedDate: LocalDate? = null,
    val selectedDateSchedules: List<ReviewSchedule> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val reviewBlocks: List<ReviewBlock> = emptyList(),
    val isBlocksLoading: Boolean = false,
    val selectedBlock: ReviewBlock? = null,   // 클릭된 블록 (바텀시트용)
    val showBlockDetail: Boolean = false,      // 바텀시트 표시 여부
    val isDeleting: Boolean = false            // 삭제 진행 중 여부
)

@HiltViewModel
class ReviewCalendarViewModel @Inject constructor(
    private val reviewScheduleRepository: ReviewScheduleRepository,
    private val updateReviewCompletionUseCase: UpdateReviewCompletionUseCase,
    private val reviewBlockRepository: ReviewBlockRepository,
    private val calendarRefreshBus: CalendarRefreshBus
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReviewCalendarUiState())
    val uiState: StateFlow<ReviewCalendarUiState> = _uiState.asStateFlow()

    private val _currentUid = MutableStateFlow<String?>(null)
    val currentUid: StateFlow<String?> = _currentUid.asStateFlow()

    private val _isUidReady = MutableStateFlow(false)
    val isUidReady: StateFlow<Boolean> = _isUidReady.asStateFlow()

    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private var loadJob: Job? = null

    // ✅ init 블록 제거 — Screen의 LaunchedEffect에서 suspend refreshUid() 호출로 통일

    /**
     * suspend fun으로 변경하여 호출부(LaunchedEffect)에서 완료를 기다릴 수 있도록 함.
     * 토큰 갱신 실패 시 캐시 uid 폴백으로 네트워크 오류와 로그인 오류를 구분.
     */
    // ✅ AFTER — refreshUid() 내부에서 직접 스케줄 로드까지 완료
    suspend fun refreshUid() {
        _isUidReady.value = false
        val user = FirebaseAuth.getInstance().currentUser
        val uid = runCatching {
            user?.getIdToken(true)?.await()
            user?.uid
        }.getOrElse {
            user?.uid
        }
        _currentUid.value = uid
        _isUidReady.value = (uid != null)
        if (!uid.isNullOrBlank()) {
            loadReviewBlocks(uid)              // 기존 유지
            // ✅ 핵심 추가: uid 세팅 직후 스케줄 로드
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            loadSchedulesForMonth(_uiState.value.displayYearMonth)
        }
    }

    fun onMonthChanged(yearMonth: YearMonth) {
        _uiState.update { it.copy(displayYearMonth = yearMonth, isLoading = true, errorMessage = null) }
        loadSchedulesForMonth(yearMonth)
    }

    fun onDateSelected(date: LocalDate) {
        val schedules = _uiState.value.schedulesMap[date] ?: emptyList()
        _uiState.update {
            it.copy(
                selectedDate = date,
                selectedDateSchedules = schedules
            )
        }
    }

    fun onCompleteSchedule(scheduleId: String) {
        val uid = _currentUid.value ?: return
        viewModelScope.launch {
            val result = reviewScheduleRepository.completeReviewSchedule(uid, scheduleId)
            if (result.isFailure) {
                _uiState.update {
                    it.copy(errorMessage = result.exceptionOrNull()?.message ?: "복습 완료 처리에 실패했습니다.")
                }
            }
        }
    }

    fun toggleReviewCompletion(scheduleId: String, currentState: Boolean) {
        val uid = _currentUid.value ?: return
        viewModelScope.launch {
            val result = updateReviewCompletionUseCase(
                uid = uid,
                scheduleId = scheduleId,
                isCompleted = !currentState
            )
            if (result.isFailure) {
                _uiState.update {
                    it.copy(errorMessage = result.exceptionOrNull()?.message ?: "복습 상태 변경에 실패했습니다.")
                }
            }
        }
    }

    fun onDismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun loadCurrentMonth() {
        loadSchedulesForMonth(_uiState.value.displayYearMonth)
    }

    fun reloadCurrentMonth() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        loadSchedulesForMonth(_uiState.value.displayYearMonth)
    }

    fun loadReviewBlocks(uid: String) {
        if (uid.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isBlocksLoading = true) }
            reviewBlockRepository.getReviewBlocks(uid)
                .onSuccess { blocks ->
                    _uiState.update { it.copy(reviewBlocks = blocks, isBlocksLoading = false) }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isBlocksLoading = false,
                            errorMessage = e.message ?: "복습 블록을 불러오지 못했습니다."
                        )
                    }
                }
        }
    }

    fun onBlockClicked(block: ReviewBlock) {
        _uiState.update { it.copy(selectedBlock = block, showBlockDetail = true) }
    }

    fun onDismissBlockDetail() {
        _uiState.update { it.copy(selectedBlock = null, showBlockDetail = false) }
    }

    fun deleteReviewBlock(blockId: String) {
        val uid = _currentUid.value ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true) }
            reviewBlockRepository.deleteReviewBlock(uid, blockId)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isDeleting = false,
                            showBlockDetail = false,
                            selectedBlock = null
                        )
                    }
                    loadReviewBlocks(uid)
                    calendarRefreshBus.notifyRefresh()
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isDeleting = false,
                            errorMessage = e.message ?: "삭제에 실패했습니다."
                        )
                    }
                }
        }
    }

    private fun loadSchedulesForMonth(yearMonth: YearMonth) {
        val uid = _currentUid.value
        if (uid.isNullOrBlank()) {
            _uiState.update { it.copy(isLoading = false, errorMessage = null) }
            return
        }

        val startDate = yearMonth.atDay(1).format(dateFormatter)
        val endDate = yearMonth.atEndOfMonth().format(dateFormatter)

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            reviewScheduleRepository
                .getReviewSchedulesByDateRange(uid, startDate, endDate)
                .catch { exception ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = exception.message ?: "일정을 불러오지 못했습니다.")
                    }
                }
                .collectLatest { schedules ->
                    val schedulesMap = schedules.groupBy { schedule ->
                        java.time.Instant.ofEpochMilli(schedule.reviewDate)
                            .atZone(java.time.ZoneId.of("Asia/Seoul"))
                            .toLocalDate()
                    }
                    val selectedDate = _uiState.value.selectedDate
                    val selectedDateSchedules = if (selectedDate != null) schedulesMap[selectedDate] ?: emptyList() else emptyList()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            schedulesMap = schedulesMap,
                            selectedDateSchedules = selectedDateSchedules
                        )
                    }
                }
        }
    }
}