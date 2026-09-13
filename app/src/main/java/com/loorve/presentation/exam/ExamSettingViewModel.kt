package com.loorve.presentation.exam

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.loorve.domain.usecase.SaveExamRequest
import com.loorve.domain.usecase.SaveExamWithSchedulingUseCase
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
    val studyEndDate: Long = 0L,          // 추가: 학습 종료일
    val dDayText: String = "",
    val studyEndDateError: String? = null, // 추가: 유효성 오류 메시지
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val canSaveExamDate: Boolean = true,
    val helperMessages: List<String> = emptyList(),
    val recommendedEarliestExamDate: LocalDate? = null,
    val warnings: List<String> = emptyList(),
    val finalReviewBufferDaysText: String = "1",
    val maxDailyReviewMinutesText: String = "",
    val timezone: String = "Asia/Seoul",
    val cramRequiredCount: Int = 0
)

sealed class ExamSettingEvent {
    object SaveSuccess : ExamSettingEvent()
}

@HiltViewModel
class ExamSettingViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val saveExamUseCase: SaveExamWithSchedulingUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExamSettingUiState())
    val uiState: StateFlow<ExamSettingUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ExamSettingEvent>(replay = 0)
    val events: SharedFlow<ExamSettingEvent> = _events.asSharedFlow()

    fun onSubjectNameChange(name: String) {
        _uiState.update { it.copy(subjectName = name) }
    }

    fun onExamDateSelected(epochMillis: Long) {
        _uiState.update { current ->
            val newStudyEndDateError = validateStudyEndDate(current.studyEndDate, epochMillis)
            current.copy(
                examDate          = epochMillis,
                dDayText          = calculateDDay(epochMillis),
                studyEndDateError = newStudyEndDateError,
                canSaveExamDate = true,
                errorMessage = null,
                recommendedEarliestExamDate = null,
                cramRequiredCount = 0,
                warnings = emptyList()
            )
        }
    }

    fun onStudyEndDateSelected(epochMs: Long) {
        _uiState.update { current ->
            val error = validateStudyEndDate(epochMs, current.examDate)
            current.copy(
                studyEndDate      = epochMs,
                studyEndDateError = error
            )
        }

    }

    fun onFinalReviewBufferDaysChange(value: String) {
        if (value.all(Char::isDigit) && value.length <= 2) {
            _uiState.update { it.copy(finalReviewBufferDaysText = value) }
        }
    }

    fun onMaxDailyReviewMinutesChange(value: String) {
        if (value.isEmpty() || (value.all(Char::isDigit) && value.length <= 4)) {
            _uiState.update { it.copy(maxDailyReviewMinutesText = value) }
        }
    }

    fun onTimezoneChange(value: String) {
        _uiState.update { it.copy(timezone = value) }
    }

    fun saveExam() {
        val state = _uiState.value
        if (state.isLoading) return

        // 학습 종료일이 설정됐는데 오류가 있으면 저장 차단
        if (state.studyEndDate != 0L && state.studyEndDateError != null) return

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val uid = firebaseAuth.currentUser?.uid
                if (uid.isNullOrBlank()) {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "로그인 정보를 찾을 수 없습니다.")
                    }
                    return@launch
                }
                val result = saveExamUseCase(
                    uid = uid,
                    request = SaveExamRequest(
                        examName = state.subjectName,
                        examDate = Instant.ofEpochMilli(state.examDate)
                            .atZone(ZoneId.of("Asia/Seoul")).toLocalDate(),
                        timezone = state.timezone,
                        finalReviewBufferDays = state.finalReviewBufferDaysText.toIntOrNull(),
                        maxDailyReviewMinutes = state.maxDailyReviewMinutesText.toIntOrNull()
                    )
                )
                result.fold(
                    onSuccess = { saveResult ->
                        if (!saveResult.canSaveExamDate) {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    canSaveExamDate = false,
                                    errorMessage = saveResult.primaryMessage,
                                    helperMessages = saveResult.helperMessages,
                                    recommendedEarliestExamDate = saveResult.recommendedEarliestExamDate,
                                    warnings = saveResult.warnings,
                                    cramRequiredCount = saveResult.cramRequiredCount
                                )
                            }
                        } else {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    canSaveExamDate = true,
                                    helperMessages = saveResult.helperMessages,
                                    recommendedEarliestExamDate = saveResult.recommendedEarliestExamDate,
                                    warnings = saveResult.warnings,
                                    cramRequiredCount = saveResult.cramRequiredCount
                                )
                            }
                            _events.emit(ExamSettingEvent.SaveSuccess)
                        }
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

    /**
     * 학습 종료일 유효성 검사: studyEndDate < examDate 조건
     * @return 오류 메시지(null이면 유효)
     */
    private fun validateStudyEndDate(studyEndDate: Long, examDate: Long): String? {
        if (studyEndDate <= 0L || examDate <= 0L) return null
        return if (studyEndDate >= examDate) "학습 종료일은 시험일보다 이전이어야 합니다." else null
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