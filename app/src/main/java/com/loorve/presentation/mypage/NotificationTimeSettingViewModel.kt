package com.loorve.presentation.mypage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.loorve.data.notification.ReviewAlarmScheduler
import com.loorve.data.local.NotificationTimePreferences
import com.loorve.domain.model.ReviewStatus
import com.loorve.domain.repository.ReviewScheduleItemRepository
import com.loorve.domain.review.alarmTriggerAtMillis
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

data class NotificationTimeUiState(
    val hour: Int = 9,
    val minute: Int = 0,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,      // 저장 완료 one-shot 이벤트용
    val errorMessage: String? = null   // 저장 실패 시 one-shot 에러 이벤트용
)

@HiltViewModel
class NotificationTimeSettingViewModel @Inject constructor(
    private val notificationTimePreferences: NotificationTimePreferences,
    private val scheduleRepository: ReviewScheduleItemRepository,
    private val firebaseAuth: FirebaseAuth,
    private val reviewAlarmScheduler: ReviewAlarmScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationTimeUiState())
    val uiState: StateFlow<NotificationTimeUiState> = _uiState.asStateFlow()

    init {
        // DataStore에서 저장된 알림 시간을 수집하여 초기 상태 업데이트
        viewModelScope.launch {
            notificationTimePreferences.notificationTime.collect { (hour, minute) ->
                _uiState.update { current ->
                    // isSaved / errorMessage 플래그는 건드리지 않고 hour/minute만 동기화
                    current.copy(hour = hour, minute = minute)
                }
            }
        }
    }

    /** 시(hour) 선택 변경 — 아직 저장하지 않은 임시 상태 */
    fun onHourChanged(hour: Int) {
        _uiState.update { it.copy(hour = hour) }
    }

    /** 분(minute) 선택 변경 — 아직 저장하지 않은 임시 상태 */
    fun onMinuteChanged(minute: Int) {
        _uiState.update { it.copy(minute = minute) }
    }

    /**
     * DataStore에 현재 선택된 알림 시간 저장.
     * 성공 시 isSaved = true, 실패 시 errorMessage 세팅 (IOException 등 예외 방어)
     */
    fun saveNotificationTime() {
        if (_uiState.value.isSaving) return
        viewModelScope.launch {
            val state = _uiState.value
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            runCatching {
                notificationTimePreferences.setNotificationTime(
                    hour   = state.hour,
                    minute = state.minute
                )
                val uid = firebaseAuth.currentUser?.uid
                    ?: throw IllegalStateException("로그인된 사용자를 찾을 수 없습니다.")
                val allSchedules = scheduleRepository.getAllScheduleItems(uid)
                    .getOrThrow()
                val now = System.currentTimeMillis()
                allSchedules.forEach { item ->
                    val triggerAtMillis = item.alarmTriggerAtMillis(state.hour to state.minute)
                    if (item.status == ReviewStatus.COMPLETED || triggerAtMillis <= now) {
                        reviewAlarmScheduler.cancelReviewAlarm(item.id)
                    } else {
                        reviewAlarmScheduler.scheduleReviewAlarm(
                            reviewScheduleId = item.id,
                            triggerAtMillis = triggerAtMillis
                        )
                    }
                }
            }.onSuccess {
                _uiState.update { it.copy(isSaving = false, isSaved = true, errorMessage = null) }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        isSaved = false,
                        errorMessage = throwable.message ?: "알림 시간 저장에 실패했습니다."
                    )
                }
            }
        }
    }

    /** 저장 완료 이벤트 소비 후 초기화 (중복 네비게이션 방지) */
    fun onSavedConsumed() {
        _uiState.update { it.copy(isSaved = false) }
    }

    /** 에러 메시지 이벤트 소비 후 초기화 (중복 스낵바 방지) */
    fun onErrorConsumed() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}