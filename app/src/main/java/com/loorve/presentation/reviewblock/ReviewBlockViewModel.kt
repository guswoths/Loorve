package com.loorve.presentation.reviewblock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loorve.domain.model.ReviewBlock
import com.loorve.domain.repository.ReviewBlockRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ReviewBlockUiState {
    object Idle : ReviewBlockUiState()
    object Loading : ReviewBlockUiState()
    object Success : ReviewBlockUiState()
    data class Error(val message: String) : ReviewBlockUiState()
}

@HiltViewModel
class ReviewBlockViewModel @Inject constructor(
    private val reviewBlockRepository: ReviewBlockRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ReviewBlockUiState>(ReviewBlockUiState.Idle)
    val uiState: StateFlow<ReviewBlockUiState> = _uiState.asStateFlow()

    private val _reviewBlocks = MutableStateFlow<List<ReviewBlock>>(emptyList())
    val reviewBlocks: StateFlow<List<ReviewBlock>> = _reviewBlocks.asStateFlow()

    fun saveReviewBlock(block: ReviewBlock) {
        viewModelScope.launch {
            _uiState.value = ReviewBlockUiState.Loading
            reviewBlockRepository.saveReviewBlock(block)
                .onSuccess {
                    _uiState.value = ReviewBlockUiState.Success
                }
                .onFailure { e ->
                    _uiState.value = ReviewBlockUiState.Error(
                        e.message ?: "알 수 없는 오류가 발생했습니다."
                    )
                }
        }
    }

    fun resetState() {
        _uiState.value = ReviewBlockUiState.Idle
    }
}