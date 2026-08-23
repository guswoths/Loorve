// AFTER — 전체 코드
package com.loorve.presentation.progress

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loorve.data.notification.ReviewAlarmScheduler
import com.loorve.domain.model.Progress
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
import javax.inject.Inject

data class ProgressDetailUiState(
    val progress: Progress? = null,
    val isLoading: Boolean = true,
    val isEditMode: Boolean = false,
    val errorMessage: String? = null,
    val saveResult: Boolean? = null,
    val deleteResult: Boolean? = null,
    val isDirty: Boolean = false,
    val initialSnapshot: Progress? = null,
    val generatedReviewDates: List<java.time.LocalDate> = emptyList()
)

@HiltViewModel
class ProgressDetailViewModel @Inject constructor(
    private val progressRepository: ProgressRepository,
    private val saveProgressAndScheduleUseCase: SaveProgressAndScheduleUseCase,
    private val reviewScheduleRepository: ReviewScheduleRepository,  // ← 추가
    private val alarmScheduler: ReviewAlarmScheduler                 // ← 추가
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
                isEditMode      = false,
                isDirty         = false,
                initialSnapshot = null
            )
        }
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

        val merged = current.copy(
            content        = updatedProgress.content,
            completedCount = updatedProgress.completedCount,
            totalCount     = updatedProgress.totalCount,
            isCompleted    = updatedProgress.isCompleted
        )

        viewModelScope.launch {
            val result = saveProgressAndScheduleUseCase(uid, merged)
            if (result.isSuccess) {
                val progressDate = java.time.Instant.ofEpochMilli(
                    if (merged.createdAt > 0L) merged.createdAt else System.currentTimeMillis()
                ).atZone(java.time.ZoneId.of("Asia/Seoul")).toLocalDate()
                ForgettingCurveScheduler.generateReviewDates(progressDate) // UI 미리보기용

                _uiState.update {
                    it.copy(
                        progress        = merged,
                        isLoading       = false,
                        isEditMode      = false,
                        saveResult      = true,
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
    }

    fun deleteProgress(uid: String, progressId: String) {
        if (uid.isBlank() || progressId.isBlank()) {
            _uiState.update { it.copy(deleteResult = false) }
            return
        }
        viewModelScope.launch {
            // ✅ 연결된 복습 알람 일괄 취소 (UI 응답성을 위해 비동기 처리)
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

            // Progress 삭제
            val result = progressRepository.deleteProgress(uid, progressId)
            _uiState.update { it.copy(deleteResult = result.isSuccess) }
        }
    }

    fun consumeSaveResult() {
        _uiState.update { it.copy(saveResult = null) }
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