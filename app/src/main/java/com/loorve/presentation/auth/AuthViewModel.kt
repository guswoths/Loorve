// 경로: app/src/main/java/com/loorve/presentation/auth/AuthViewModel.kt
package com.loorve.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loorve.domain.model.User
import com.loorve.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.content.Context
import androidx.credentials.exceptions.GetCredentialException

sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data object Cancelled : AuthUiState
    data object LogoutComplete : AuthUiState               // 신규
    data class Success(val user: User) : AuthUiState
    data class NetworkError(val message: String) : AuthUiState
    data class Error(val message: String) : AuthUiState
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    /** 현재 로그인 사용자 Flow — MyPageScreen에서 계정 정보 표시용 */
    val currentUser = authRepository.getCurrentUser()

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.signInWithGoogle(idToken)
                .onSuccess { user -> _uiState.value = AuthUiState.Success(user) }
                .onFailure { e -> _uiState.value = classifyError(e) }
        }
    }

    fun launchGoogleSignIn(context: Context) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.launchGoogleSignIn(context)
                .onSuccess { user -> _uiState.value = AuthUiState.Success(user) }
                .onFailure { e ->
                    _uiState.value = if (e.message == "CANCELLED") AuthUiState.Cancelled
                    else classifyError(e)
                }
        }
    }

    /**
     * 로그아웃 처리.
     * 성공 → LogoutComplete (View에서 네비게이션 후 resetState() 호출)
     * 실패 → Error (Snackbar 안내, 강제 이탈 금지)
     */
    fun signOut() {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.signOut()
                .onSuccess { _uiState.value = AuthUiState.LogoutComplete }
                .onFailure { e ->
                    _uiState.value = AuthUiState.Error(
                        e.message ?: "로그아웃에 실패했습니다. 다시 시도해주세요."
                    )
                }
        }
    }

    fun handleGoogleCredentialError(e: GetCredentialException) {
        _uiState.value = classifyError(e)
    }

    fun handleGoogleTokenParsingError() {
        _uiState.value = AuthUiState.Error("인증 토큰 처리 중 오류가 발생했습니다.")
    }

    fun onLoginCancelled() {
        _uiState.value = AuthUiState.Cancelled
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }

    private fun classifyError(e: Throwable): AuthUiState {
        return when {
            e is java.io.IOException ||
                    e.message?.contains("network", ignoreCase = true) == true ||
                    e.message?.contains("timeout", ignoreCase = true) == true ->
                AuthUiState.NetworkError("네트워크 연결을 확인해주세요.")
            else ->
                AuthUiState.Error("Google 로그인에 실패했습니다. 다시 시도해주세요.")
        }
    }
}