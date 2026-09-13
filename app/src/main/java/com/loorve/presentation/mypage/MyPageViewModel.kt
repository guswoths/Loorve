package com.loorve.presentation.mypage

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loorve.data.local.NotificationTimePreferences
import com.loorve.data.notification.ReviewAlarmScheduler
import com.loorve.domain.model.ReviewStatus
import com.loorve.domain.model.User
import com.loorve.domain.repository.AuthRepository
import com.loorve.domain.repository.ReviewScheduleItemRepository
import com.loorve.domain.repository.ScheduleSyncStatus
import com.loorve.domain.review.nextAlarmTriggerAtMillis
import com.loorve.domain.usecase.SignOutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

data class MyPageUiState(
    val user: User? = null,
    val profileBitmap: Bitmap? = null,
    val notificationsEnabled: Boolean = true,
    val notificationTime: Pair<Int, Int> = 9 to 0,
    val syncStatus: ScheduleSyncStatus = ScheduleSyncStatus.SYNCING,
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed class MyPageEvent {
    data object SignOutSuccess : MyPageEvent()
    data object DeleteAccountSuccess : MyPageEvent()
}

@HiltViewModel
class MyPageViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val signOutUseCase: SignOutUseCase,
    private val notificationTimePreferences: NotificationTimePreferences,
    private val scheduleRepository: ReviewScheduleItemRepository,
    private val reviewAlarmScheduler: ReviewAlarmScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyPageUiState())
    val uiState: StateFlow<MyPageUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<MyPageEvent>()
    val events: SharedFlow<MyPageEvent> = _events.asSharedFlow()
    private var accountJob: Job? = null

    init {
        viewModelScope.launch {
            authRepository.getCurrentUser().collectLatest { user ->
                accountJob?.cancel()
                _uiState.value = MyPageUiState(user = user)
                if (user == null) return@collectLatest

                accountJob = launch {
                    launch {
                        notificationTimePreferences.notificationTime(user.id)
                            .collect { time -> _uiState.update { it.copy(notificationTime = time) } }
                    }
                    launch {
                        notificationTimePreferences.notificationEnabled(user.id)
                            .collect { enabled ->
                                _uiState.update { it.copy(notificationsEnabled = enabled) }
                            }
                    }
                    launch {
                        scheduleRepository.observeSyncStatus(user.id)
                            .collect { status -> _uiState.update { it.copy(syncStatus = status) } }
                    }
                    launch(Dispatchers.IO) {
                        val bitmap = user.profileImageUrl?.let { downloadProfileImage(it) }
                        if (user.id == _uiState.value.user?.id) {
                            _uiState.update { it.copy(profileBitmap = bitmap) }
                        }
                    }
                }
            }
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        val uid = _uiState.value.user?.id ?: return
        viewModelScope.launch {
            runCatching {
                notificationTimePreferences.setNotificationEnabled(uid, enabled)
                val schedules = scheduleRepository.getAllScheduleItems(uid).getOrThrow()
                if (!enabled) {
                    reviewAlarmScheduler.cancelAll(schedules.map { it.id })
                } else {
                    val time = notificationTimePreferences.notificationTime(uid).first()
                    schedules.forEach { item ->
                        if (item.status == ReviewStatus.COMPLETED) {
                            reviewAlarmScheduler.cancelReviewAlarm(item.id)
                        } else {
                            reviewAlarmScheduler.scheduleReviewAlarm(
                                item.id,
                                nextAlarmTriggerAtMillis(item.reviewDate, item.effectiveTime(time))
                            )
                        }
                    }
                }
            }.onFailure { error ->
                _uiState.update { it.copy(error = "Notification setting could not be saved.") }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            val previousState = _uiState.value
            _uiState.update { it.copy(isLoading = true) }
            _uiState.update {
                it.copy(
                    user = null,
                    profileBitmap = null,
                    syncStatus = ScheduleSyncStatus.SYNCING
                )
            }
            signOutUseCase()
                .onSuccess { _events.emit(MyPageEvent.SignOutSuccess) }
                .onFailure { error ->
                    _uiState.update {
                        previousState.copy(
                            isLoading = false,
                            error = "Logout failed. Please try again."
                        )
                    }
                }
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            authRepository.deleteAccount()
                .onSuccess { _events.emit(MyPageEvent.DeleteAccountSuccess) }
                .onFailure {
                    _uiState.update {
                        it.copy(isLoading = false, error = "Account deletion failed. Please try again.")
                    }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun com.loorve.domain.model.ReviewScheduleItem.effectiveTime(
        default: Pair<Int, Int>
    ): Pair<Int, Int> = customAlarmTime ?: default

    private suspend fun downloadProfileImage(url: String): Bitmap? = withContext(Dispatchers.IO) {
        runCatching {
            (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 5_000
                readTimeout = 5_000
                doInput = true
                connect()
            }.inputStream.use(BitmapFactory::decodeStream)
        }.getOrNull()
    }
}
