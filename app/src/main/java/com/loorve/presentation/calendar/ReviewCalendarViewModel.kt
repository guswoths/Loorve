// 경로: app/src/main/java/com/loorve/presentation/calendar/ReviewCalendarViewModel.kt
package com.loorve.presentation.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.loorve.domain.model.ReviewSchedule
import com.loorve.domain.repository.ReviewScheduleRepository
import com.loorve.domain.repository.ReviewScheduleItemRepository
import com.loorve.domain.review.DailyReviewCompletionStat
import com.loorve.domain.review.ReviewCompletionSchedule
import com.loorve.domain.review.buildRecentReviewCompletionStats
import com.loorve.domain.usecase.UpdateReviewCompletionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import com.loorve.domain.model.ReviewBlock
import com.loorve.domain.repository.ReviewBlockRepository
import com.loorve.domain.subscription.ReviewBlockAccessPolicy
import com.loorve.domain.subscription.SubscriptionEntitlement
import com.loorve.domain.subscription.SubscriptionRepository
import com.loorve.util.CalendarRefreshBus

data class ReviewCalendarUiState(
    val displayYearMonth: YearMonth = YearMonth.now(),
    val schedulesMap: Map<LocalDate, List<ReviewSchedule>> = emptyMap(),
    val selectedDate: LocalDate? = null,
    val selectedDateSchedules: List<ReviewSchedule> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val reviewBlocks: List<ReviewBlock> = emptyList(),
    val isBlocksLoading: Boolean = false,
    val selectedBlock: ReviewBlock? = null,   // 클릭된 블록 (바텀시트용)
    val showBlockDetail: Boolean = false,      // 바텀시트 표시 여부
    val isDeleting: Boolean = false,            // 삭제 진행 중 여부
    val completionStats: List<DailyReviewCompletionStat> = emptyList(),
    val selectedCompletionStat: DailyReviewCompletionStat? = null,
    val isCompletionStatsLoading: Boolean = false,
    val lockedBlockIds: Set<String> = emptySet(),
    val subscriptionEntitlement: SubscriptionEntitlement = SubscriptionEntitlement.Loading,
    val delayedBlockIds: Set<String> = emptySet()
)

@HiltViewModel
class ReviewCalendarViewModel @Inject constructor(
    private val reviewScheduleRepository: ReviewScheduleRepository,
    private val reviewScheduleItemRepository: ReviewScheduleItemRepository,
    private val updateReviewCompletionUseCase: UpdateReviewCompletionUseCase,
    private val reviewBlockRepository: ReviewBlockRepository,
    private val calendarRefreshBus: CalendarRefreshBus,
    private val subscriptionRepository: SubscriptionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReviewCalendarUiState())
    val uiState: StateFlow<ReviewCalendarUiState> = _uiState.asStateFlow()

    private val _currentUid = MutableStateFlow<String?>(null)
    val currentUid: StateFlow<String?> = _currentUid.asStateFlow()

    private val _isUidReady = MutableStateFlow(false)
    val isUidReady: StateFlow<Boolean> = _isUidReady.asStateFlow()

    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val seoulZone = ZoneId.of("Asia/Seoul")
    private var loadJob: Job? = null
    private var recentBlocksJob: Job? = null
    private var recentLegacyJob: Job? = null
    private var recentItemsJob: Job? = null
    private var recentLegacySchedules: List<ReviewCompletionSchedule> = emptyList()
    private var recentScheduleItems: List<ReviewCompletionSchedule> = emptyList()
    private var recentActiveBlockIds: Set<String> = emptySet()
    private var recentBlocksLoaded = false
    private var recentLegacyLoaded = false
    private var recentItemsLoaded = false

    init {
        viewModelScope.launch {
            subscriptionRepository.state.collect { subscriptionState ->
                _uiState.update { state ->
                    val accessible = ReviewBlockAccessPolicy.accessibleBlockIds(
                        state.reviewBlocks,
                        subscriptionState.entitlement
                    )
                    state.copy(
                        subscriptionEntitlement = subscriptionState.entitlement,
                        lockedBlockIds = state.reviewBlocks
                            .mapNotNull { it.blockId.takeIf { id -> id !in accessible } }
                            .toSet()
                    )
                }
            }
        }
    }

    // ✅ init 블록 제거 — Screen의 LaunchedEffect에서 suspend refreshUid() 호출로 통일

    /**
     * suspend fun으로 변경하여 호출부(LaunchedEffect)에서 완료를 기다릴 수 있도록 함.
     * 토큰 갱신 실패 시 캐시 uid 폴백으로 네트워크 오류와 로그인 오류를 구분.
     */
    // ✅ AFTER — refreshUid() 내부에서 직접 스케줄 로드까지 완료
    suspend fun refreshUid() {
        loadJob?.cancel()
        recentBlocksJob?.cancel()
        recentLegacyJob?.cancel()
        recentItemsJob?.cancel()
        recentLegacySchedules = emptyList()
        recentScheduleItems = emptyList()
        recentActiveBlockIds = emptySet()
        recentBlocksLoaded = false
        recentLegacyLoaded = false
        recentItemsLoaded = false
        _uiState.update { state ->
            state.copy(
                completionStats = emptyList(),
                selectedCompletionStat = null,
                isCompletionStatsLoading = true
            )
        }
        _isUidReady.value = false
        val user = FirebaseAuth.getInstance().currentUser
        val uid = runCatching {
            user?.getIdToken(true)?.await()
            user?.uid
        }.getOrElse {
            user?.uid
        }
        _currentUid.value = uid
        _isUidReady.value = (uid != null)
        if (!uid.isNullOrBlank()) {
            loadReviewBlocks(uid)              // 기존 유지
            // ✅ 핵심 추가: uid 세팅 직후 스케줄 로드
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            loadSchedulesForMonth(_uiState.value.displayYearMonth)
            observeRecentCompletionSchedules(uid)
        } else {
            _uiState.update { it.copy(isCompletionStatsLoading = false) }
        }
    }

    fun onMonthChanged(yearMonth: YearMonth) {
        _uiState.update { it.copy(displayYearMonth = yearMonth, isLoading = true, errorMessage = null) }
        loadSchedulesForMonth(yearMonth)
    }

    fun onDateSelected(date: LocalDate) {
        val schedules = _uiState.value.schedulesMap[date] ?: emptyList()
        _uiState.update { currentState ->
            currentState.copy(
                selectedDate = date,
                selectedDateSchedules = schedules
            )
        }
    }

    fun onCompletionStatSelected(stat: DailyReviewCompletionStat) {
        _uiState.update { it.copy(selectedCompletionStat = stat) }
    }

    fun onCompleteSchedule(scheduleId: String) {
        val uid = _currentUid.value ?: return
        viewModelScope.launch {
            val result = reviewScheduleRepository.completeReviewSchedule(uid, scheduleId)
            if (result.isFailure) {
                _uiState.update {
                    it.copy(errorMessage = result.exceptionOrNull()?.message ?: "복습 완료 처리에 실패했습니다.")
                }
            }
        }
    }

    fun toggleReviewCompletion(scheduleId: String, currentState: Boolean) {
        val uid = _currentUid.value ?: return
        val updatedState = !currentState
        val previousSchedules = _uiState.value.selectedDateSchedules
        val scheduleKey = scheduleId.ifBlank { return }
        _uiState.update { state ->
            val updatedSchedules = state.selectedDateSchedules.map { schedule ->
                val key = schedule.scheduleId.ifBlank {
                    "${schedule.reviewDate}_${schedule.blockId}_${schedule.reviewOrder}"
                }
                if (key == scheduleKey) {
                    schedule.copy(isCompleted = updatedState)
                } else {
                    schedule
                }
            }
            state.copy(
                selectedDateSchedules = updatedSchedules,
                schedulesMap = state.schedulesMap.mapValues { (date, schedules) ->
                    if (date == state.selectedDate) {
                        schedules.map { schedule ->
                            val key = schedule.scheduleId.ifBlank {
                                "${schedule.reviewDate}_${schedule.blockId}_${schedule.reviewOrder}"
                            }
                            if (key == scheduleKey) {
                                schedule.copy(isCompleted = updatedState)
                            } else {
                                schedule
                            }
                        }
                    } else {
                        schedules
                    }
                }
            )
        }
        viewModelScope.launch {
            val itemResult = reviewScheduleItemRepository.updateScheduleCompletion(
                uid = uid,
                scheduleId = scheduleKey,
                isCompleted = updatedState
            )
            val result = if (itemResult.isSuccess) {
                itemResult
            } else {
                updateReviewCompletionUseCase(
                    uid = uid,
                    scheduleId = scheduleKey,
                    isCompleted = updatedState
                )
            }
            if (result.isFailure) {
                Log.e(
                    "ReviewCalendarViewModel",
                    "복습 상태 저장 실패: ${result.exceptionOrNull()?.message}"
                )
                _uiState.update {
                    it.copy(
                        selectedDateSchedules = previousSchedules,
                    )
                }
            } else {
                calendarRefreshBus.notifyRefresh()
            }
        }
    }

    fun onDismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun loadCurrentMonth() {
        loadSchedulesForMonth(_uiState.value.displayYearMonth)
    }

    fun reloadCurrentMonth() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        loadSchedulesForMonth(_uiState.value.displayYearMonth)
    }

    fun loadReviewBlocks(uid: String) {
        if (uid.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isBlocksLoading = true) }
            reviewBlockRepository.getReviewBlocks(uid)
                .onSuccess { blocks ->
                    _uiState.update { currentState ->
                        currentState.copy(
                            reviewBlocks = blocks.sortedBy { it.createdAt },
                            isBlocksLoading = false,
                            lockedBlockIds = blocks
                                .mapNotNull { block ->
                                    block.blockId.takeIf {
                                        it !in ReviewBlockAccessPolicy.accessibleBlockIds(
                                            blocks,
                                            currentState.subscriptionEntitlement
                                        )
                                    }
                                }
                                .toSet()
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isBlocksLoading = false,
                            errorMessage = e.message ?: "복습 블록을 불러오지 못했습니다."
                        )
                    }
                }

        }
    }

    fun onBlockClicked(block: ReviewBlock) {
        _uiState.update { it.copy(selectedBlock = block, showBlockDetail = true) }
    }

    fun onDismissBlockDetail() {
        _uiState.update { it.copy(selectedBlock = null, showBlockDetail = false) }
    }

    fun deleteReviewBlock(blockId: String) {
        val uid = _currentUid.value ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true) }
            reviewBlockRepository.deleteReviewBlock(uid, blockId)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isDeleting = false,
                            showBlockDetail = false,
                            selectedBlock = null
                        )
                    }
                    loadReviewBlocks(uid)
                    calendarRefreshBus.notifyRefresh()
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isDeleting = false,
                            errorMessage = e.message ?: "삭제에 실패했습니다."
                        )
                    }
                }
        }
    }

    private fun loadSchedulesForMonth(yearMonth: YearMonth) {
        val uid = _currentUid.value
        if (uid.isNullOrBlank()) {
            _uiState.update { it.copy(isLoading = false, errorMessage = null) }
            return
        }

        val startDate = yearMonth.atDay(1).format(dateFormatter)
        val endDate = yearMonth.atEndOfMonth().format(dateFormatter)

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            reviewScheduleRepository
                .getReviewSchedulesByDateRange(uid, startDate, endDate)
                .catch { exception ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = exception.message ?: "일정을 불러오지 못했습니다.")
                    }
                }
                .collectLatest { schedules ->
                    val schedulesMap = schedules.groupBy { schedule ->
                        java.time.Instant.ofEpochMilli(schedule.reviewDate)
                            .atZone(java.time.ZoneId.of("Asia/Seoul"))
                            .toLocalDate()
                    }
                    val selectedDate = _uiState.value.selectedDate
                    val selectedDateSchedules = if (selectedDate != null) schedulesMap[selectedDate] ?: emptyList() else emptyList()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            schedulesMap = schedulesMap,
                            selectedDateSchedules = selectedDateSchedules
                        )
                    }
                }
        }
    }

    private fun observeRecentCompletionSchedules(uid: String) {
        val today = LocalDate.now(seoulZone)
        val startDate = today.minusDays(6).format(dateFormatter)
        val endDate = today.format(dateFormatter)
        _uiState.update { it.copy(isCompletionStatsLoading = true) }

        recentBlocksJob = viewModelScope.launch {
            reviewBlockRepository
                .observeReviewBlocks(uid)
                .catch { exception ->
                    recentBlocksLoaded = true
                    recentActiveBlockIds = emptySet()
                    updateCompletionStats(today)
                    _uiState.update {
                        it.copy(errorMessage = exception.message ?: "복습 블록을 불러오지 못했습니다.")
                    }
                }
                .collectLatest { blocks ->
                    recentBlocksLoaded = true
                    recentActiveBlockIds = blocks
                        .map { it.blockId }
                        .filter { it.isNotBlank() }
                        .toSet()
                    updateCompletionStats(today)
                }
        }

        recentLegacyJob = viewModelScope.launch {
            reviewScheduleRepository
                .getReviewSchedulesByDateRange(uid, startDate, endDate)
                .catch { exception ->
                    recentLegacySchedules = emptyList()
                    recentLegacyLoaded = true
                    updateCompletionStats(today)
                    _uiState.update {
                        it.copy(
                            errorMessage = exception.message ?: "복습 통계를 불러오지 못했습니다."
                        )
                    }
                }
                .collectLatest { schedules ->
                    recentLegacyLoaded = true
                    recentLegacySchedules = schedules.map { schedule ->
                        ReviewCompletionSchedule(
                            id = schedule.scheduleId,
                            dueDate = java.time.Instant.ofEpochMilli(schedule.reviewDate)
                                .atZone(seoulZone)
                                .toLocalDate(),
                            isCompleted = schedule.isCompleted,
                            sourceId = schedule.originProgressId.ifBlank { schedule.blockId },
                            reviewOrder = schedule.reviewOrder,
                            blockId = schedule.blockId
                        )
                    }
                    updateCompletionStats(today)
                }
        }

        recentItemsJob = viewModelScope.launch {
            reviewScheduleItemRepository
                .observeReviewScheduleItems(uid)
                .catch { exception ->
                    recentScheduleItems = emptyList()
                    recentItemsLoaded = true
                    updateCompletionStats(today)
                    _uiState.update {
                        it.copy(
                            errorMessage = exception.message ?: "복습 통계를 불러오지 못했습니다."
                        )
                    }
                }
                .collectLatest { items ->
                    recentItemsLoaded = true
                    val delayedItemBlockIds = items
                        .filter {
                            it.blockId.isNotBlank() &&
                                it.reviewDate < today.atStartOfDay(seoulZone)
                                    .toInstant()
                                    .toEpochMilli() &&
                                it.status != com.loorve.domain.model.ReviewStatus.COMPLETED
                        }
                        .map { it.blockId }
                        .toSet()
                    recentScheduleItems = items.mapNotNull { item ->
                        val dueDate = runCatching {
                            java.time.Instant.ofEpochMilli(item.reviewDate)
                                .atZone(seoulZone)
                                .toLocalDate()
                        }.getOrNull() ?: return@mapNotNull null
                        if (dueDate !in today.minusDays(6)..today) return@mapNotNull null
                        ReviewCompletionSchedule(
                            id = item.id,
                            dueDate = dueDate,
                            isCompleted = item.status == com.loorve.domain.model.ReviewStatus.COMPLETED,
                            sourceId = item.studyRecordId.ifBlank { item.blockId },
                            reviewOrder = item.reviewOrder,
                            blockId = item.blockId
                        )
                    }
                    _uiState.update { it.copy(delayedBlockIds = delayedItemBlockIds) }
                    updateCompletionStats(today)
                }
        }
    }

    private fun updateCompletionStats(today: LocalDate) {
        val activeLegacySchedules = if (!recentBlocksLoaded) {
            recentLegacySchedules
        } else {
            recentLegacySchedules.filter { schedule ->
                schedule.blockId.isBlank() || schedule.blockId in recentActiveBlockIds
            }
        }
        val activeScheduleItems = if (!recentBlocksLoaded) {
            recentScheduleItems
        } else {
            recentScheduleItems.filter { schedule ->
                schedule.blockId.isBlank() || schedule.blockId in recentActiveBlockIds
            }
        }
        val schedules = (activeLegacySchedules + activeScheduleItems)
            .distinctBy { Triple(it.dueDate, it.sourceId, it.reviewOrder) }
        val stats = buildRecentReviewCompletionStats(schedules, today)
        val selectedDate = _uiState.value.selectedCompletionStat?.date ?: today
        _uiState.update {
            it.copy(
                completionStats = stats,
                selectedCompletionStat = stats.firstOrNull { stat -> stat.date == selectedDate }
                    ?: stats.lastOrNull(),
                isCompletionStatsLoading = !(recentLegacyLoaded && recentItemsLoaded)
            )
        }
    }
}