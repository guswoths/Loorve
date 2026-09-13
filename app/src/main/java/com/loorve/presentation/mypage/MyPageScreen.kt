package com.loorve.presentation.mypage

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.loorve.BuildConfig
import com.loorve.R
import com.loorve.domain.repository.ScheduleSyncStatus
import com.loorve.ui.component.BannerAdView
import com.loorve.ui.component.LoorveCard
import com.loorve.ui.theme.Background
import com.loorve.ui.theme.Error
import com.loorve.ui.theme.LoorveTypography
import com.loorve.ui.theme.OnBackground
import com.loorve.ui.theme.OnSurfaceVariant
import com.loorve.ui.theme.Primary
import com.loorve.ui.theme.Surface
import com.loorve.ui.theme.SurfaceVariant

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MyPageScreen(
    onBack: () -> Unit,
    onNavigateToNotificationTimeSetting: () -> Unit,
    onNavigateToBatteryOptimization: () -> Unit,
    onNavigateToNotificationPermission: () -> Unit = onNavigateToNotificationTimeSetting,
    onSignOut: () -> Unit,
    viewModel: MyPageViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val notificationAllowed = lifecycleState.let { Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                MyPageEvent.SignOutSuccess, MyPageEvent.DeleteAccountSuccess -> onSignOut()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.settings_header_english), style = LoorveTypography.labelSmall, color = Primary, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.settings_header_korean), style = LoorveTypography.titleLarge, color = OnBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        },
        bottomBar = { BannerAdView(modifier = Modifier.fillMaxWidth()) },
        containerColor = Background
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
            ) {
                item {
                    SectionTitle(stringResource(R.string.settings_account))
                    LoorveCard(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            uiState.profileBitmap?.let {
                                Image(
                                    bitmap = it.asImageBitmap(),
                                    contentDescription = stringResource(R.string.settings_profile_image),
                                    modifier = Modifier.size(56.dp).clip(CircleShape)
                                )
                            } ?: Box(
                                Modifier.size(56.dp).background(Primary.copy(alpha = 0.12f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = (uiState.user?.nickname ?: stringResource(R.string.settings_fallback_name))
                                        .firstOrNull()?.uppercase() ?: "?",
                                    color = Primary,
                                    style = LoorveTypography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.size(14.dp))
                            Column {
                                Text(
                                    uiState.user?.nickname ?: stringResource(R.string.settings_fallback_name),
                                    style = LoorveTypography.bodyLarge,
                                    color = OnBackground,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    uiState.user?.email?.takeIf { it.isNotBlank() }
                                        ?: stringResource(R.string.settings_no_email),
                                    style = LoorveTypography.bodySmall,
                                    color = OnSurfaceVariant
                                )
                            }
                        }
                        HorizontalDivider(color = SurfaceVariant, thickness = 0.5.dp)
                        SettingsRow(
                            icon = Icons.AutoMirrored.Filled.Logout,
                            title = stringResource(R.string.settings_logout),
                            subtitle = stringResource(R.string.settings_logout_subtitle),
                            actionLabel = stringResource(R.string.settings_confirm_logout),
                            actionColor = Error,
                            onAction = { showLogoutDialog = true }
                        )
                    }
                }

                item {
                    SectionTitle(stringResource(R.string.settings_notifications))
                    LoorveCard(Modifier.fillMaxWidth()) {
                        SettingsSwitchRow(
                            title = stringResource(R.string.settings_enable_notifications),
                            checked = uiState.notificationsEnabled,
                            onCheckedChange = viewModel::setNotificationsEnabled
                        )
                        HorizontalDivider(color = SurfaceVariant, thickness = 0.5.dp)
                        SettingsRow(
                            icon = Icons.Default.Schedule,
                            title = stringResource(R.string.settings_notification_time),
                            subtitle = stringResource(
                                R.string.settings_time_format,
                                uiState.notificationTime.first,
                                uiState.notificationTime.second
                            ),
                            actionLabel = stringResource(R.string.settings_chevron),
                            onAction = onNavigateToNotificationTimeSetting
                        )
                        HorizontalDivider(color = SurfaceVariant, thickness = 0.5.dp)
                        SettingsRow(
                            icon = Icons.Default.Notifications,
                            title = stringResource(R.string.settings_notification_permission),
                            subtitle = stringResource(
                                if (notificationAllowed) R.string.settings_permission_allowed
                                else R.string.settings_permission_not_allowed
                            ),
                            actionLabel = if (notificationAllowed) null
                            else stringResource(R.string.settings_request_permission),
                            onAction = onNavigateToNotificationPermission
                        )
                        HorizontalDivider(color = SurfaceVariant, thickness = 0.5.dp)
                        SettingsRow(
                            icon = Icons.Default.BatteryFull,
                            title = stringResource(R.string.settings_battery_guidance),
                            subtitle = stringResource(R.string.settings_battery_guidance_subtitle),
                            actionLabel = stringResource(R.string.settings_chevron),
                            onAction = onNavigateToBatteryOptimization
                        )
                    }
                }

                item {
                    SectionTitle(stringResource(R.string.settings_data))
                    LoorveCard(Modifier.fillMaxWidth()) {
                        SettingsRow(
                            icon = Icons.Default.Cloud,
                            title = stringResource(R.string.settings_sync_status),
                            subtitle = syncStatusText(uiState.syncStatus),
                            onAction = {}
                        )
                    }
                }

                item {
                    SectionTitle(stringResource(R.string.settings_app_information))
                    LoorveCard(Modifier.fillMaxWidth()) {
                        SettingsRow(
                            icon = Icons.Default.Info,
                            title = stringResource(R.string.settings_app_information),
                            subtitle = stringResource(
                                R.string.settings_version,
                                BuildConfig.VERSION_NAME.ifBlank { stringResource(R.string.settings_unknown) }
                            ),
                            onAction = {}
                        )
                    }
                }

                item {
                    TextButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            stringResource(R.string.settings_delete_account),
                            color = Error.copy(alpha = 0.7f),
                            style = LoorveTypography.bodyMedium
                        )
                    }
                }
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { if (!uiState.isLoading) showLogoutDialog = false },
            containerColor = Surface,
            title = { Text(stringResource(R.string.settings_logout_title), color = OnBackground) },
            text = { Text(stringResource(R.string.settings_logout_message), color = OnBackground) },
            confirmButton = {
                TextButton(
                    enabled = !uiState.isLoading,
                    onClick = {
                        showLogoutDialog = false
                        viewModel.signOut()
                    }
                ) { Text(stringResource(R.string.settings_confirm_logout), color = Error) }
            },
            dismissButton = {
                TextButton(
                    enabled = !uiState.isLoading,
                    onClick = { showLogoutDialog = false }
                ) { Text(stringResource(R.string.settings_cancel), color = OnSurfaceVariant) }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { if (!uiState.isLoading) showDeleteDialog = false },
            containerColor = Surface,
            title = { Text(stringResource(R.string.settings_delete_account), color = OnBackground) },
            text = { Text(stringResource(R.string.settings_delete_account_message), color = OnBackground) },
            confirmButton = {
                TextButton(
                    enabled = !uiState.isLoading,
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteAccount()
                    }
                ) { Text(stringResource(R.string.settings_delete), color = Error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.settings_cancel), color = OnSurfaceVariant)
                }
            }
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = LoorveTypography.labelLarge,
        color = OnSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
    )
}

@Composable
private fun syncStatusText(status: ScheduleSyncStatus): String = stringResource(
    when (status) {
        ScheduleSyncStatus.SYNCED -> R.string.settings_synced
        ScheduleSyncStatus.OFFLINE -> R.string.settings_offline
        ScheduleSyncStatus.SYNCING -> R.string.settings_syncing
        ScheduleSyncStatus.ERROR -> R.string.settings_sync_error
    }
)

@Composable
private fun SettingsSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Notifications, contentDescription = null, tint = OnBackground, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Text(title, Modifier.weight(1f), style = LoorveTypography.bodyLarge, color = OnBackground)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    actionColor: Color = Primary,
    onAction: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth()
            .clickable(role = Role.Button, onClick = onAction)
            .semantics { contentDescription = title }
            .padding(vertical = 14.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = OnBackground, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = LoorveTypography.bodyLarge, color = OnBackground, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = LoorveTypography.bodySmall, color = OnSurfaceVariant)
        }
        actionLabel?.let {
            Text(it, color = actionColor, style = LoorveTypography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}
