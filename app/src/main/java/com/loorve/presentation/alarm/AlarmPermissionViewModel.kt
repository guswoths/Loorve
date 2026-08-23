package com.loorve.presentation.alarm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loorve.data.notification.ReviewAlarmScheduler
import com.loorve.util.ExactAlarmPermissionHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 정확한 알람 권한 상태 타입.
 */
enum class ExactAlarmPermissionState {
    /** 권한 있음: 정확한 알람 예약 가능 */
    GRANTED,
    /** 권한 없음: 사용자에게 설정 화면 이동 요청 필요 */
    DENIED,
    /** Fallback 활성: 비정확 알람으로 예약 진행 중 */
    FALLBACK_ACTIVE
}

/**
 * UI 상태 데이터 클래스.
 *
 * @param permissionState 현재 권한 상태
 * @param showPermissionDialog 권한 요청 다이얼로그 표시 여부
 * @param pendingScheduleId 권한 확인 후 예약 대기 중인 알람 ID
 * @param pendingTriggerMillis 권한 확인 후 예약 대기 중인 알람 시각
 */
data class AlarmPermissionUiState(
    val permissionState: ExactAlarmPermissionState = ExactAlarmPermissionState.GRANTED,
    val showPermissionDialog: Boolean = false,
    val pendingScheduleId: String? = null,
    val pendingTriggerMillis: Long? = null
)

@HiltViewModel
class AlarmPermissionViewModel @Inject constructor(
    private val alarmScheduler: ReviewAlarmScheduler,
    private val permissionHelper: ExactAlarmPermissionHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlarmPermissionUiState())
    val uiState: StateFlow<AlarmPermissionUiState> = _uiState.asStateFlow()

    /**
     * 앱 포그라운드 복귀 시 권한 상태 재확인 (설정 화면 복귀 후 호출).
     */
    fun refreshPermissionState() {
        val newState = if (permissionHelper.canScheduleExactAlarms()) {
            ExactAlarmPermissionState.GRANTED
        } else {
            ExactAlarmPermissionState.DENIED
        }
        _uiState.update { it.copy(permissionState = newState) }
    }

    /**
     * 알람 예약 요청 진입점.
     * 권한 없으면 다이얼로그 표시 후 대기, 권한 있으면 즉시 예약.
     *
     * @param reviewScheduleId 복습 일정 고유 ID
     * @param triggerAtMillis  알림 발생 시각
     */
    fun requestScheduleAlarm(reviewScheduleId: String, triggerAtMillis: Long) {
        viewModelScope.launch {
            if (!permissionHelper.canScheduleExactAlarms() &&
                permissionHelper.requiresExactAlarmPermission()
            ) {
                // 권한 없음: 다이얼로그 표시 + 대기 정보 저장
                _uiState.update {
                    it.copy(
                        permissionState = ExactAlarmPermissionState.DENIED,
                        showPermissionDialog = true,
                        pendingScheduleId = reviewScheduleId,
                        pendingTriggerMillis = triggerAtMillis
                    )
                }
            } else {
                scheduleAlarmInternal(reviewScheduleId, triggerAtMillis)
            }
        }
    }

    /**
     * [설정으로 이동] 버튼 클릭 핸들러.
     * 다이얼로그를 닫고 설정 화면으로 이동 (실제 Intent는 View에서 처리).
     */
    fun onGoToSettings() {
        _uiState.update { it.copy(showPermissionDialog = false) }
    }

    /**
     * [나중에] 버튼 클릭 핸들러: Fallback(비정확 알람)으로 즉시 예약.
     */
    fun onUseFallback() {
        viewModelScope.launch {
            val state = _uiState.value
            val id = state.pendingScheduleId ?: return@launch
            val millis = state.pendingTriggerMillis ?: return@launch

            _uiState.update {
                it.copy(
                    showPermissionDialog = false,
                    permissionState = ExactAlarmPermissionState.FALLBACK_ACTIVE
                )
            }
            scheduleAlarmInternal(id, millis)
        }
    }

    /**
     * 다이얼로그 닫기 (외부 클릭 등).
     */
    fun onDismissDialog() {
        _uiState.update { it.copy(showPermissionDialog = false) }
    }

    /**
     * 설정 화면에서 돌아온 후 권한 재확인 및 대기 알람 처리.
     */
    fun onReturnFromSettings() {
        viewModelScope.launch {
            refreshPermissionState()
            val state = _uiState.value
            if (state.permissionState == ExactAlarmPermissionState.GRANTED) {
                val id = state.pendingScheduleId ?: return@launch
                val millis = state.pendingTriggerMillis ?: return@launch
                scheduleAlarmInternal(id, millis)
            }
        }
    }

    private fun scheduleAlarmInternal(reviewScheduleId: String, triggerAtMillis: Long) {
        val result = alarmScheduler.scheduleReviewAlarm(reviewScheduleId, triggerAtMillis)
        val resultState = when (result) {
            ReviewAlarmScheduler.ScheduleResult.EXACT -> ExactAlarmPermissionState.GRANTED
            ReviewAlarmScheduler.ScheduleResult.FALLBACK_INEXACT -> ExactAlarmPermissionState.FALLBACK_ACTIVE
            ReviewAlarmScheduler.ScheduleResult.FAILED -> _uiState.value.permissionState
        }
        _uiState.update {
            it.copy(
                permissionState = resultState,
                pendingScheduleId = null,
                pendingTriggerMillis = null
            )
        }
    }
}