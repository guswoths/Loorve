package com.loorve.presentation.login

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

// 로그인 화면 UI 상태 정의 (UDF 단방향 데이터 흐름)
sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object Loading : LoginUiState
    data class Success(val user: User) : LoginUiState  // 도메인 모델 사용 (FirebaseUser 노출 금지)
    data class Error(val message: String) : LoginUiState
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /**
     * 이메일/비밀번호 로그인
     * 보안 정책: 비밀번호는 절대 로그에 출력하지 않는다.
     * SECURITY: password must never be logged or exposed in any output.
     */
    fun login(email: String, password: String) {
        // 최소 유효성 검사 (빈 값 방지)
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = LoginUiState.Error("이메일과 비밀번호를 모두 입력해주세요.")
            return
        }

        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            authRepository.login(email, password)
                .onSuccess { user -> _uiState.value = LoginUiState.Success(user) }
                .onFailure { e -> _uiState.value = LoginUiState.Error(e.message ?: "알 수 없는 오류") }
        }
    }

    /** UI 상태를 Idle로 초기화 (화면 재진입 또는 오류 해제 시 사용) */
    fun resetState() {
        _uiState.value = LoginUiState.Idle
    }
}
