package com.loorve.presentation.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.loorve.data.notification.ReviewAlarmScheduler
import com.loorve.domain.model.Progress
import com.loorve.domain.model.ReviewSchedule
import com.loorve.domain.repository.ProgressRepository
import com.loorve.domain.repository.ReviewScheduleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProgressDetailUiState(
    val isLoading: Boolean = false,
    val progress: Progress? = null,
    val errorMessage: String? = null,
    val isDeleted: Boolean = false
)

@HiltViewModel
class ProgressDetailViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val progressRepository: ProgressRepository,
    private val reviewScheduleRepository: ReviewScheduleRepository,
    private val reviewAlarmScheduler: ReviewAlarmScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProgressDetailUiState())
    val uiState: StateFlow<ProgressDetailUiState> = _uiState.asStateFlow()

    fun loadProgress(progressId: String) {
        val uid = firebaseAuth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            _uiState.value = ProgressDetailUiState(
                errorMessage = "로그인 정보를 찾을 수 없습니다."
            )
            return
        }

        if (progressId.isBlank()) {
            _uiState.value = ProgressDetailUiState(
                errorMessage = "진도 ID가 올바르지 않습니다."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            progressRepository.getProgressById(uid, progressId)
                .onSuccess { progress ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        progress = progress
                    )
                }
                .onFailure { throwable ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = throwable.message ?: "진도를 불러오지 못했습니다."
                    )
                }
        }
    }

    fun saveProgressWithReviewSchedules(
        progress: Progress,
        reviewDates: List<Long>
    ) {
        val uid = firebaseAuth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "로그인 정보를 찾을 수 없습니다."
            )
            return
        }

        if (progress.progressId.isBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "진도 ID가 올바르지 않습니다."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            val progressResult = progressRepository.saveProgress(
                uid = uid,
                progress = progress
            )

            if (progressResult.isFailure) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = progressResult.exceptionOrNull()?.message
                        ?: "진도 저장에 실패했습니다."
                )
                return@launch
            }

            val now = System.currentTimeMillis()

            reviewDates.forEachIndexed { index, reviewDate ->
                val schedule = ReviewSchedule(
                    scheduleId = UUID.randomUUID().toString(),
                    blockId = "",
                    userId = uid,
                    originProgressId = progress.progressId,
                    title = progress.content,  // ← 'title' → 'content' 로 수정
                    reviewDate = reviewDate,
                    reviewDateText = "",
                    reviewOrder = index + 1,
                    scheduleType = "EBBINGHAUS",
                    isCompleted = false,
                    createdAt = now,
                    updatedAt = now
                )

                val scheduleResult = reviewScheduleRepository.saveReviewSchedule(
                    reviewSchedule = schedule
                )

                if (scheduleResult.isSuccess) {
                    reviewAlarmScheduler.scheduleReviewAlarm(
                        reviewScheduleId = schedule.scheduleId,
                        triggerAtMillis = schedule.reviewDate
                    )
                }
            }

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                progress = progress
            )
        }
    }

    fun deleteProgress(progressId: String) {
        val uid = firebaseAuth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "로그인 정보를 찾을 수 없습니다."
            )
            return
        }

        if (progressId.isBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "진도 ID가 올바르지 않습니다."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            /*
             * 1. 이 진도와 연결된 복습 일정 조회
             * 2. 연결된 알람 취소
             * 3. Firestore 복습 일정 삭제
             * 4. 마지막으로 원본 진도 삭제
             *
             * 진도 삭제에 성공했는데 일정만 남는 고아 데이터를 줄이기 위해
             * 복습 일정 정리를 먼저 수행한다.
             */
            val schedulesResult = reviewScheduleRepository
                .getReviewSchedulesByProgressId(
                    uid = uid,
                    progressId = progressId
                )

            if (schedulesResult.isFailure) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = schedulesResult.exceptionOrNull()?.message
                        ?: "연결된 복습 일정을 조회하지 못했습니다."
                )
                return@launch
            }

            val schedules = schedulesResult.getOrDefault(emptyList())

            schedules.forEach { schedule ->
                reviewAlarmScheduler.cancelReviewAlarm(schedule.scheduleId)

                val deleteScheduleResult = reviewScheduleRepository
                    .deleteReviewSchedule(
                        uid = uid,
                        scheduleId = schedule.scheduleId
                    )

                if (deleteScheduleResult.isFailure) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = deleteScheduleResult.exceptionOrNull()?.message
                            ?: "연결된 복습 일정 삭제에 실패했습니다."
                    )
                    return@launch
                }
            }

            val deleteProgressResult = progressRepository.deleteProgress(
                uid = uid,
                progressId = progressId
            )

            deleteProgressResult
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        progress = null,
                        isDeleted = true
                    )
                }
                .onFailure { throwable ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = throwable.message ?: "진도 삭제에 실패했습니다."
                    )
                }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun consumeDeletionEvent() {
        _uiState.value = _uiState.value.copy(isDeleted = false)
    }
}