package com.loorve.presentation.mypage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

@HiltViewModel
class MyPageViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(MyPageUiState())
    val uiState: StateFlow<MyPageUiState> = _uiState.asStateFlow()

    fun signOut() {
        viewModelScope.launch {
            // TODO: Firebase SignOut 연결
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            // TODO: Firebase 계정 삭제 연결
        }
    }
}