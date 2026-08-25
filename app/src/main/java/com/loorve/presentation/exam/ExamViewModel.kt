package com.loorve.presentation.exam

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import java.time.LocalDate

data class ExamUiModel(
    val id: String = "",
    val subjectName: String = "",
    val examDate: LocalDate = LocalDate.now(),
    val examDateFormatted: String = "",
    val daysLeft: Int = 0
)

data class ExamUiState(
    val isLoading: Boolean = false,
    val exams: List<ExamUiModel> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class ExamViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(ExamUiState())
    val uiState: StateFlow<ExamUiState> = _uiState.asStateFlow()

    fun addExam(subjectName: String, examDate: LocalDate) {
        val newExam = ExamUiModel(
            id = java.util.UUID.randomUUID().toString(),
            subjectName = subjectName,
            examDate = examDate,
            examDateFormatted = examDate.toString(),
            daysLeft = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), examDate).toInt()
        )
        _uiState.update { it.copy(exams = it.exams + newExam) }
    }

    fun updateExam(id: String, subjectName: String, examDate: LocalDate) {
        _uiState.update { state ->
            state.copy(exams = state.exams.map { exam ->
                if (exam.id == id) exam.copy(
                    subjectName = subjectName,
                    examDate = examDate,
                    examDateFormatted = examDate.toString(),
                    daysLeft = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), examDate).toInt()
                ) else exam
            })
        }
    }

    fun deleteExam(id: String) {
        _uiState.update { it.copy(exams = it.exams.filter { exam -> exam.id != id }) }
    }
}