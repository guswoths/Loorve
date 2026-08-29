// 경로: app/src/main/java/com/loorve/presentation/calendar/AddReviewBlockScreen.kt
package com.loorve.presentation.calendar

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.loorve.ui.component.BannerAdView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReviewBlockScreen(
    onNavigateBack: () -> Unit,
    onSaveSuccess: () -> Unit = {}
) {
    // UI 상태만 관리 (기능 로직 없음)
    var examName by remember { mutableStateOf("") }
    var examDate by remember { mutableStateOf("") }
    // 0 = 에빙하우스, 1 = 직접 세팅
    var selectedCycleOption by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("복습 블록 생성하기") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기"
                        )
                    }
                }
            )
        },
        bottomBar = {
            Column {
                // 하단 고정 버튼
                Button(
                    onClick = onSaveSuccess,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .height(52.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text  = "블록 생성하기",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
                // 배너 광고
                BannerAdView(modifier = Modifier.fillMaxWidth())
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // 본문 설명 텍스트
            Text(
                text  = "시험 정보와 복습 주기를 정하면 블록이 자동 생성됩니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // 안내 카드
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text  = "💡",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 1.dp, end = 8.dp)
                    )
                    Column {
                        Text(
                            text  = "블록 생성 전 확인",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text  = "시험 종료일까지의 기간에 맞춰 복습 간격이 자동으로 압축되거나 조정됩니다.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            // 시험 이름 입력
            OutlinedTextField(
                value         = examName,
                onValueChange = { examName = it },
                label         = { Text("시험 이름") },
                placeholder   = { Text("한국사능력검정 심화") },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true
            )

            // 시험 종료일 입력
            OutlinedTextField(
                value         = examDate,
                onValueChange = { examDate = it },
                label         = { Text("시험 종료일") },
                placeholder   = { Text("2026년 9월 12일") },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true
            )

            // 복습 주기 선택
            Text(
                text  = "복습 주기",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
            )

            // 옵션 0: 에빙하우스 망각주기
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors   = CardDefaults.cardColors(
                    containerColor = if (selectedCycleOption == 0)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surface
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedCycleOption == 0,
                        onClick  = { selectedCycleOption = 0 }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text  = "에빙하우스 망각주기",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text  = "1일 · 3일 · 7일 · 14일 · 30일 자동 배치",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 옵션 1: 직접 세팅
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors   = CardDefaults.cardColors(
                    containerColor = if (selectedCycleOption == 1)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surface
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedCycleOption == 1,
                        onClick  = { selectedCycleOption = 1 }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text  = "직접 세팅",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text  = "복습 간격을 직접 지정해 나만의 블록 구성",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}