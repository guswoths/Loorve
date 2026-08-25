package com.loorve.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.loorve.domain.model.Exam
import com.loorve.domain.model.Progress
import com.loorve.domain.usecase.AddProgressUseCase
import com.loorve.domain.usecase.GetExamsUseCase
import com.loorve.domain.usecase.GetProgressListUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject

// ── UI 표시용 중간 모델 ─────────────────────────────────────────────────────

data class NearestExamUiModel(
    val subjectName: String,         // HomeScreen: exam.subjectName
    val daysLeft: Int,               // HomeScreen: daysLeft
    val examDateFormatted: String    // HomeScreen: examDateFormatted
)

data class ProgressUiModel(
    val id: String,                  // HomeScreen: progress.id
    val examId: String,
    val content: String,
    val completed: Int,              // HomeScreen: completed
    val total: Int,                  // HomeScreen: total
    val dateFormatted: String        // HomeScreen: dateFormatted
)

// ── UiState ────────────────────────────────────────────────────────────────

data class HomeUiState(
    val exams: List<Exam> = emptyList(),
    val progressList: List<ProgressUiModel> = emptyList(),
    val nearestExam: NearestExamUiModel? = null,  // HomeScreen: nearestExam
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,                // HomeScreen: isSaving
    val saveMessage: String? = null,              // HomeScreen: saveMessage
    val errorMessage: String? = null
)

// ── ViewModel ─────────────────────────────────────────────────────────────

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getExamsUseCase: GetExamsUseCase,
    private val addProgressUseCase: AddProgressUseCase,
    private val getProgressListUseCase: GetProgressListUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val displayFormatter = DateTimeFormatter.ofPattern("M월 d일")

    init {
        loadExams()
        loadProgressList()
    }

    // ── 시험 목록 로드 + nearestExam 계산 ──────────────────────────────────
    fun loadExams() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            getExamsUseCase()
                .catch { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading    = false,
                            errorMessage = exception.message ?: "목록을 불러오지 못했습니다."
                        )
                    }
                }
                .collect { exams ->
                    val nearest = exams
                        .mapNotNull { exam ->
                            runCatching {
                                val examDate = LocalDate.parse(exam.examDate)
                                val today    = LocalDate.now()
                                val days     = ChronoUnit.DAYS.between(today, examDate).toInt()
                                if (days >= 0) {
                                    NearestExamUiModel(
                                        subjectName       = exam.subjectName,
                                        daysLeft          = days,
                                        examDateFormatted = examDate.format(displayFormatter)
                                    )
                                } else null
                            }.getOrNull()
                        }
                        .minByOrNull { it.daysLeft }

                    _uiState.update {
                        it.copy(
                            isLoading    = false,
                            exams        = exams,
                            nearestExam  = nearest
                        )
                    }
                }
        }
    }

    // ── 진도 목록 로드 ──────────────────────────────────────────────────────
    fun loadProgressList() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch {
            getProgressListUseCase(uid)
                .catch { /* 진도 로딩 실패는 조용히 무시 */ }
                .collect { list ->
                    val uiList = list.map { p ->
                        ProgressUiModel(
                            id            = p.id,
                            examId        = p.examId,
                            content       = p.content,
                            completed     = p.completedCount,
                            total         = p.totalCount,
                            dateFormatted = runCatching {
                                LocalDate.parse(p.date).format(displayFormatter)
                            }.getOrElse { p.date }
                        )
                    }
                    _uiState.update { it.copy(progressList = uiList) }
                }
        }
    }

    // ── 진도 저장 ───────────────────────────────────────────────────────────
    fun addProgress(
        examId: String,
        content: String,
        completedCount: Int,
        totalCount: Int
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    saveMessage  = "로그인 정보가 없습니다. 다시 로그인해 주세요.",
                    errorMessage = "로그인 정보가 없습니다. 다시 로그인해 주세요."
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveMessage = null, errorMessage = null) }

            val progress = Progress(
                examId         = examId,
                content        = content.trim(),
                completedCount = completedCount,
                totalCount     = totalCount,
                isCompleted    = totalCount > 0 && completedCount >= totalCount
            )

            val result = addProgressUseCase(uid, progress)
            _uiState.update {
                if (result.isSuccess) {
                    it.copy(isSaving = false, saveMessage = "학습 진도가 저장되었습니다.")
                } else {
                    it.copy(
                        isSaving     = false,
                        saveMessage  = result.exceptionOrNull()?.message ?: "저장하지 못했습니다.",
                        errorMessage = result.exceptionOrNull()?.message ?: "저장하지 못했습니다."
                    )
                }
            }
        }
    }

    // ── 메시지 소비 (HomeScreen에서 LaunchedEffect 후 호출) ──────────────────
    fun clearSaveMessage() {
        _uiState.update { it.copy(saveMessage = null) }
    }

    fun consumeErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}