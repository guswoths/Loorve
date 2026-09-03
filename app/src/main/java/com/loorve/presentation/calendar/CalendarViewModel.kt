package com.loorve.presentation.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.loorve.domain.repository.ReviewScheduleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class CalendarUiState(
    val isLoading: Boolean = false,
    val currentMonth: LocalDate = LocalDate.now().withDayOfMonth(1),
    val selectedDate: LocalDate? = null,
    val reviewDates: Set<LocalDate> = emptySet(),
    val examDates: Set<LocalDate> = emptySet(),
    val selectedDateItems: List<CalendarItem> = emptyList(),
    val error: String? = null
)

data class CalendarItem(
    val id: String = "",
    val title: String = "",
    val subtitle: String = "",
    val type: CalendarItemType = CalendarItemType.REVIEW
)

enum class CalendarItemType { REVIEW, EXAM }

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val reviewScheduleRepository: ReviewScheduleRepository  // ✅ Repository 주입
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val kstZone = ZoneId.of("Asia/Seoul")

    init {
        loadCurrentMonthSchedules()
    }

    fun selectDate(date: LocalDate) {
        _uiState.update { state ->
            val items = buildSelectedDateItems(state, date)
            state.copy(selectedDate = date, selectedDateItems = items)
        }
    }

    fun previousMonth() {
        _uiState.update { it.copy(currentMonth = it.currentMonth.minusMonths(1)) }
        loadCurrentMonthSchedules()
    }

    fun nextMonth() {
        _uiState.update { it.copy(currentMonth = it.currentMonth.plusMonths(1)) }
        loadCurrentMonthSchedules()
    }

    fun reloadCurrentMonth() {
        loadCurrentMonthSchedules()
    }

    private fun loadCurrentMonthSchedules() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrBlank()) {
            _uiState.update { it.copy(isLoading = false) }
            return
        }

        val yearMonth = YearMonth.from(_uiState.value.currentMonth)
        val startDate = yearMonth.atDay(1).format(dateFormatter)
        val endDate = yearMonth.atEndOfMonth().format(dateFormatter)

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            reviewScheduleRepository
                .getReviewSchedulesByDateRange(uid, startDate, endDate)
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
                .collectLatest { schedules ->
                    // reviewDate(millis) → LocalDate(KST) 변환 후 Set으로
                    val reviewDates = schedules.map { schedule ->
                        Instant.ofEpochMilli(schedule.reviewDate)
                            .atZone(kstZone)
                            .toLocalDate()
                    }.toSet()

                    _uiState.update { state ->
                        val selectedItems = state.selectedDate?.let {
                            buildSelectedDateItems(state.copy(reviewDates = reviewDates), it)
                        } ?: emptyList()
                        state.copy(
                            isLoading = false,
                            reviewDates = reviewDates,
                            selectedDateItems = selectedItems
                        )
                    }
                }
        }
    }

    private fun buildSelectedDateItems(
        state: CalendarUiState,
        date: LocalDate
    ): List<CalendarItem> {
        // 현재는 reviewDates 기반 더미 아이템 — 추후 schedule 상세 정보로 확장 가능
        return if (state.reviewDates.contains(date)) {
            listOf(CalendarItem(title = "복습 일정 있음", subtitle = date.toString(), type = CalendarItemType.REVIEW))
        } else emptyList()
    }
}