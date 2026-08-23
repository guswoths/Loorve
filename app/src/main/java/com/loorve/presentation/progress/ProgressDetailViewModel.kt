package com.loorve.presentation.progress

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loorve.data.notification.ReviewAlarmScheduler
import com.loorve.domain.model.Progress
import com.loorve.domain.model.ReviewSchedule
import com.loorve.domain.repository.ProgressRepository
import com.loorve.domain.repository.ReviewScheduleRepository
import com.loorve.domain.usecase.ForgettingCurveScheduler
import com.loorve.domain.usecase.SaveProgressAndScheduleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject

// ✅ 신규 추가: 복습 스케줄 설정 모드
enum class ReviewScheduleMode {
    FORGETTING_CURVE,  // 망각곡선 자동 설정 (D+1, D+3, D+7, D+14, D+30)
    MANUAL             // 직접 입력
}

data class ProgressDetailUiState(
    val progress: Progress? = null,
    val isLoading: Boolean = true,
    val isEditMode: Boolean = false,
    val errorMessage: String? = null,
    val saveResult: Boolean? = null,
    val deleteResult: Boolean? = null,
    val isDirty: Boolean = false,
    val initialSnapshot: Progress? = null,
    val generatedReviewDates: List<java.time.LocalDate> = emptyList(),
    // ✅ 신규 추가
    val reviewScheduleMode: ReviewScheduleMode = ReviewScheduleMode.FORGETTING_CURVE,
    val manualReviewDateTime: Long? = null,   // epoch ms, 직접 입력 시 사용
    val successMessage: String? = null        // Snackbar 메시지 분기용
)

@HiltViewModel
class ProgressDetailViewModel @Inject constructor(
    private val progressRepository: ProgressRepository,
    private val saveProgressAndScheduleUseCase: SaveProgressAndScheduleUseCase,
    private val reviewScheduleRepository: ReviewScheduleRepository,
    private val alarmScheduler: ReviewAlarmScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProgressDetailUiState())
    val uiState: StateFlow<ProgressDetailUiState> = _uiState.asStateFlow()

    fun loadProgress(uid: String, progressId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = progressRepository.getProgressById(uid, progressId)
            if (result.isSuccess) {
                _uiState.update { it.copy(isLoading = false, progress = result.getOrNull()) }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading    = false,
                        errorMessage = result.exceptionOrNull()?.message ?: "불러오지 못했습니다."
                    )
                }
            }
        }
    }

    fun enterEditMode() {
        _uiState.update {
            it.copy(
                isEditMode      = true,
                initialSnapshot = it.progress,
                isDirty         = false
            )
        }
    }

    fun exitEditMode() {
        _uiState.update {
            it.copy(
                isEditMode           = false,
                isDirty              = false,
                initialSnapshot      = null,
                reviewScheduleMode   = ReviewScheduleMode.FORGETTING_CURVE,
                manualReviewDateTime = null
            )
        }
    }

    // ✅ 신규 추가: 복습 스케줄 모드 변경
    fun onReviewScheduleModeChanged(mode: ReviewScheduleMode) {
        _uiState.update { it.copy(reviewScheduleMode = mode, manualReviewDateTime = null) }
    }

    // ✅ 신규 추가: 직접 입력 일시 변경
    fun onManualReviewDateTimeChanged(epochMs: Long) {
        _uiState.update { it.copy(manualReviewDateTime = epochMs) }
    }

    fun saveProgress(uid: String, updatedProgress: Progress) {
        val current = _uiState.value.progress ?: run {
            Log.w(TAG, "saveProgress 호출됐지만 현재 progress 상태가 null입니다.")
            _uiState.update { it.copy(saveResult = false, errorMessage = "저장할 데이터가 없습니다.") }
            return
        }

        if (uid.isBlank()) {
            Log.e(TAG, "saveProgress 실패: uid가 비어 있습니다.")
            _uiState.update { it.copy(saveResult = false, errorMessage = "로그인 정보가 없습니다. 다시 로그인해 주세요.") }
            return
        }

        if (updatedProgress.content.isBlank()) {
            Log.w(TAG, "saveProgress 실패: content가 비어 있습니다.")
            _uiState.update { it.copy(saveResult = false, errorMessage = "학습 내용을 입력해주세요.") }
            return
        }

        val mode = _uiState.value.reviewScheduleMode

        // ✅ 보안: MANUAL 모드일 때 과거 시각 차단
        if (mode == ReviewScheduleMode.MANUAL) {
            val manualMs = _uiState.value.manualReviewDateTime
            if (manualMs == null) {
                _uiState.update { it.copy(saveResult = false, errorMessage = "복습 예정 일시를 선택해주세요.") }
                return
            }
            if (manualMs <= System.currentTimeMillis()) {
                _uiState.update { it.copy(saveResult = false, errorMessage = "복습 예정 시각은 현재 시각 이후여야 합니다.") }
                return
            }
        }

        val merged = current.copy(
            content        = updatedProgress.content,
            completedCount = updatedProgress.completedCount,
            totalCount     = updatedProgress.totalCount,
            isCompleted    = updatedProgress.isCompleted
        )

        viewModelScope.launch {
            when (mode) {
                // ─── 기존 로직 유지 ───
                ReviewScheduleMode.FORGETTING_CURVE -> {
                    val result = saveProgressAndScheduleUseCase(uid, merged)
                    if (result.isSuccess) {
                        val progressDate = Instant.ofEpochMilli(
                            if (merged.createdAt > 0L) merged.createdAt else System.currentTimeMillis()
                        ).atZone(ZoneId.of("Asia/Seoul")).toLocalDate()
                        ForgettingCurveScheduler.generateReviewDates(progressDate)

                        _uiState.update {
                            it.copy(
                                progress        = merged,
                                isLoading       = false,
                                isEditMode      = false,
                                saveResult      = true,
                                successMessage  = "복습 일정 5개가 생성되었습니다 📅",
                                errorMessage    = null,
                                isDirty         = false,
                                initialSnapshot = null
                            )
                        }
                    } else {
                        val errMsg = result.exceptionOrNull()?.message ?: "저장 중 알 수 없는 오류가 발생했습니다."
                        Log.e(TAG, "saveProgress 실패: $errMsg")
                        _uiState.update {
                            it.copy(
                                isLoading    = false,
                                saveResult   = false,
                                errorMessage = errMsg
                            )
                        }
                    }
                }

                // ─── 신규: 직접 입력 모드 ───
                ReviewScheduleMode.MANUAL -> {
                    val manualMs = _uiState.value.manualReviewDateTime!!
                    runCatching {
                        // ① Progress 저장
                        val progressResult = progressRepository.saveProgress(uid, merged)
                        if (progressResult.isFailure) throw progressResult.exceptionOrNull()!!

                        // ② ReviewSchedule 1개 생성 및 저장
                        val scheduleId = UUID.randomUUID().toString()
                        val now = System.currentTimeMillis()
                        val schedule = ReviewSchedule(
                            reviewScheduleId = scheduleId,
                            originProgressId = merged.progressId,  // ✅ 실제 필드명 사용
                            reviewDate       = manualMs,
                            reviewRound      = 1,
                            isCompleted      = false,
                            createdAt        = now,
                            updatedAt        = now,
                            scheduleType     = "MANUAL"
                        )
                        val scheduleResult = reviewScheduleRepository.saveReviewSchedule(uid, schedule)
                        if (scheduleResult.isFailure) throw scheduleResult.exceptionOrNull()!!

                        // ③ 알람 등록
                        alarmScheduler.scheduleReviewAlarm(scheduleId, manualMs)
                    }.onSuccess {
                        val displayTime = Instant.ofEpochMilli(manualMs)
                            .atZone(ZoneId.of("Asia/Seoul"))
                            .format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH:mm"))
                        val msg = "복습 알람이 설정되었습니다 ⏰ ($displayTime)"

                        _uiState.update {
                            it.copy(
                                progress             = merged,
                                isLoading            = false,
                                isEditMode           = false,
                                saveResult           = true,
                                successMessage       = msg,
                                errorMessage         = null,
                                isDirty              = false,
                                initialSnapshot      = null,
                                reviewScheduleMode   = ReviewScheduleMode.FORGETTING_CURVE,
                                manualReviewDateTime = null
                            )
                        }
                    }.onFailure { e ->
                        Log.e(TAG, "saveProgress(MANUAL) 실패: ${e.message}")
                        _uiState.update {
                            it.copy(
                                isLoading    = false,
                                saveResult   = false,
                                errorMessage = e.message ?: "저장 중 오류가 발생했습니다."
                            )
                        }
                    }
                }
            }
        }
    }

    fun deleteProgress(uid: String, progressId: String) {
        if (uid.isBlank() || progressId.isBlank()) {
            _uiState.update { it.copy(deleteResult = false) }
            return
        }
        viewModelScope.launch {
            val schedulesResult = reviewScheduleRepository
                .getReviewSchedulesByProgressId(uid, progressId)
            if (schedulesResult.isSuccess) {
                schedulesResult.getOrNull()?.forEach { schedule ->
                    alarmScheduler.cancelReviewAlarm(schedule.reviewScheduleId)
                }
            } else {
                Log.w(TAG, "deleteProgress 알람 취소 실패 — 스케줄 조회 오류: " +
                        "${schedulesResult.exceptionOrNull()?.message}")
            }

            val result = progressRepository.deleteProgress(uid, progressId)
            _uiState.update { it.copy(deleteResult = result.isSuccess) }
        }
    }

    fun consumeSaveResult() {
        _uiState.update { it.copy(saveResult = null, successMessage = null) }
    }

    fun consumeDeleteResult() {
        _uiState.update { it.copy(deleteResult = null) }
    }

    fun onEditChanged(
        content: String,
        completedCount: String,
        totalCount: String,
        isCompleted: Boolean
    ) {
        val snap = _uiState.value.initialSnapshot ?: return
        val dirty = snap.content != content ||
                snap.completedCount.toString() != completedCount ||
                snap.totalCount.toString() != totalCount ||
                snap.isCompleted != isCompleted
        _uiState.update { it.copy(isDirty = dirty) }
    }

    companion object {
        private const val TAG = "ProgressDetailViewModel"
    }
}