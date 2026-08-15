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

// ⚠️ 변경: FirebaseUser 제거 → 도메인 User 사용 (보안 원칙 준수)
// ⚠️ 추가: Cancelled 케이스 (사용자 취소 시 에러 노출 방지)
sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data object Cancelled : AuthUiState                    // 신규: 사용자 취소
    data class Success(val user: User) : AuthUiState       // 변경: FirebaseUser → User
    data class NetworkError(val message: String) : AuthUiState  // 신규: 네트워크 에러
    data class Error(val message: String) : AuthUiState    // 기존 유지: 일반 에러
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.signInWithGoogle(idToken)
                .onSuccess { user -> _uiState.value = AuthUiState.Success(user) }
                .onFailure { e ->
                    _uiState.value = classifyError(e)
                }
        }
    }

    fun launchGoogleSignIn(context: Context) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.launchGoogleSignIn(context)
                .onSuccess { user -> _uiState.value = AuthUiState.Success(user) }
                .onFailure { e ->
                    if (e.message == "CANCELLED") {
                        _uiState.value = AuthUiState.Cancelled
                    } else {
                        _uiState.value = classifyError(e)
                    }
                }
        }
    }


    // AuthViewModel.kt 내부 추가
    fun handleGoogleCredentialError(e: GetCredentialException) {
        _uiState.value = classifyError(e)
    }

    fun handleGoogleTokenParsingError() {
        _uiState.value = AuthUiState.Error("인증 토큰 처리 중 오류가 발생했습니다.")
    }

    fun onLoginCancelled() {
        // 소셜 로그인 팝업 취소 시 View에서 직접 호출
        _uiState.value = AuthUiState.Cancelled
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }

    /**
     * 예외 분류기: 보안상 내부 메시지를 그대로 노출하지 않음.
     * 취소 / 네트워크 / 일반 에러를 구분하여 사용자 친화적 메시지 반환.
     */
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
