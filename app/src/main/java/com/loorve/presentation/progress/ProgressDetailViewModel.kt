package com.loorve.presentation.progress

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
    /** null=초기, true=성공, false=실패. 소비 후 null로 리셋 */
    val saveResult: Boolean? = null,
    val deleteResult: Boolean? = null
)

@HiltViewModel
class ProgressDetailViewModel @Inject constructor(
    private val progressRepository: ProgressRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProgressDetailUiState())
    val uiState: StateFlow<ProgressDetailUiState> = _uiState.asStateFlow()

    /**
     * 특정 progressId의 Progress를 로드합니다.
     * ✅ 원인3 수정: getProgressById 반환 타입이 Result<Progress>로 통일되어
     *    result.getOrNull()이 Progress? 타입으로 직접 바인딩됩니다.
     */
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

    /** 편집 모드 진입 */
    fun enterEditMode() {
        _uiState.update { it.copy(isEditMode = true) }
    }

    /** 편집 모드 종료 (변경 사항 미저장, 원본 유지) */
    fun exitEditMode() {
        _uiState.update { it.copy(isEditMode = false) }
    }

    /**
     * 수정된 Progress를 저장합니다.
     * id, examId, createdAt은 기존 값을 유지하고,
     * content / completedCount / totalCount / isCompleted 만 갱신합니다.
     *
     * ✅ 원인3 수정: Progress data class에 모든 필드가 정의되어 있으므로
     *    copy() 호출 시 Unresolved reference 에러 해소됩니다.
     */
    fun saveProgress(uid: String, updatedProgress: Progress) {
        val current = _uiState.value.progress ?: return

        if (uid.isBlank()) {
            _uiState.update { it.copy(saveResult = false) }
            return
        }
        if (updatedProgress.content.isBlank()) {
            _uiState.update { it.copy(saveResult = false) }
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
                        saveResult = true
                    )
                }
            } else {
                _uiState.update { it.copy(saveResult = false) }
            }
        }
    }

    /**
     * Progress를 삭제합니다.
     * progressId는 Progress.id 필드 값을 전달해야 합니다.
     */
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

    /** UI에서 저장 결과 이벤트를 소비한 후 상태를 초기화합니다. */
    fun consumeSaveResult() {
        _uiState.update { it.copy(saveResult = null) }
    }

    /** UI에서 삭제 결과 이벤트를 소비한 후 상태를 초기화합니다. */
    fun consumeDeleteResult() {
        _uiState.update { it.copy(deleteResult = null) }
    }
}
