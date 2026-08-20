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
    val progressSaveResult: Boolean? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getExamsUseCase: GetExamsUseCase,
    private val addProgressUseCase: AddProgressUseCase
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
                .catch { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = exception.message ?: "목록을 불러오지 못했습니다."
                        )
                    }
                }
                .collect { exams ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            exams = exams
                        )
                    }
                }
        }
    }

    fun addProgress(
        examId: String,
        content: String,
        completedCount: Int,
        totalCount: Int
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    progressSaveResult = false,
                    errorMessage = "로그인 정보가 없습니다. 다시 로그인해 주세요."
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    progressSaveResult = null,
                    errorMessage = null
                )
            }

            val progress = Progress(
                examId = examId,
                content = content.trim(),
                completedCount = completedCount,
                totalCount = totalCount,
                isCompleted = totalCount > 0 && completedCount >= totalCount
            )

            val result = addProgressUseCase(uid, progress)

            _uiState.update {
                if (result.isSuccess) {
                    it.copy(
                        progressSaveResult = true,
                        errorMessage = null
                    )
                } else {
                    it.copy(
                        progressSaveResult = false,
                        errorMessage = result.exceptionOrNull()?.message
                            ?: "학습 진도를 저장하지 못했습니다."
                    )
                }
            }
        }
    }

    fun consumeProgressSaveResult() {
        _uiState.update {
            it.copy(progressSaveResult = null)
        }
    }

    fun consumeErrorMessage() {
        _uiState.update {
            it.copy(errorMessage = null)
        }
    }
}
