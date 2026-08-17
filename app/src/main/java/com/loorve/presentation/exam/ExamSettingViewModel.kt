package com.loorve.presentation.exam

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loorve.domain.model.Exam
import com.loorve.domain.usecase.AddExamUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * 시험 설정 화면의 UI 상태
 */
data class ExamSettingUiState(
    val subjectName: String = "",
    val examDate: Long = 0L,
    val dDayText: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSaveSuccess: Boolean = false
)

/**
 * 시험 설정 화면 ViewModel
 */
@HiltViewModel
class ExamSettingViewModel @Inject constructor(
    private val addExamUseCase: AddExamUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExamSettingUiState())
    val uiState: StateFlow<ExamSettingUiState> = _uiState.asStateFlow()

    /** 과목명 변경 */
    fun onSubjectNameChange(name: String) {
        _uiState.update { it.copy(subjectName = name) }
    }

    /** 시험일 선택 후 D-day 자동 계산 */
    fun onExamDateSelected(epochMillis: Long) {
        _uiState.update {
            it.copy(
                examDate = epochMillis,
                dDayText = calculateDDay(epochMillis)
            )
        }
    }

    /** 시험 저장 */
    fun saveExam() {
        val state = _uiState.value
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val result = addExamUseCase(
                    Exam(
                        subjectName = state.subjectName,
                        examDate = state.examDate
                    )
                )
                result.fold(
                    onSuccess = {
                        _uiState.update { it.copy(isLoading = false, isSaveSuccess = true) }
                        // 네비게이션 완료 후 중복 트리거 방지
                    },
                    onFailure = { throwable ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = throwable.message ?: "저장 중 오류가 발생했습니다."
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "알 수 없는 오류가 발생했습니다."
                    )
                }
            }
        }
    }

    /** 에러 메시지 초기화 */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun resetSaveSuccess() {
        _uiState.update { it.copy(isSaveSuccess = false) }
    }

    /**
     * D-day 텍스트 계산
     * - 양수: "D-{days}"
     * - 0: "D-Day"
     * - 음수: "D+{abs(days)}"
     * - 0L (미선택): ""
     */
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
