package com.loorve.presentation.notification

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class NotificationPermissionState {
    GRANTED,
    NEEDS_REQUEST,
    SHOW_RATIONALE,
    PERMANENTLY_DENIED
}

data class NotificationPermissionUiState(
    val permissionState: NotificationPermissionState = NotificationPermissionState.GRANTED,
    val showRationaleDialog: Boolean = false,
    val showSettingsDialog: Boolean = false,
    val hasRequestedBefore: Boolean = false
)

class NotificationPermissionViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val appContext: Context = application.applicationContext

    private val _uiState = MutableStateFlow(NotificationPermissionUiState())
    val uiState: StateFlow<NotificationPermissionUiState> = _uiState.asStateFlow()

    fun refreshPermissionState(
        shouldShowRationale: Boolean,
        hasRequestedBefore: Boolean
    ) {
        val nextState = resolvePermissionState(
            context = appContext,
            shouldShowRationale = shouldShowRationale,
            hasRequestedBefore = hasRequestedBefore
        )

        _uiState.update {
            it.copy(
                permissionState = nextState,
                hasRequestedBefore = hasRequestedBefore,
                showRationaleDialog = false,
                showSettingsDialog = false
            )
        }
    }

    fun onEnableNotificationClicked(
        shouldShowRationale: Boolean,
        hasRequestedBefore: Boolean
    ): NotificationPermissionAction {
        val currentState = resolvePermissionState(
            context = appContext,
            shouldShowRationale = shouldShowRationale,
            hasRequestedBefore = hasRequestedBefore
        )

        return when (currentState) {
            NotificationPermissionState.GRANTED -> NotificationPermissionAction.None
            NotificationPermissionState.SHOW_RATIONALE -> {
                _uiState.update {
                    it.copy(
                        permissionState = NotificationPermissionState.SHOW_RATIONALE,
                        showRationaleDialog = true,
                        showSettingsDialog = false,
                        hasRequestedBefore = hasRequestedBefore
                    )
                }
                NotificationPermissionAction.None
            }
            NotificationPermissionState.PERMANENTLY_DENIED -> {
                _uiState.update {
                    it.copy(
                        permissionState = NotificationPermissionState.PERMANENTLY_DENIED,
                        showRationaleDialog = false,
                        showSettingsDialog = true,
                        hasRequestedBefore = hasRequestedBefore
                    )
                }
                NotificationPermissionAction.None
            }
            NotificationPermissionState.NEEDS_REQUEST -> {
                _uiState.update {
                    it.copy(
                        permissionState = NotificationPermissionState.NEEDS_REQUEST,
                        showRationaleDialog = false,
                        showSettingsDialog = false,
                        hasRequestedBefore = hasRequestedBefore
                    )
                }
                NotificationPermissionAction.LaunchSystemPermissionRequest
            }
        }
    }

    fun onConfirmRationaleRequest(): NotificationPermissionAction {
        _uiState.update { it.copy(showRationaleDialog = false) }
        return NotificationPermissionAction.LaunchSystemPermissionRequest
    }

    fun onDismissRationaleDialog() {
        _uiState.update { it.copy(showRationaleDialog = false) }
    }

    fun onDismissSettingsDialog() {
        _uiState.update { it.copy(showSettingsDialog = false) }
    }

    fun onPermissionResult(
        isGranted: Boolean,
        shouldShowRationale: Boolean,
        hasRequestedBefore: Boolean
    ) {
        val nextState = when {
            isGranted -> NotificationPermissionState.GRANTED
            shouldShowRationale -> NotificationPermissionState.SHOW_RATIONALE
            hasRequestedBefore -> NotificationPermissionState.PERMANENTLY_DENIED
            else -> NotificationPermissionState.NEEDS_REQUEST
        }

        _uiState.update {
            it.copy(
                permissionState = nextState,
                showRationaleDialog = false,
                showSettingsDialog = nextState == NotificationPermissionState.PERMANENTLY_DENIED,
                hasRequestedBefore = hasRequestedBefore
            )
        }
    }

    companion object {
        fun hasNotificationPermission(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                NotificationManagerCompat.from(context).areNotificationsEnabled()
            }
        }

        private fun resolvePermissionState(
            context: Context,
            shouldShowRationale: Boolean,
            hasRequestedBefore: Boolean
        ): NotificationPermissionState {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                return if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                    NotificationPermissionState.GRANTED
                } else {
                    NotificationPermissionState.PERMANENTLY_DENIED
                }
            }

            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            return when {
                granted -> NotificationPermissionState.GRANTED
                shouldShowRationale -> NotificationPermissionState.SHOW_RATIONALE
                hasRequestedBefore -> NotificationPermissionState.PERMANENTLY_DENIED
                else -> NotificationPermissionState.NEEDS_REQUEST
            }
        }
    }
}

enum class NotificationPermissionAction {
    None,
    LaunchSystemPermissionRequest
}