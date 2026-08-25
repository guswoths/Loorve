package com.loorve.presentation.mypage

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable   // ✅ 이 import 추가
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.firebase.auth.FirebaseAuth
import com.loorve.ui.component.*
import com.loorve.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPageScreen(
    onBack: () -> Unit,
    onNavigateToNotificationTimeSetting: () -> Unit,
    onSignOut: () -> Unit,
    viewModel: MyPageViewModel = hiltViewModel()
) {
    val user = FirebaseAuth.getInstance().currentUser
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("MY", style = LoorveTypography.titleLarge, color = OnBackground)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "뒤로가기",
                            tint = OnBackground
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* settings */ }) {
                        Icon(Icons.Default.Settings, null, tint = OnBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        },
        containerColor = Background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 20.dp, bottom = 88.dp)
        ) {
            // 프로필 섹션
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(GradientStart, GradientEnd))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user?.displayName?.firstOrNull()?.toString() ?: "?",
                            style = LoorveTypography.displayLarge,
                            color = Color.White
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = user?.displayName ?: "사용자",
                        style = LoorveTypography.titleMedium,
                        color = OnBackground
                    )
                    Text(
                        text = user?.email ?: "",
                        style = LoorveTypography.bodyMedium,
                        color = OnSurfaceVariant
                    )
                    TextButton(onClick = { /* 프로필 편집 */ }) {
                        Text("프로필 편집", color = Primary)
                    }
                }
            }

            // 통계 카드 Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    listOf(
                        Triple("총 학습일", uiState.totalStudyDays.toString(), "일"),
                        Triple("완료 진도", uiState.completedProgress.toString(), "개"),
                        Triple("등록 시험", uiState.examCount.toString(), "개")
                    ).forEach { (label, value, unit) ->
                        LoorveCard(modifier = Modifier.weight(1f)) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$value$unit",
                                    style = LoorveTypography.titleLarge,
                                    color = Primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = label,
                                    style = LoorveTypography.labelMedium,
                                    color = OnSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // 메뉴 리스트
            item {
                LoorveCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        val menuItems = listOf(
                            Triple(Icons.Default.Notifications, "알림 설정", onNavigateToNotificationTimeSetting),
                            Triple(Icons.Default.Tune, "복습 설정", { }),
                        )
                        menuItems.forEachIndexed { index, (icon, label, action) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .clickable { action() },  // ✅ clickable import 추가로 정상 동작
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(icon, null, tint = OnSurface, modifier = Modifier.size(22.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    label,
                                    style = LoorveTypography.bodyLarge,
                                    color = OnBackground,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(Icons.Default.ChevronRight, null, tint = OnSurfaceVariant)
                            }
                            if (index < menuItems.lastIndex) {
                                Divider(color = SurfaceVariant, thickness = 0.5.dp)
                            }
                        }
                    }
                }
            }

            // 로그아웃 / 계정 삭제
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            viewModel.signOut()
                            onSignOut()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("로그아웃", color = OnSurface, style = LoorveTypography.bodyLarge)
                    }
                    TextButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("계정 삭제", color = Error, style = LoorveTypography.bodyLarge)
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = Surface,
            title = { Text("계정 삭제", color = OnBackground) },
            text = { Text("정말 계정을 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.", color = OnSurface) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteAccount()
                    onSignOut()
                }) { Text("삭제", color = Error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("취소", color = OnSurfaceVariant)
                }
            }
        )
    }
}