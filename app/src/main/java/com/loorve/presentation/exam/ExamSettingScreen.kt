package com.loorve.presentation.exam

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.loorve.ui.component.GradientButton
import com.loorve.ui.theme.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar

@Composable
fun ExamSettingScreen(
    onSaveSuccess: () -> Unit,
    viewModel: ExamSettingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy년 MM월 dd일") }

    // ──────── ONE-SHOT 이벤트 (기존 로직 유지) ────────
    var navigated by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ExamSettingEvent.SaveSuccess -> {
                    if (!navigated) {
                        navigated = true
                        onSaveSuccess()
                    }
                }
            }
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    // ──────── 시험일 DatePickerDialog (기존 로직 유지) ────────
    val examCalendar = remember { Calendar.getInstance() }
    val examDatePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            examCalendar.set(year, month, dayOfMonth, 0, 0, 0)
            examCalendar.set(Calendar.MILLISECOND, 0)
            viewModel.onExamDateSelected(examCalendar.timeInMillis)
        },
        examCalendar.get(Calendar.YEAR),
        examCalendar.get(Calendar.MONTH),
        examCalendar.get(Calendar.DAY_OF_MONTH)
    )

    // ──────── 학습 종료일 DatePickerDialog (기존 로직 유지) ────────
    val studyEndCalendar = remember { Calendar.getInstance() }
    val studyEndDatePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            studyEndCalendar.set(year, month, dayOfMonth, 0, 0, 0)
            studyEndCalendar.set(Calendar.MILLISECOND, 0)
            viewModel.onStudyEndDateSelected(studyEndCalendar.timeInMillis)
        },
        studyEndCalendar.get(Calendar.YEAR),
        studyEndCalendar.get(Calendar.MONTH),
        studyEndCalendar.get(Calendar.DAY_OF_MONTH)
    )

    // 에빙하우스/직접설정 선택 상태 (UI 전용, 로직 무변경)
    var useEbbinghaus by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 48.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ── 헤더 ──
            Text(
                text = "복습 블록 생성하기",
                style = LoorveTypography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = OnBackground
            )
            Text(
                text = "시험 정보와 복습 주기를 정하면 블록이 자동 생성됩니다.",
                style = LoorveTypography.bodyMedium,
                color = OnSurfaceVariant
            )

            // ── 블록 생성 안내 카드 ──
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "블록 생성 전 확인",
                        style = LoorveTypography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = Primary
                    )
                    Text(
                        text = "시험 종료일까지의 기간에 맞춰 복습 간격이 자동으로 압축되거나 조정됩니다.",
                        style = LoorveTypography.bodySmall,
                        color = OnSurfaceVariant
                    )
                }
            }

            // ── 과목명 입력 ──
            OutlinedTextField(
                value = uiState.subjectName,
                onValueChange = { viewModel.onSubjectNameChange(it) },
                label = { Text("시험 이름", color = OnSurfaceVariant) },
                placeholder = { Text("한국사능력검정 심화", color = OnSurfaceVariant.copy(alpha = 0.5f)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = OnBackground,
                    unfocusedTextColor = OnBackground,
                    cursorColor = Primary,
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = SurfaceVariant,
                    focusedLabelColor = Primary,
                    unfocusedLabelColor = OnSurfaceVariant
                )
            )

            // ── 시험일 선택 ──
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "시험 종료일",
                    style = LoorveTypography.labelMedium,
                    color = OnSurfaceVariant
                )
                OutlinedButton(
                    onClick = { examDatePickerDialog.show() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (uiState.examDate != 0L) Primary else SurfaceVariant
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Surface,
                        contentColor = OnBackground
                    )
                ) {
                    val displayText = if (uiState.examDate != 0L) {
                        Instant.ofEpochMilli(uiState.examDate)
                            .atZone(ZoneId.of("Asia/Seoul"))
                            .toLocalDate()
                            .format(dateFormatter)
                    } else {
                        "날짜 선택"
                    }
                    Text(
                        text = displayText,
                        color = if (uiState.examDate != 0L) OnBackground else OnSurfaceVariant
                    )
                }
            }

            // ── 학습 종료일 선택 ──
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "학습 종료일 (선택사항)",
                    style = LoorveTypography.labelMedium,
                    color = OnSurfaceVariant
                )
                OutlinedButton(
                    onClick = { studyEndDatePickerDialog.show() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (uiState.studyEndDate != 0L) Primary else SurfaceVariant
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Surface,
                        contentColor = OnBackground
                    )
                ) {
                    val displayText = if (uiState.studyEndDate != 0L) {
                        Instant.ofEpochMilli(uiState.studyEndDate)
                            .atZone(ZoneId.of("Asia/Seoul"))
                            .toLocalDate()
                            .format(dateFormatter)
                    } else {
                        "날짜 선택"
                    }
                    Text(
                        text = displayText,
                        color = if (uiState.studyEndDate != 0L) OnBackground else OnSurfaceVariant
                    )
                }
                uiState.studyEndDateError?.let { errorMsg ->
                    Text(
                        text = errorMsg,
                        style = LoorveTypography.bodySmall,
                        color = Error
                    )
                }
            }

            // ── 복습 주기 선택 (에빙하우스 / 직접설정) ──
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 에빙하우스 옵션
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (useEbbinghaus) Primary.copy(alpha = 0.08f) else Color.Transparent
                            )
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "에빙하우스 망각주기",
                                style = LoorveTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = if (useEbbinghaus) Primary else OnBackground
                            )
                            Text(
                                text = "1일 · 3일 · 7일 · 14일 · 30일 자동 배치",
                                style = LoorveTypography.bodySmall,
                                color = OnSurfaceVariant
                            )
                        }
                        RadioButton(
                            selected = useEbbinghaus,
                            onClick = { useEbbinghaus = true },
                            colors = RadioButtonDefaults.colors(selectedColor = Primary)
                        )
                    }

                    Divider(color = SurfaceVariant, thickness = 0.5.dp)

                    // 직접 설정 옵션
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (!useEbbinghaus) Primary.copy(alpha = 0.08f) else Color.Transparent
                            )
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "직접 세팅",
                                style = LoorveTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = if (!useEbbinghaus) Primary else OnBackground
                            )
                            Text(
                                text = "복습 간격을 직접 지정해 나만의 블록 구성",
                                style = LoorveTypography.bodySmall,
                                color = OnSurfaceVariant
                            )
                        }
                        RadioButton(
                            selected = !useEbbinghaus,
                            onClick = { useEbbinghaus = false },
                            colors = RadioButtonDefaults.colors(selectedColor = Primary)
                        )
                    }
                }
            }

            // ── D-Day 표시 (기존 로직 유지, 스타일만 변경) ──
            if (uiState.dDayText.isNotEmpty()) {
                val dDayColor = when {
                    uiState.dDayText == "D-Day"        -> Error
                    uiState.dDayText.startsWith("D+")  -> OnSurfaceVariant
                    else                                -> Primary
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = uiState.dDayText,
                            style = LoorveTypography.displayMedium.copy(fontWeight = FontWeight.Bold),
                            color = dDayColor
                        )
                        Text(
                            text = "시험까지 ${uiState.dDayText}",
                            style = LoorveTypography.bodySmall,
                            color = OnSurfaceVariant
                        )
                    }
                }
            }

            // ── 배너 광고 플레이스홀더 ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceVariant.copy(alpha = 0.5f))
                    .border(1.dp, SurfaceVariant, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "배너 광고 320 × 50",
                    style = LoorveTypography.labelSmall,
                    color = OnSurfaceVariant.copy(alpha = 0.5f)
                )
            }

            // ── 저장 버튼 ──
            GradientButton(
                text = "저장",
                onClick = { viewModel.saveExam() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !uiState.isLoading &&
                        uiState.subjectName.isNotBlank() &&
                        uiState.examDate != 0L &&
                        uiState.studyEndDateError == null
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}