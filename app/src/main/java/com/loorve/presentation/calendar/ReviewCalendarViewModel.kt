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
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class ReviewCalendarUiState(
    val displayYearMonth: YearMonth = YearMonth.now(),
    val schedulesMap: Map<LocalDate, List<ReviewSchedule>> = emptyMap(),
    val selectedDate: LocalDate? = null,
    val selectedDateSchedules: List<ReviewSchedule> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class ReviewCalendarViewModel @Inject constructor(
    private val reviewScheduleRepository: ReviewScheduleRepository,
    private val updateReviewCompletionUseCase: UpdateReviewCompletionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReviewCalendarUiState())
    val uiState: StateFlow<ReviewCalendarUiState> = _uiState.asStateFlow()

    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private var loadJob: Job? = null

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
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch {
            val result = reviewScheduleRepository.completeReviewSchedule(uid, scheduleId)
            if (result.isFailure) {
                _uiState.update {
                    it.copy(
                        errorMessage = result.exceptionOrNull()?.message ?: "복습 완료 처리에 실패했습니다."
                    )
                }
            }
        }
    }

    fun toggleReviewCompletion(scheduleId: String, currentState: Boolean) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch {
            val result = updateReviewCompletionUseCase(
                uid = uid,
                scheduleId = scheduleId,
                isCompleted = !currentState
            )
            if (result.isFailure) {
                _uiState.update {
                    it.copy(
                        errorMessage = result.exceptionOrNull()?.message ?: "복습 상태 변경에 실패했습니다."
                    )
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

    /**
     * 복습 블록 생성 후 popBackStack으로 복귀 시 호출.
     * 현재 월 스케줄을 Firestore에서 강제 재로드.
     */
    fun reloadCurrentMonth() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        loadSchedulesForMonth(_uiState.value.displayYearMonth)
    }

    private fun loadSchedulesForMonth(yearMonth: YearMonth) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = "로그인 정보가 없습니다. 다시 로그인해 주세요."
                )
            }
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
                        it.copy(
                            isLoading = false,
                            errorMessage = exception.message ?: "일정을 불러오지 못했습니다."
                        )
                    }
                }
                .collectLatest { schedules ->
                    val schedulesMap = schedules.groupBy { schedule ->
                        java.time.Instant.ofEpochMilli(schedule.reviewDate)
                            .atZone(java.time.ZoneId.of("Asia/Seoul"))
                            .toLocalDate()
                    }
                    val selectedDate = _uiState.value.selectedDate
                    val selectedDateSchedules = if (selectedDate != null) {
                        schedulesMap[selectedDate] ?: emptyList()
                    } else {
                        emptyList()
                    }
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