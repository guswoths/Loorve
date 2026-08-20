package com.loorve.presentation.progress

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loorve.domain.model.Progress
import com.loorve.domain.repository.ProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProgressDetailUiState(
    val progress: Progress? = null,
    val isLoading: Boolean = true,
    val isEditMode: Boolean = false,
    val errorMessage: String? = null,
    val saveResult: Boolean? = null,
    val deleteResult: Boolean? = null
)

@HiltViewModel
class ProgressDetailViewModel @Inject constructor(
    private val progressRepository: ProgressRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProgressDetailUiState())
    val uiState: StateFlow<ProgressDetailUiState> = _uiState.asStateFlow()

    fun loadProgress(uid: String, progressId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = progressRepository.getProgressById(uid, progressId)
            if (result.isSuccess) {
                _uiState.update { it.copy(isLoading = false, progress = result.getOrNull()) }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading    = false,
                        errorMessage = result.exceptionOrNull()?.message ?: "불러오지 못했습니다."
                    )
                }
            }
        }
    }

    fun enterEditMode() {
        _uiState.update { it.copy(isEditMode = true) }
    }

    fun exitEditMode() {
        _uiState.update { it.copy(isEditMode = false) }
    }

    fun saveProgress(uid: String, updatedProgress: Progress) {
        val current = _uiState.value.progress ?: run {
            Log.w(TAG, "saveProgress 호출됐지만 현재 progress 상태가 null입니다.")
            _uiState.update { it.copy(saveResult = false, errorMessage = "저장할 데이터가 없습니다.") }
            return
        }

        // ✅ uid 빈값 시 명시적 에러 메시지 추가
        if (uid.isBlank()) {
            Log.e(TAG, "saveProgress 실패: uid가 비어 있습니다. 로그인 상태를 확인하세요.")
            _uiState.update { it.copy(saveResult = false, errorMessage = "로그인 정보가 없습니다. 다시 로그인해 주세요.") }
            return
        }

        // ✅ content 빈값 시 명시적 에러 메시지 추가
        if (updatedProgress.content.isBlank()) {
            Log.w(TAG, "saveProgress 실패: content가 비어 있습니다.")
            _uiState.update { it.copy(saveResult = false, errorMessage = "학습 내용을 입력해주세요.") }
            return
        }

        val merged = current.copy(
            content        = updatedProgress.content,
            completedCount = updatedProgress.completedCount,
            totalCount     = updatedProgress.totalCount,
            isCompleted    = updatedProgress.isCompleted
        )

        viewModelScope.launch {
            val result = progressRepository.saveProgress(uid, merged)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        progress   = merged,
                        isEditMode = false,
                        saveResult = true,
                        errorMessage = null
                    )
                }
            } else {
                // ✅ 실제 예외 메시지를 errorMessage에 전달
                val errMsg = result.exceptionOrNull()?.message ?: "저장 중 알 수 없는 오류가 발생했습니다."
                Log.e(TAG, "saveProgress 실패: $errMsg")
                _uiState.update { it.copy(saveResult = false, errorMessage = errMsg) }
            }
        }
    }

    fun deleteProgress(uid: String, progressId: String) {
        if (uid.isBlank() || progressId.isBlank()) {
            _uiState.update { it.copy(deleteResult = false) }
            return
        }
        viewModelScope.launch {
            val result = progressRepository.deleteProgress(uid, progressId)
            _uiState.update { it.copy(deleteResult = result.isSuccess) }
        }
    }

    fun consumeSaveResult() {
        _uiState.update { it.copy(saveResult = null) }
    }

    fun consumeDeleteResult() {
        _uiState.update { it.copy(deleteResult = null) }
    }

    companion object {
        private const val TAG = "ProgressDetailViewModel"
    }
}
