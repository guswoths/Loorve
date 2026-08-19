// 경로: app/src/main/java/com/loorve/presentation/home/HomeViewModel.kt
package com.loorve.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loorve.domain.model.Exam
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
    val errorMessage: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getExamsUseCase: GetExamsUseCase
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
}
