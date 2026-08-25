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
    val dateFormatted: String,
    val createdAt: Long = 0L          // ✅ 추가: HomeScreen 달력 필터링용
)

data class HomeUiState(
    val exams: List<Exam> = emptyList(),
    val progressList: List<ProgressUiModel> = emptyList(),
    val nearestExam: NearestExamUiModel? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val saveMessage: String? = null,
    val errorMessage: String? = null,
    // ✅ 추가: 주간 복습률 카드용
    val weeklyCompletionRate: Float = 0f,
    val weeklyCompleted: Int = 0,
    val weeklyTotal: Int = 0,
    // ✅ 추가: 미니 달력 점 표시용
    val scheduledDates: Set<LocalDate> = emptySet()
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
    private val seoulZone = ZoneId.of("Asia/Seoul")

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
                            isLoading = false,
                            errorMessage = exception.message ?: "목록을 불러오지 못했습니다."
                        )
                    }
                }
                .collect { exams ->
                    val nearest = exams
                        .mapNotNull { exam ->
                            runCatching {
                                val examDate = Instant.ofEpochMilli(exam.examDate)
                                    .atZone(seoulZone)
                                    .toLocalDate()
                                val today = LocalDate.now()
                                val days = ChronoUnit.DAYS.between(today, examDate).toInt()
                                if (days >= 0) {
                                    NearestExamUiModel(
                                        subjectName = exam.subjectName,
                                        daysLeft = days,
                                        examDateFormatted = examDate.format(displayFormatter)
                                    )
                                } else null
                            }.getOrNull()
                        }
                        .minByOrNull { it.daysLeft }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            exams = exams,
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
                .catch { /* 진도 로딩 실패는 무시 */ }
                .collect { list ->
                    val uiList = list.map { p ->
                        ProgressUiModel(
                            id = p.progressId,
                            examId = p.examId,
                            content = p.content,
                            completed = p.completedCount,
                            total = p.totalCount,
                            createdAt = p.createdAt,   // ✅ 원본 epoch ms 보존
                            dateFormatted = runCatching {
                                Instant.ofEpochMilli(p.createdAt)
                                    .atZone(seoulZone)
                                    .toLocalDate()
                                    .format(displayFormatter)
                            }.getOrElse { p.createdAt.toString() }
                        )
                    }

                    // ✅ 주간 복습률 계산 (이번 주 월~일)
                    val today = LocalDate.now()
                    val weekStart = today.with(java.time.DayOfWeek.MONDAY)
                    val weekEnd = today.with(java.time.DayOfWeek.SUNDAY)
                    val weeklyList = uiList.filter { p ->
                        runCatching {
                            val d = Instant.ofEpochMilli(p.createdAt)
                                .atZone(seoulZone).toLocalDate()
                            !d.isBefore(weekStart) && !d.isAfter(weekEnd)
                        }.getOrElse { false }
                    }
                    val weeklyCompleted = weeklyList.sumOf { it.completed }
                    val weeklyTotal = weeklyList.sumOf { it.total }
                    val weeklyRate = if (weeklyTotal > 0)
                        weeklyCompleted.toFloat() / weeklyTotal.toFloat() else 0f

                    // ✅ 달력 점 표시용 날짜 Set 생성
                    val scheduledDates = uiList.mapNotNull { p ->
                        runCatching {
                            Instant.ofEpochMilli(p.createdAt)
                                .atZone(seoulZone).toLocalDate()
                        }.getOrNull()
                    }.toSet()

                    _uiState.update {
                        it.copy(
                            progressList = uiList,
                            weeklyCompleted = weeklyCompleted,
                            weeklyTotal = weeklyTotal,
                            weeklyCompletionRate = weeklyRate,
                            scheduledDates = scheduledDates
                        )
                    }
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
                    saveMessage = "로그인 정보가 없습니다. 다시 로그인해 주세요.",
                    errorMessage = "로그인 정보가 없습니다. 다시 로그인해 주세요."
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveMessage = null, errorMessage = null) }

            val progress = Progress(
                examId = examId,
                content = content.trim(),
                completedCount = completedCount,
                totalCount = totalCount,
                isCompleted = totalCount > 0 && completedCount >= totalCount
            )

            val result = addProgressUseCase(uid, progress)
            _uiState.update {
                if (result.isSuccess) {
                    it.copy(isSaving = false, saveMessage = "학습 진도가 저장되었습니다.")
                } else {
                    it.copy(
                        isSaving = false,
                        saveMessage = result.exceptionOrNull()?.message ?: "저장하지 못했습니다.",
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