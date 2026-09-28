package com.loorve.presentation.mypage

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.loorve.BuildConfig
import com.loorve.R
import com.loorve.domain.repository.ScheduleSyncStatus
import com.loorve.presentation.notification.NotificationPermissionViewModel
import com.loorve.domain.subscription.SubscriptionEntitlement
import com.loorve.presentation.subscription.ProPaywallDialog
import com.loorve.presentation.subscription.SubscriptionViewModel
import com.loorve.ui.component.BannerAdView
import com.loorve.ui.component.LoorveCard
import com.loorve.ui.theme.Background
import com.loorve.ui.theme.CanvasWarm
import com.loorve.ui.theme.Error
import com.loorve.ui.theme.Active
import com.loorve.ui.theme.ActiveContainer
import com.loorve.ui.theme.AiSurface
import com.loorve.ui.theme.AuroraBlue
import com.loorve.ui.theme.AuroraPink
import com.loorve.ui.theme.AuroraViolet
import com.loorve.ui.theme.Divider
import com.loorve.ui.theme.InternalDivider
import com.loorve.ui.theme.LoorveTypography
import com.loorve.ui.theme.OnBackground
import com.loorve.ui.theme.OnSurfaceVariant
import com.loorve.ui.theme.Primary
import com.loorve.ui.theme.Surface
import com.loorve.ui.theme.SurfaceSolid
import com.loorve.ui.theme.SurfaceVariant
import com.loorve.ui.theme.Success
import com.loorve.ui.theme.SuccessContainer
import com.loorve.ui.theme.Warning
import com.loorve.ui.theme.WarningContainer
import com.loorve.ui.theme.Notice
import com.loorve.ui.theme.NoticeContainer
import com.loorve.ui.theme.VioletGlow

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MyPageScreen(
    onBack: () -> Unit,
    onNavigateToNotificationTimeSetting: () -> Unit,
    onNavigateToBatteryOptimization: () -> Unit,
    onNavigateToNotificationPermission: () -> Unit = onNavigateToNotificationTimeSetting,
    onSignOut: () -> Unit,
    viewModel: MyPageViewModel = hiltViewModel(),
    subscriptionViewModel: SubscriptionViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showProDialog by remember { mutableStateOf(false) }
    val subscriptionState by subscriptionViewModel.state.collectAsStateWithLifecycle()
    val notificationAllowed = lifecycleState.let {
        NotificationPermissionViewModel.hasNotificationPermission(context)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                MyPageEvent.SignOutSuccess, MyPageEvent.DeleteAccountSuccess -> onSignOut()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Scaffold(
            topBar = {},
            bottomBar = {
                val showBanner = subscriptionState.entitlement is SubscriptionEntitlement.Free
                if (showBanner) {
                    key(showBanner) {
                        BannerAdView(modifier = Modifier.fillMaxWidth())
                    }
                } else {
                    Spacer(modifier = Modifier.height(0.dp))
                }
            },
            containerColor = Color.Transparent
        ) { padding ->
            if (uiState.isLoading) {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Primary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 20.dp, bottom = 140.dp)
                ) {
                item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp)
                ) {
                    Text(
                        text = "SETTINGS",
                        style = LoorveTypography.labelSmall.copy(
                            fontSize = 12.sp,
                            letterSpacing = 1.5.sp
                        ),
                        color = Primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "설정",
                        style = LoorveTypography.titleLarge.copy(
                            fontSize = 26.sp,
                            letterSpacing = (-0.6).sp
                        ),
                        color = Color(0xFF0F172A),
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                }
                item {
                    SectionTitle(stringResource(R.string.settings_account))
                    LoorveCard(
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = Color.White
                    ) {
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        uiState.user?.nickname
                                            ?: stringResource(R.string.settings_fallback_name),
                                        style = LoorveTypography.bodyLarge,
                                        color = OnBackground,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    if (subscriptionState.entitlement is SubscriptionEntitlement.Pro) {
                                        SettingsProBadgeLiquid()
                                    } else {
                                        Surface(shape = CircleShape, color = Color(0xFFF1F5F9)) {
                                            Text(
                                                text = "BASIC",
                                                style = LoorveTypography.labelSmall.copy(
                                                    fontSize = 10.sp,
                                                    letterSpacing = 0.8.sp
                                                ),
                                                color = Color(0xFF94A3B8),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }
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
                    LoorveCard(
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = Color.White
                    ) {
                        SettingsSwitchRow(
                            title = stringResource(R.string.settings_enable_notifications),
                            checked = uiState.notificationsEnabled,
                            onCheckedChange = viewModel::setNotificationsEnabled
                        )
                        HorizontalDivider(color = SurfaceVariant, thickness = 0.5.dp)
                        NotificationSettingRow(
                            notificationTime = stringResource(
                                R.string.settings_time_format,
                                uiState.notificationTime.first,
                                uiState.notificationTime.second
                            ),
                            notificationAllowed = notificationAllowed,
                            onChangeTime = onNavigateToNotificationTimeSetting,
                            onRequestPermission = onNavigateToNotificationPermission
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
                    LoorveCard(
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = Color.White
                    ) {
                        SettingsRow(
                            icon = Icons.Default.Cloud,
                            title = stringResource(R.string.settings_sync_status),
                            subtitle = syncStatusText(uiState.syncStatus),
                            onAction = {}
                        )
                    }
                }

                item {
                    ProShowcaseBanner(
                        isProSubscribed = subscriptionState.entitlement is SubscriptionEntitlement.Pro,
                        onClick = { showProDialog = true }
                    )
                }

                item {
                    SectionTitle(stringResource(R.string.settings_app_information))
                    LoorveCard(
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = Color.White
                    ) {
                        SettingsRow(
                            icon = Icons.Default.Info,
                            title = stringResource(R.string.settings_app_information),
                            subtitle = stringResource(
                                R.string.settings_version,
                                BuildConfig.VERSION_NAME.ifBlank { stringResource(R.string.settings_unknown) }
                            ),
                            onAction = {}
                        )
                        HorizontalDivider(color = SurfaceVariant, thickness = 0.5.dp)
                        SettingsRow(
                            icon = Icons.Default.Info,
                            title = "개인정보처리방침",
                            subtitle = "공식 개인정보처리방침 웹페이지 확인",
                            actionLabel = "보기",
                            onAction = {
                                runCatching {
                                    context.startActivity(
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("https://living-nitrogen-cfb.notion.site/3d8da8c67899800cb78be40cd9efdf97")
                                        )
                                    )
                                }
                            }
                        )
                    }
                }

                item {
                    SectionTitle("계정 관리")
                    LoorveCard(
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = Color.White
                    ) {
                        SettingsRow(
                            icon = Icons.Default.Delete,
                            title = "계정 탈퇴 (계정 및 데이터 삭제)",
                            subtitle = "탈퇴 시 학습 기록, 복습 일정 등 모든 데이터 영구 삭제",
                            actionLabel = "탈퇴",
                            actionColor = Error,
                            onAction = { showDeleteDialog = true }
                        )
                    }
                }

                item {
                    Spacer(Modifier.height(10.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "개인정보처리방침 (웹페이지 바로가기)",
                                style = LoorveTypography.bodySmall.copy(
                                    fontSize = 11.5.sp,
                                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                                ),
                                color = Color(0xFF2563EB),
                                modifier = Modifier.clickable {
                                    runCatching {
                                        context.startActivity(
                                            Intent(
                                                Intent.ACTION_VIEW,
                                                Uri.parse("https://living-nitrogen-cfb.notion.site/3d8da8c67899800cb78be40cd9efdf97")
                                            )
                                        )
                                    }
                                }
                            )
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "상호: guswoths | 대표자명: 손현재",
                            style = LoorveTypography.bodySmall.copy(fontSize = 11.5.sp),
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "사업자등록번호: 등록 예정 | 통신판매업신고번호: 신고 예정",
                            style = LoorveTypography.bodySmall.copy(fontSize = 11.5.sp),
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "고객지원 / 버그 문의: hjson7585@gmail.com",
                            style = LoorveTypography.bodySmall.copy(fontSize = 11.5.sp),
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "© 2026 guswoths. All rights reserved.",
                            style = LoorveTypography.labelSmall.copy(fontSize = 10.sp),
                            color = Color(0xFFCBD5E1),
                            textAlign = TextAlign.Center
                        )
                    }
                }

            }
        }
    }
    }

    if (showProDialog) {
        ProPaywallDialog(
            viewModel = subscriptionViewModel,
            onDismiss = { showProDialog = false }
        )
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
            containerColor = Color.White,
            title = {
                Text(
                    text = "계정 탈퇴 (계정 및 데이터 삭제)",
                    color = Color(0xFF0F172A),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "정말로 계정을 탈퇴하시겠습니까? 탈퇴 시 아래의 안내 사항을 반드시 확인해 주세요.",
                        color = Color(0xFF334155),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "• 삭제 데이터 범위: 회원 프로필, 학습 기록, 복습 블록 및 복습 일정 전체, 알림 설정 등 계정과 연관된 모든 활동 데이터",
                                fontSize = 12.sp,
                                color = Color(0xFF475569),
                                lineHeight = 16.sp
                            )
                            Text(
                                text = "• 탈퇴 즉시 실제 모든 데이터가 영구 삭제되며 복구할 수 없습니다.",
                                fontSize = 12.sp,
                                color = Color(0xFFE11D48),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFFF1F2),
                        border = BorderStroke(1.dp, Color(0xFFFECDD3)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "⚠️ 정기결제(구독) 주의사항",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE11D48)
                            )
                            Text(
                                text = "앱 계정을 탈퇴하더라도 Google Play 스토어의 정기결제(구독)는 자동으로 해지되지 않습니다. 구독 해지 및 정기결제 관리는 Google Play 스토어의 [결제 및 정기 결제]에서 직접 진행하셔야 합니다.",
                                fontSize = 12.sp,
                                color = Color(0xFF9F1239),
                                lineHeight = 16.5.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !uiState.isLoading,
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteAccount()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48))
                ) {
                    Text("데이터 모두 삭제 후 탈퇴", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !uiState.isLoading,
                    onClick = { showDeleteDialog = false }
                ) {
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
        fontSize = 12.sp,
        color = Color(0xFF64748B),
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp)
    )
}

@Suppress("unused")
@Composable
private fun SettingsAmbientAura() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        fun aura(center: Offset, radius: Float, color: Color) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = 0.24f), color.copy(alpha = 0.09f), Color.Transparent),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
        }

        aura(
            center = Offset(size.width * 0.06f, size.height * 0.08f),
            radius = size.minDimension * 0.72f,
            color = Color(0xFF38BDF8)
        )
        aura(
            center = Offset(size.width * 0.96f, size.height * 0.36f),
            radius = size.minDimension * 0.80f,
            color = Color(0xFF0284C7)
        )
        aura(
            center = Offset(size.width * 0.12f, size.height * 0.78f),
            radius = size.minDimension * 0.74f,
            color = Color(0xFF7DD3FC)
        )
        aura(
            center = Offset(size.width * 0.94f, size.height * 0.92f),
            radius = size.minDimension * 0.64f,
            color = Color(0xFFBAE6FD)
        )
    }
}

@Composable
private fun ProShowcaseBanner(
    isProSubscribed: Boolean,
    onClick: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "settingsProBlock")
    val shift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "settingsProBlockShift"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF0B1930),
                        Color(0xFF1E3A8A),
                        Color(0xFF0284C7)
                    ),
                    start = Offset(shift * 80f, 0f),
                    end = Offset(420f + shift * 80f, 220f)
                )
            )
            .drawWithCache {
                val sheenX = size.width * (-0.8f + shift * 2.4f)
                onDrawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.10f),
                                Color.White.copy(alpha = 0.34f),
                                Color.White.copy(alpha = 0.10f),
                                Color.Transparent
                            ),
                            start = Offset(sheenX - size.width, 0f),
                            end = Offset(sheenX, size.height)
                        )
                    )
                }
            }
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "LOORVE PRO",
                style = LoorveTypography.labelSmall,
                color = Color(0xFF7DD3FC),
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.settings_loorve_pro),
                style = LoorveTypography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = stringResource(R.string.settings_loorve_pro_subtitle),
                style = LoorveTypography.bodySmall,
                color = Color(0xFFBAE6FD)
            )
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xFF0F172A).copy(alpha = 0.26f))
                    .border(1.dp, Color.White.copy(alpha = 0.42f), CircleShape)
                    .clickable(onClick = onClick)
                    .padding(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF0284C7),
                                    Color(0xFF38BDF8),
                                    Color(0xFF7DD3FC)
                                )
                            )
                        )
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isProSubscribed) "구독 중" else "구독",
                        style = LoorveTypography.labelLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsProBadgeLiquid() {
    val transition = rememberInfiniteTransition(label = "settingsProBadge")
    val shift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "settingsProBadgeShift"
    )
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF0B1930),
                        Color(0xFF1E3A8A),
                        Color(0xFF2563EB),
                        Color(0xFF38BDF8)
                    ),
                    start = Offset(shift * 80f, 0f),
                    end = Offset(120f + shift * 80f, 40f)
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
            .drawWithCache {
                val sheenX = size.width * (-0.8f + shift * 2.4f)
                onDrawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.14f),
                                Color.White.copy(alpha = 0.5f),
                                Color.White.copy(alpha = 0.14f),
                                Color.Transparent
                            ),
                            start = Offset(sheenX - size.width, 0f),
                            end = Offset(sheenX, size.height)
                        )
                    )
                }
            }
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            text = "PRO",
            style = LoorveTypography.labelSmall.copy(
                fontSize = 10.5.sp,
                letterSpacing = 0.8.sp
            ),
            color = Color.White,
            fontWeight = FontWeight.ExtraBold
        )
    }
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
        Modifier.fillMaxWidth().padding(vertical = 15.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Notifications,
            contentDescription = null,
            tint = Color(0xFF2563EB),
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(14.dp))
        Text(
            title,
            Modifier.weight(1f),
            style = LoorveTypography.bodyLarge,
            color = OnBackground,
            fontWeight = FontWeight.Medium
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = androidx.compose.material3.SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF2563EB),
                checkedBorderColor = Color(0xFF2563EB),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = SurfaceVariant,
                uncheckedBorderColor = Divider
            )
        )
    }
}

@Composable
private fun NotificationSettingRow(
    notificationTime: String,
    notificationAllowed: Boolean,
    onChangeTime: () -> Unit,
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp, horizontal = 4.dp)
    ) {
        SettingsRow(
            icon = Icons.Default.Schedule,
            title = stringResource(R.string.settings_notification_time),
            subtitle = notificationTime,
            actionLabel = stringResource(R.string.settings_change_notification_time),
            onAction = onChangeTime
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 36.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_notification_permission),
                    style = LoorveTypography.bodyMedium,
                    color = OnBackground
                )
                Text(
                    text = stringResource(
                        if (notificationAllowed) {
                            R.string.settings_notifications_enabled
                        } else {
                            R.string.settings_notification_permission_required
                        }
                    ),
                    style = LoorveTypography.bodySmall,
                    color = OnSurfaceVariant
                )
            }
            if (!notificationAllowed) {
                StatusBadge(
                    text = stringResource(R.string.settings_allow_notifications),
                    containerColor = NoticeContainer,
                    contentColor = Notice,
                    onClick = onRequestPermission
                )
            } else {
                StatusBadge(
                    text = stringResource(R.string.settings_notifications_enabled),
                    containerColor = SuccessContainer,
                    contentColor = Success
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(
    text: String,
    containerColor: Color,
    contentColor: Color,
    onClick: (() -> Unit)? = null
) {
    Text(
        text = text,
        style = LoorveTypography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = contentColor,
        modifier = Modifier
            .clip(CircleShape)
            .background(containerColor)
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 10.dp, vertical = 6.dp)
    )
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
            Text(
                title,
                style = LoorveTypography.bodyLarge,
                color = OnBackground,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = LoorveTypography.bodySmall, color = OnSurfaceVariant)
        }
        actionLabel?.let {
            StatusBadge(
                text = it,
                containerColor = if (actionColor == Error) {
                    WarningContainer
                } else {
                    NoticeContainer
                },
                contentColor = actionColor
            )
        }
    }
}
