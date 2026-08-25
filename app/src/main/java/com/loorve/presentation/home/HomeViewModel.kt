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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class NearestExamUiModel(
    val subjectName: String,
    val daysLeft: Int,
    val examDateFormatted: String
)

data class ProgressUiModel(
    val id: String,
    val examId: String,
    val content: String,
    val completed: Int,
    val total: Int,
    val dateFormatted: String
)

data class HomeUiState(
    val exams: List<Exam> = emptyList(),
    val progressList: List<ProgressUiModel> = emptyList(),
    val nearestExam: NearestExamUiModel? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val saveMessage: String? = null,
    val errorMessage: String? = null
)

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
                                // ✅ Long → Int 명시적 변환
                                val days     = ChronoUnit.DAYS.between(today, examDate).toInt()
                                if (days >= 0) {
                                    NearestExamUiModel(
                                        subjectName       = exam.subjectName,
                                        daysLeft          = days,
                                        // ✅ examDate(LocalDate)를 String으로 포맷
                                        examDateFormatted = examDate.format(displayFormatter)
                                    )
                                } else null
                            }.getOrNull()
                        }
                        .minByOrNull { it.daysLeft }

                    _uiState.update {
                        it.copy(
                            isLoading   = false,
                            exams       = exams,
                            nearestExam = nearest
                        )
                    }
                }
        }
    }

    fun loadProgressList() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch {
            getProgressListUseCase(uid)
                .catch { /* 진도 로딩 실패는 조용히 무시 */ }
                .collect { list ->
                    val uiList = list.map { p ->
                        ProgressUiModel(
                            // ✅ Progress 도메인 모델의 실제 필드명 사용
                            id            = p.progressId,
                            examId        = p.examId,
                            content       = p.content,
                            completed     = p.completedCount,
                            total         = p.totalCount,
                            // ✅ createdAt(Long epoch ms) → LocalDate → 포맷
                            dateFormatted = runCatching {
                                Instant.ofEpochMilli(p.createdAt)
                                    .atZone(ZoneId.of("Asia/Seoul"))
                                    .toLocalDate()
                                    .format(displayFormatter)
                            }.getOrElse { p.createdAt.toString() }
                        )
                    }
                    _uiState.update { it.copy(progressList = uiList) }
                }
        }
    }

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

    fun clearSaveMessage() {
        _uiState.update { it.copy(saveMessage = null) }
    }

    fun consumeErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}