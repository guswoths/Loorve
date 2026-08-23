package com.loorve.presentation.mypage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loorve.data.local.NotificationTimePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationTimeUiState(
    val hour: Int = 9,
    val minute: Int = 0,
    val isSaved: Boolean = false,      // 저장 완료 one-shot 이벤트용
    val errorMessage: String? = null   // 저장 실패 시 one-shot 에러 이벤트용
)

@HiltViewModel
class NotificationTimeSettingViewModel @Inject constructor(
    private val notificationTimePreferences: NotificationTimePreferences
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
        viewModelScope.launch {
            runCatching {
                notificationTimePreferences.setNotificationTime(
                    hour   = _uiState.value.hour,
                    minute = _uiState.value.minute
                )
            }.onSuccess {
                _uiState.update { it.copy(isSaved = true, errorMessage = null) }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
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