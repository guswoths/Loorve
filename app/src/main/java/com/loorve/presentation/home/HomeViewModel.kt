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
import com.loorve.domain.repository.StudyRecordRepository
import com.loorve.util.CalendarRefreshBus  // ✅ 추가
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.DayOfWeek
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
    val studyRecordDates: Set<LocalDate> = emptySet(),
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
    private val reviewScheduleRepository: ReviewScheduleRepository,
    private val studyRecordRepository: StudyRecordRepository,
    private val calendarRefreshBus: CalendarRefreshBus  // ✅ 추가
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _displayYearMonth = MutableStateFlow(YearMonth.now())
    val displayYearMonth: StateFlow<YearMonth> = _displayYearMonth.asStateFlow()

    private val displayFormatter = DateTimeFormatter.ofPattern("M월 d일")
    private val seoulZone = ZoneId.of("Asia/Seoul")
    private val dateRangeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    private var reviewScheduleJob: Job? = null

    init {
        // ✅ uid를 반드시 토큰 갱신 후 확보, 그 다음 모든 데이터 로드
        viewModelScope.launch {
            val uid = getUidSafely() ?: run {
                _uiState.update { it.copy(isLoading = false) }
                return@launch
            }
            loadExams()
            loadReviewBlocks(uid)
            loadProgressListAndThenSchedules(uid, _displayYearMonth.value)
        }
        // ✅ 다른 화면(복습기록 생성/삭제)에서 이벤트 수신 시 캘린더 자동 갱신
        viewModelScope.launch {
            calendarRefreshBus.refreshEvent.collect {
                refreshCalendar()
            }
        }
    }

    // ✅ 핵심: 토큰 강제 갱신으로 uid를 안전하게 확보
    private suspend fun getUidSafely(): String? {
        val user = FirebaseAuth.getInstance().currentUser ?: return null
        return runCatching {
            user.getIdToken(true).await()
            user.uid
        }.getOrElse { user.uid }
    }

    fun setDisplayYearMonth(yearMonth: YearMonth) {
        _displayYearMonth.update { yearMonth }
        viewModelScope.launch {
            val uid = getUidSafely() ?: return@launch
            loadReviewScheduleDatesByMonth(uid, yearMonth)
            loadStudyRecordDatesByMonth(uid, yearMonth)
        }
    }

    private suspend fun loadProgressListAndThenSchedules(uid: String, yearMonth: YearMonth) {
        try {
            val list = getProgressListUseCase(uid).first()
            applyProgressList(list)
        } catch (_: Exception) {}
        loadReviewScheduleDatesByMonth(uid, yearMonth)
        loadStudyRecordDatesByMonth(uid, yearMonth)
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
                    val nearest = exams.mapNotNull { exam ->
                        runCatching {
                            val examDate = Instant.ofEpochMilli(exam.examDate)
                                .atZone(seoulZone).toLocalDate()
                            val today = LocalDate.now()
                            val days = ChronoUnit.DAYS.between(today, examDate).toInt()
                            if (days >= 0) NearestExamUiModel(
                                subjectName = exam.subjectName,
                                daysLeft = days,
                                examDateFormatted = examDate.format(displayFormatter)
                            ) else null
                        }.getOrNull()
                    }.minByOrNull { it.daysLeft }
                    _uiState.update { it.copy(isLoading = false, exams = exams, nearestExam = nearest) }
                }
        }
    }

    fun loadProgressList() {
        viewModelScope.launch {
            val uid = getUidSafely() ?: return@launch
            getProgressListUseCase(uid)
                .catch { }
                .collect { list ->
                    applyProgressList(list)
                    refreshReviewSchedulesFromCache()
                }
        }
    }

    private fun applyProgressList(list: List<Progress>) {
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
        val weekStart = today.with(DayOfWeek.MONDAY)
        val weekEnd = today.with(DayOfWeek.SUNDAY)
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
    }

    private var studyRecordJob: Job? = null

    // ✅ 복습 블록에서 저장된 StudyRecord의 learningDate 조회하여 캘린더 dot 연동
    fun loadStudyRecordDatesByMonth(uid: String, yearMonth: YearMonth) {
        studyRecordJob?.cancel()
        studyRecordJob = viewModelScope.launch {
            val startMillis = yearMonth.atDay(1).minusDays(1)
                .atStartOfDay(seoulZone).toInstant().toEpochMilli()
            val endMillis = yearMonth.atEndOfMonth().plusDays(1)
                .atTime(23, 59, 59, 999).atZone(seoulZone).toInstant().toEpochMilli()

            studyRecordRepository.getStudyRecordsByDateRange(uid, startMillis, endMillis)
                .onSuccess { records ->
                    val dates = records.mapNotNull { record ->
                        runCatching {
                            Instant.ofEpochMilli(record.learningDate)
                                .atZone(seoulZone)
                                .toLocalDate()
                        }.getOrNull()
                    }.filter { date ->
                        date.year == yearMonth.year && date.monthValue == yearMonth.monthValue
                    }.toSet()

                    _uiState.update { state ->
                        state.copy(studyRecordDates = dates)
                    }
                }
                .onFailure {
                    // 폴백: 전체 조회
                    studyRecordRepository.getAllStudyRecords(uid).onSuccess { allRecords ->
                        val dates = allRecords.mapNotNull { record ->
                            runCatching {
                                Instant.ofEpochMilli(record.learningDate)
                                    .atZone(seoulZone)
                                    .toLocalDate()
                            }.getOrNull()
                        }.filter { date ->
                            date.year == yearMonth.year && date.monthValue == yearMonth.monthValue
                        }.toSet()

                        _uiState.update { state ->
                            state.copy(studyRecordDates = dates)
                        }
                    }
                }
        }
    }

    // ✅ uid를 파라미터로 받아서 절대 null 상황이 생기지 않도록 변경
    fun loadReviewScheduleDatesByMonth(uid: String, yearMonth: YearMonth) {
        loadStudyRecordDatesByMonth(uid, yearMonth)
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

                        val originProgress = currentProgressMap[schedule.originProgressId]
                        val displayContent = originProgress?.content
                            ?: schedule.title.ifBlank { "복습 일정 ${schedule.reviewOrder}회차" }
                        val displayExamId = originProgress?.examId ?: ""

                        ReviewScheduleUiModel(
                            scheduleId = schedule.scheduleId,
                            originProgressId = schedule.originProgressId,
                            examId = displayExamId,
                            content = displayContent,
                            reviewDate = localDate,
                            reviewOrder = schedule.reviewOrder
                        )
                    }

                    val reviewDates = reviewScheduleUiModels.map { it.reviewDate }.toSet()
                    _uiState.update { state ->
                        state.copy(
                            reviewScheduleDates = reviewDates,
                            reviewSchedules = reviewScheduleUiModels
                        )
                    }
                }
        }
    }

    private fun refreshReviewSchedulesFromCache() {
        val currentProgressMap = _uiState.value.progressList.associateBy { it.id }
        val refreshed = _uiState.value.reviewSchedules.map { schedule ->
            val originProgress = currentProgressMap[schedule.originProgressId]
            if (originProgress != null) {
                schedule.copy(examId = originProgress.examId, content = originProgress.content)
            } else schedule
        }
        _uiState.update { it.copy(reviewSchedules = refreshed) }
    }

    private fun loadReviewBlocks(uid: String) {
        viewModelScope.launch {
            reviewBlockRepository.getReviewBlocks(uid)
                .onSuccess { blocks: List<ReviewBlock> ->
                    val today = LocalDate.now()
                    val uiBlocks = blocks.map { block: ReviewBlock ->
                        val examLocalDate = Instant.ofEpochMilli(block.examDate)
                            .atZone(seoulZone).toLocalDate()
                        val dDay = ChronoUnit.DAYS.between(today, examLocalDate).toInt()
                        ReviewBlockUiModel(
                            blockId = block.blockId,
                            examName = block.examName.ifBlank { block.title },
                            dDay = dDay,
                            completionRate = 0f,
                            examDateMillis = block.examDate,
                            prepStartDateMillis = block.prepStartDate,
                            dailyCap = block.dailyCap
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
        viewModelScope.launch {
            val uid = getUidSafely()
            if (uid.isNullOrBlank()) {
                _uiState.update { it.copy(saveMessage = "로그인 정보가 없습니다.") }
                return@launch
            }
            _uiState.update { it.copy(isCreatingBlock = true, errorMessage = null) }
            val block = ReviewBlock(
                blockId = "",
                uid = uid,
                examName = examName,
                title = examName,
                examDate = examDateMillis,
                prepStartDate = prepStartDateMillis,
                dailyCap = dailyCap,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            reviewBlockRepository.saveReviewBlock(block)
                .onSuccess {
                    _uiState.update { it.copy(isCreatingBlock = false, saveMessage = "복습 블록이 생성되었습니다.") }
                    loadReviewBlocks(uid)
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isCreatingBlock = false, errorMessage = e.message ?: "생성에 실패했습니다.")
                    }
                }
        }
    }

    fun addProgress(examId: String, content: String, completedCount: Int, totalCount: Int) {
        viewModelScope.launch {
            val uid = getUidSafely()
            if (uid.isNullOrBlank()) {
                _uiState.update {
                    it.copy(
                        saveMessage = "로그인 정보가 없습니다. 다시 로그인해 주세요.",
                        errorMessage = "로그인 정보가 없습니다."
                    )
                }
                return@launch
            }
            _uiState.update { it.copy(isSaving = true, saveMessage = null, errorMessage = null) }

            val progress = Progress(
                progressId = UUID.randomUUID().toString(),
                examId = examId,
                content = content.trim(),
                completedCount = completedCount,
                totalCount = totalCount,
                isCompleted = totalCount > 0 && completedCount >= totalCount,
                createdAt = System.currentTimeMillis()
            )

            val result = saveProgressAndScheduleUseCase(uid, progress)

            if (result.isSuccess) {
                _uiState.update { it.copy(isSaving = false, saveMessage = "학습 진도가 저장되었습니다.") }
                // ✅ 진도 + 복습 일정 모두 최신 상태로 갱신 (캘린더 도트 즉시 반영)
                loadProgressListAndThenSchedules(uid, _displayYearMonth.value)
                // ✅ 현재 월 외에 다음 달 일정도 캘린더에 즉시 반영되도록 추가 트리거
                loadReviewScheduleDatesByMonth(uid, _displayYearMonth.value)
            } else {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        saveMessage = result.exceptionOrNull()?.message ?: "저장하지 못했습니다.",
                        errorMessage = result.exceptionOrNull()?.message
                    )
                }
            }
        }
    }

    // ✅ 복습 로그 생성 또는 완료 처리 후 캘린더 강제 갱신
    fun refreshCalendar() {
        viewModelScope.launch {
            val uid = getUidSafely() ?: return@launch
            loadReviewScheduleDatesByMonth(uid, _displayYearMonth.value)
        }
    }

    fun clearSaveMessage() = _uiState.update { it.copy(saveMessage = null) }
    fun consumeErrorMessage() = _uiState.update { it.copy(errorMessage = null) }
}