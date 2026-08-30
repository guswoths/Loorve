package com.loorve.presentation.reviewblock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loorve.domain.usecase.CreateReviewBlockRequest
import com.loorve.domain.usecase.CreateReviewBlockUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ReviewBlockUiState {
    data object Idle : ReviewBlockUiState()
    data object Loading : ReviewBlockUiState()
    data object Success : ReviewBlockUiState()
    data class Error(val message: String) : ReviewBlockUiState()
}

@HiltViewModel
class ReviewBlockViewModel @Inject constructor(
    private val createReviewBlockUseCase: CreateReviewBlockUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ReviewBlockUiState>(
        ReviewBlockUiState.Idle
    )
    val uiState: StateFlow<ReviewBlockUiState> = _uiState.asStateFlow()

    fun createReviewBlock(
        uid: String,
        examName: String,
        examDateMillis: Long,
        cycleOption: Int
    ) {
        if (_uiState.value is ReviewBlockUiState.Loading) return

        viewModelScope.launch {
            _uiState.value = ReviewBlockUiState.Loading

            createReviewBlockUseCase(
                CreateReviewBlockRequest(
                    uid = uid,
                    examName = examName,
                    examDateMillis = examDateMillis,
                    cycleOption = cycleOption
                )
            ).onSuccess {
                _uiState.value = ReviewBlockUiState.Success
            }.onFailure { throwable ->
                android.util.Log.e("ReviewBlockVM", "createReviewBlock failed", throwable)
                _uiState.value = ReviewBlockUiState.Error(
                    throwable.message ?: "복습 블록 생성에 실패했습니다."
                )
            }
        }
    }

    fun resetState() {
        _uiState.value = ReviewBlockUiState.Idle
    }
}