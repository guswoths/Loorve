// 경로: app/src/main/java/com/loorve/presentation/home/HomeViewModel.kt
package com.loorve.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.loorve.domain.model.Exam
import com.loorve.domain.model.Progress
import com.loorve.domain.usecase.AddProgressUseCase
import com.loorve.domain.usecase.GetExamsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val exams: List<Exam> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    /** null=초기, true=저장 성공, false=저장 실패. 소비 후 null로 리셋 */
    val progressSaveResult: Boolean? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getExamsUseCase: GetExamsUseCase,
    private val addProgressUseCase: AddProgressUseCase   // ← 추가
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadExams()
    }

    fun loadExams() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            getExamsUseCase()
                .catch { e ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = e.message ?: "목록을 불러오지 못했습니다.")
                    }
                }
                .collect { exams ->
                    _uiState.update { it.copy(isLoading = false, exams = exams) }
                }
        }
    }

    /**
     * 학습 진도를 저장합니다.
     * uid는 현재 FirebaseAuth 로그인 사용자 기준이며,
     * createdAt은 UseCase 내부에서 KST 당일 자정으로 자동 부여됩니다.
     */
    fun addProgress(
        examId: String,
        content: String,
        completedCount: Int,
        totalCount: Int
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            _uiState.update { it.copy(progressSaveResult = false) }
            return
        }
        viewModelScope.launch {
            val progress = Progress(
                examId         = examId,
                content        = content,
                completedCount = completedCount,
                totalCount     = totalCount,
                isCompleted    = totalCount > 0 && completedCount >= totalCount
            )
            val result = addProgressUseCase(uid, progress)
            _uiState.update { it.copy(progressSaveResult = result.isSuccess) }
        }
    }

    /** UI에서 저장 결과 이벤트를 소비한 후 상태를 초기화합니다. */
    fun consumeProgressSaveResult() {
        _uiState.update { it.copy(progressSaveResult = null) }
    }
}
