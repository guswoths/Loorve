package com.loorve.presentation.mypage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

// ── UiState ──────────────────────────────────────────────────────
data class MyPageUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)

// ── 일회성 이벤트 ─────────────────────────────────────────────────
sealed class MyPageEvent {
    object SignOutSuccess : MyPageEvent()
    object DeleteAccountSuccess : MyPageEvent()
}

// ── ViewModel ────────────────────────────────────────────────────
@HiltViewModel
class MyPageViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(MyPageUiState())
    val uiState: StateFlow<MyPageUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<MyPageEvent>()
    val events: SharedFlow<MyPageEvent> = _events.asSharedFlow()

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun signOut() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                FirebaseAuth.getInstance().signOut()
                _events.emit(MyPageEvent.SignOutSuccess)
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "로그아웃에 실패했습니다: ${e.message}") }
            }
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                FirebaseAuth.getInstance().currentUser?.delete()?.await()
                _events.emit(MyPageEvent.DeleteAccountSuccess)
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "계정 삭제에 실패했습니다: ${e.message}") }
            }
        }
    }
}