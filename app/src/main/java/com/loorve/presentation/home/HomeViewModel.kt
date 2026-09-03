package com.loorve.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.loorve.domain.model.Exam
import com.loorve.domain.model.Progress
import com.loorve.domain.model.ReviewBlock
import com.loorve.domain.usecase.GetExamsUseCase
import com.loorve.domain.usecase.GetProgressListUseCase
import com.loorve.domain.usecase.SaveProgressAndScheduleUseCase
import com.loorve.domain.repository.ReviewBlockRepository
import com.loorve.domain.repository.ReviewScheduleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.UUID
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
    val createdAt: Long = 0L
)

data class ReviewScheduleUiModel(
    val scheduleId: String,
    val originProgressId: String,
    val examId: String,
    val content: String,
    val reviewDate: LocalDate,
    val reviewOrder: Int
)

data class ReviewBlockUiModel(
    val blockId: String,
    val examName: String,
    val dDay: Int,
    val completionRate: Float,
    val examDateMillis: Long,
    val prepStartDateMillis: Long,
    val dailyCap: Int
)

data class HomeUiState(
    val exams: List<Exam> = emptyList(),
    val progressList: List<ProgressUiModel> = emptyList(),
    val nearestExam: NearestExamUiModel? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val saveMessage: String? = null,
    val errorMessage: String? = null,
    val weeklyCompletionRate: Float = 0f,
    val weeklyCompleted: Int = 0,
    val weeklyTotal: Int = 0,
    val scheduledDates: Set<LocalDate> = emptySet(),
    val reviewScheduleDates: Set<LocalDate> = emptySet(),
    val reviewSchedules: List<ReviewScheduleUiModel> = emptyList(),
    val reviewBlocks: List<ReviewBlockUiModel> = emptyList(),
    val isCreatingBlock: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getExamsUseCase: GetExamsUseCase,
    private val saveProgressAndScheduleUseCase: SaveProgressAndScheduleUseCase,
    private val getProgressListUseCase: GetProgressListUseCase,
    private val reviewBlockRepository: ReviewBlockRepository,
    private val reviewScheduleRepository: ReviewScheduleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val displayFormatter = DateTimeFormatter.ofPattern("M월 d일")
    private val seoulZone = ZoneId.of("Asia/Seoul")
    private val dateRangeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    private var reviewScheduleJob: Job? = null

    init {
        loadExams()
        loadProgressList()
        loadReviewBlocks()
        loadReviewScheduleDatesByMonth(YearMonth.now())
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
                                    .atZone(seoulZone).toLocalDate()
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
                        it.copy(isLoading = false, exams = exams, nearestExam = nearest)
                    }
                }
        }
    }

    fun loadProgressList() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch {
            getProgressListUseCase(uid)
                .catch { }
                .collect { list ->
                    val uiList = list.map { p ->
                        ProgressUiModel(
                            id = p.progressId,
                            examId = p.examId,
                            content = p.content,
                            completed = p.completedCount,
                            total = p.totalCount,
                            createdAt = p.createdAt,
                            dateFormatted = runCatching {
                                Instant.ofEpochMilli(p.createdAt)
                                    .atZone(seoulZone).toLocalDate()
                                    .format(displayFormatter)
                            }.getOrElse { p.createdAt.toString() }
                        )
                    }
                    val today = LocalDate.now()
                    val weekStart = today.with(java.time.DayOfWeek.MONDAY)
                    val weekEnd = today.with(java.time.DayOfWeek.SUNDAY)
                    val weeklyList = uiList.filter { p ->
                        runCatching {
                            val d = Instant.ofEpochMilli(p.createdAt).atZone(seoulZone).toLocalDate()
                            !d.isBefore(weekStart) && !d.isAfter(weekEnd)
                        }.getOrElse { false }
                    }
                    val weeklyCompleted = weeklyList.sumOf { it.completed }
                    val weeklyTotal = weeklyList.sumOf { it.total }
                    val weeklyRate = if (weeklyTotal > 0) weeklyCompleted.toFloat() / weeklyTotal else 0f

                    val scheduledDates = uiList.mapNotNull { p ->
                        runCatching {
                            Instant.ofEpochMilli(p.createdAt).atZone(seoulZone).toLocalDate()
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

                    // ✅ progressList 갱신 후 reviewSchedules 콘텐츠도 즉시 재매핑
                    refreshReviewSchedulesFromCache()
                }
        }
    }

    fun loadReviewScheduleDatesByMonth(yearMonth: YearMonth) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        reviewScheduleJob?.cancel()

        reviewScheduleJob = viewModelScope.launch {
            val startDate = yearMonth.atDay(1).format(dateRangeFormatter)
            val endDate = yearMonth.atEndOfMonth().format(dateRangeFormatter)

            reviewScheduleRepository
                .getReviewSchedulesByDateRange(uid, startDate, endDate)
                .catch { }
                .collect { schedules ->
                    val currentProgressMap = _uiState.value.progressList.associateBy { it.id }

                    val reviewScheduleUiModels = schedules.mapNotNull { schedule ->
                        val localDate = runCatching {
                            Instant.ofEpochMilli(schedule.reviewDate)
                                .atZone(seoulZone)
                                .toLocalDate()
                        }.getOrNull() ?: return@mapNotNull null

                        // ✅ originProgressId가 "" 이면 content를 schedule 자체 title로 대체
                        val originProgress = currentProgressMap[schedule.originProgressId]
                        val displayContent = originProgress?.content
                            ?: schedule.originProgressId.ifBlank { "복습 일정" }
                        val displayExamId = originProgress?.examId ?: ""

                        ReviewScheduleUiModel(
                            scheduleId       = schedule.scheduleId,
                            originProgressId = schedule.originProgressId,
                            examId           = displayExamId,
                            content          = displayContent,
                            reviewDate       = localDate,
                            reviewOrder      = schedule.reviewOrder
                        )
                    }

                    val reviewDates = reviewScheduleUiModels.map { it.reviewDate }.toSet()

                    _uiState.update { state ->
                        state.copy(
                            reviewScheduleDates = reviewDates,
                            reviewSchedules     = reviewScheduleUiModels
                        )
                    }
                }
        }
    }

    // ✅ progressList가 갱신된 뒤 이미 로드된 reviewSchedules의 content/examId를 재매핑
    private fun refreshReviewSchedulesFromCache() {
        val currentProgressMap = _uiState.value.progressList.associateBy { it.id }
        val refreshed = _uiState.value.reviewSchedules.map { schedule ->
            val originProgress = currentProgressMap[schedule.originProgressId]
            if (originProgress != null) {
                schedule.copy(
                    examId  = originProgress.examId,
                    content = originProgress.content
                )
            } else {
                schedule
            }
        }
        _uiState.update { it.copy(reviewSchedules = refreshed) }
    }

    fun loadReviewBlocks() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch {
            reviewBlockRepository.getReviewBlocks(uid)
                .onSuccess { blocks: List<ReviewBlock> ->
                    val today = LocalDate.now()
                    val uiBlocks = blocks.map { block: ReviewBlock ->
                        val examLocalDate = Instant.ofEpochMilli(block.examDate)
                            .atZone(seoulZone).toLocalDate()
                        val dDay = ChronoUnit.DAYS.between(today, examLocalDate).toInt()
                        ReviewBlockUiModel(
                            blockId            = block.blockId,
                            examName           = block.examName.ifBlank { block.title },
                            dDay               = dDay,
                            completionRate     = 0f,
                            examDateMillis     = block.examDate,
                            prepStartDateMillis = block.prepStartDate,
                            dailyCap           = block.dailyCap
                        )
                    }
                    _uiState.update { it.copy(reviewBlocks = uiBlocks) }
                }
                .onFailure { }
        }
    }

    fun createReviewBlock(
        examName: String,
        examDateMillis: Long,
        prepStartDateMillis: Long,
        dailyCap: Int
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrBlank()) {
            _uiState.update { it.copy(saveMessage = "로그인 정보가 없습니다.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isCreatingBlock = true, errorMessage = null) }
            val block = ReviewBlock(
                blockId         = "",
                uid             = uid,
                examName        = examName,
                title           = examName,
                examDate        = examDateMillis,
                prepStartDate   = prepStartDateMillis,
                dailyCap        = dailyCap,
                createdAt       = System.currentTimeMillis(),
                updatedAt       = System.currentTimeMillis()
            )
            reviewBlockRepository.saveReviewBlock(block)
                .onSuccess {
                    _uiState.update { it.copy(isCreatingBlock = false, saveMessage = "복습 블록이 생성되었습니다.") }
                    loadReviewBlocks()
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isCreatingBlock = false, errorMessage = e.message ?: "생성에 실패했습니다.")
                    }
                }
        }
    }

    fun addProgress(examId: String, content: String, completedCount: Int, totalCount: Int) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    saveMessage  = "로그인 정보가 없습니다. 다시 로그인해 주세요.",
                    errorMessage = "로그인 정보가 없습니다."
                )
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveMessage = null, errorMessage = null) }

            val progress = Progress(
                progressId     = UUID.randomUUID().toString(), // ✅ 핵심 수정: ID 미리 생성
                examId         = examId,
                content        = content.trim(),
                completedCount = completedCount,
                totalCount     = totalCount,
                isCompleted    = totalCount > 0 && completedCount >= totalCount,
                createdAt      = System.currentTimeMillis()   // ✅ 저장 시각도 미리 확정
            )

            val result = saveProgressAndScheduleUseCase(uid, progress)

            if (result.isSuccess) {
                _uiState.update { it.copy(isSaving = false, saveMessage = "학습 진도가 저장되었습니다.") }
                loadProgressList()
                loadReviewScheduleDatesByMonth(YearMonth.now())
            } else {
                _uiState.update {
                    it.copy(
                        isSaving     = false,
                        saveMessage  = result.exceptionOrNull()?.message ?: "저장하지 못했습니다.",
                        errorMessage = result.exceptionOrNull()?.message
                    )
                }
            }
        }
    }

    fun clearSaveMessage() = _uiState.update { it.copy(saveMessage = null) }
    fun consumeErrorMessage() = _uiState.update { it.copy(errorMessage = null) }
}