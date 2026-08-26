package com.loorve.presentation.mypage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loorve.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
class MyPageViewModel @Inject constructor(
    private val authRepository: AuthRepository   // ✅ Repository DI 주입
) : ViewModel() {

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
            // ✅ AuthRepositoryImpl.signOut() 위임
            // → CredentialManager.clearCredentialState() + FirebaseAuth.signOut() 포함
            authRepository.signOut()
                .onSuccess {
                    _events.emit(MyPageEvent.SignOutSuccess)
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = "로그아웃에 실패했습니다: ${e.message}")
                    }
                }
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            // ✅ AuthRepositoryImpl.deleteAccount() 위임
            // → Firestore 서브컬렉션 삭제 + credential 초기화 + Auth 계정 삭제 포함
            authRepository.deleteAccount()
                .onSuccess {
                    _events.emit(MyPageEvent.DeleteAccountSuccess)
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = "계정 삭제에 실패했습니다: ${e.message}")
                    }
                }
        }
    }
}