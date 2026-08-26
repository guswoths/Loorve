package com.loorve.presentation.mypage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loorve.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MyPageUiState(
    val isLoading: Boolean = false,
    val totalStudyDays: Int = 0,
    val completedProgress: Int = 0,
    val examCount: Int = 0,
    val error: String? = null
)

sealed interface MyPageEvent {
    data object SignOutSuccess : MyPageEvent
    data object DeleteAccountSuccess : MyPageEvent
}

@HiltViewModel
class MyPageViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyPageUiState())
    val uiState: StateFlow<MyPageUiState> = _uiState.asStateFlow()

    // 일회성 이벤트용 Channel (성공 시 화면 이동 트리거)
    private val _events = Channel<MyPageEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun signOut() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            authRepository.signOut()
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false) }
                    _events.send(MyPageEvent.SignOutSuccess)
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            authRepository.deleteAccount()
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false) }
                    _events.send(MyPageEvent.DeleteAccountSuccess)
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}