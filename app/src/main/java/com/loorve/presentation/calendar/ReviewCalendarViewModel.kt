package com.loorve.presentation.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.loorve.domain.model.ReviewSchedule
import com.loorve.domain.repository.ReviewScheduleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val reviewScheduleRepository: ReviewScheduleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReviewCalendarUiState())
    val uiState: StateFlow<ReviewCalendarUiState> = _uiState.asStateFlow()

    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

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

    fun onDismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun loadCurrentMonth() {
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

        viewModelScope.launch {
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
