// 경로: app/src/main/java/com/loorve/presentation/exam/ExamSettingViewModel.kt
package com.loorve.presentation.exam

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loorve.domain.model.Exam
import com.loorve.domain.usecase.AddExamUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class ExamSettingUiState(
    val subjectName: String = "",
    val examDate: Long = 0L,
    val dDayText: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

// 저장 완료 이벤트는 SharedFlow로 분리 (ONE-SHOT 보장)
sealed class ExamSettingEvent {
    object SaveSuccess : ExamSettingEvent()
}

@HiltViewModel
class ExamSettingViewModel @Inject constructor(
    private val addExamUseCase: AddExamUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExamSettingUiState())
    val uiState: StateFlow<ExamSettingUiState> = _uiState.asStateFlow()

    // replay=0 → 한 번만 소비, 재구독 시 재전달 없음
    private val _events = MutableSharedFlow<ExamSettingEvent>(replay = 0)
    val events: SharedFlow<ExamSettingEvent> = _events.asSharedFlow()

    fun onSubjectNameChange(name: String) {
        _uiState.update { it.copy(subjectName = name) }
    }

    fun onExamDateSelected(epochMillis: Long) {
        _uiState.update {
            it.copy(
                examDate  = epochMillis,
                dDayText  = calculateDDay(epochMillis)
            )
        }
    }

    fun saveExam() {
        val state = _uiState.value
        if (state.isLoading) return          // 중복 클릭 방지
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val result = addExamUseCase(
                    Exam(
                        id          = "",   // ✅ 신규 생성이므로 빈 문자열 (Firestore가 ID 부여)
                        subjectName = state.subjectName,
                        examDate    = state.examDate
                    )
                )
                result.fold(
                    onSuccess = {
                        _uiState.update { it.copy(isLoading = false) }
                        _events.emit(ExamSettingEvent.SaveSuccess)
                    },
                    onFailure = { throwable ->
                        _uiState.update {
                            it.copy(
                                isLoading    = false,
                                errorMessage = throwable.message ?: "저장 중 오류가 발생했습니다."
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading    = false,
                        errorMessage = e.message ?: "알 수 없는 오류가 발생했습니다."
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun calculateDDay(epochMillis: Long): String {
        if (epochMillis <= 0L) return ""
        val today = LocalDate.now()
        val examLocalDate = Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        val days = ChronoUnit.DAYS.between(today, examLocalDate)
        return when {
            days > 0L  -> "D-$days"
            days == 0L -> "D-Day"
            else       -> "D+${-days}"
        }
    }
}
