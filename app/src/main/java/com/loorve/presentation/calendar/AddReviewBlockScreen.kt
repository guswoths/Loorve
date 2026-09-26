// 경로: app/src/main/java/com/loorve/presentation/calendar/AddReviewBlockScreen.kt
package com.loorve.presentation.calendar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import com.loorve.ui.theme.DeepBlueLightBlueGradient
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.loorve.domain.review.ReviewSchedulingEngine
import com.loorve.domain.subscription.SubscriptionEntitlement
import com.loorve.presentation.reviewblock.ReviewBlockUiState
import com.loorve.presentation.reviewblock.ReviewBlockViewModel
import com.loorve.presentation.subscription.ProPaywallDialog
import com.loorve.presentation.subscription.SubscriptionViewModel
import com.loorve.ui.component.BannerAdView
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReviewBlockScreen(
    onNavigateBack: () -> Unit,
    onSaveSuccess: () -> Unit,
    reviewBlockViewModel: ReviewBlockViewModel = hiltViewModel(),
    subscriptionViewModel: SubscriptionViewModel = hiltViewModel()
) {
    val currentUid = remember { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid }
    var examName by rememberSaveable { mutableStateOf("") }
    var examDateMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedCycleOption by rememberSaveable { mutableStateOf(0) } // 0: 에빙하우스 망각곡선, 1: 직접세팅
    var isCycleDropdownExpanded by rememberSaveable { mutableStateOf(false) }
    var customIntervalText by rememberSaveable { mutableStateOf("") }
    var appliedCustomIntervalDays by rememberSaveable { mutableStateOf<Int?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showProDialog by remember { mutableStateOf(false) }
    var isExamNameFocused by remember { mutableStateOf(false) }
    var isCustomIntervalFocused by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val uiState by reviewBlockViewModel.uiState.collectAsState()
    val subscriptionState by subscriptionViewModel.state.collectAsState()

    // 키보드 높이 감지
    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)

    // 키보드가 올라올 때 포커스된 필드에 따라 스크롤 조절
    // - 과목/시험명: 살짝만 위로 올라가도록 조절하여 입력란이 시야에서 벗어나지 않게 함
    // - 직접세팅: 입력란 및 하단 버튼들이 키보드에 가려지지 않게 끝까지 올려줌
    LaunchedEffect(imeBottom, isCustomIntervalFocused, isExamNameFocused) {
        if (imeBottom > 0) {
            delay(50)
            if (isCustomIntervalFocused) {
                scrollState.animateScrollTo(scrollState.maxValue)
            } else if (isExamNameFocused) {
                val slightScrollPx = with(density) { 50.dp.roundToPx() }
                scrollState.animateScrollTo(slightScrollPx.coerceAtMost(scrollState.maxValue))
            }
        }
    }

    // Success 시 resetState() 후 onSaveSuccess()
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is ReviewBlockUiState.Success -> {
                reviewBlockViewModel.resetState()
                onSaveSuccess()
            }
            is ReviewBlockUiState.Error -> {
                snackbarHostState.showSnackbar(
                    message = "저장에 실패했습니다: ${state.message}",
                    duration = SnackbarDuration.Long
                )
                reviewBlockViewModel.resetState()
            }
            ReviewBlockUiState.RequiresPro -> {
                showProDialog = true
            }
            else -> Unit
        }
    }

    val examDateFormatted = examDateMillis?.let { millis ->
        Instant.ofEpochMilli(millis)
            .atZone(ZoneId.of("Asia/Seoul"))
            .toLocalDate()
            .format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))
    }

    val dDayText = examDateMillis?.let { millis ->
        val examDate = Instant.ofEpochMilli(millis).atZone(ZoneId.of("Asia/Seoul")).toLocalDate()
        val today = LocalDate.now(ZoneId.of("Asia/Seoul"))
        val daysBetween = ChronoUnit.DAYS.between(today, examDate)
        when {
            daysBetween > 0 -> "(D-$daysBetween)"
            daysBetween == 0L -> "(D-Day)"
            else -> "(D+${-daysBetween})"
        }
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = examDateMillis
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        examDateMillis = datePickerState.selectedDateMillis
                        appliedCustomIntervalDays = null
                        showDatePicker = false
                    }
                ) { Text("확인") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("취소") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showProDialog) {
        ProPaywallDialog(
            viewModel = subscriptionViewModel,
            onDismiss = {
                showProDialog = false
                reviewBlockViewModel.resetState()
            }
        )
    }

    val isLoading = uiState is ReviewBlockUiState.Loading
    val isUidReady = !currentUid.isNullOrBlank()

    Scaffold(
        containerColor = Color(0xFFF8FAFC),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "복습 블록 생성하기",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, enabled = !isLoading) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기",
                            tint = Color(0xFF1E293B)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        bottomBar = {
            // 키보드가 올라와 있을 때는 배너광고를 숨겨서 화면 가림 방지
            val showBanner = (subscriptionState.entitlement is SubscriptionEntitlement.Free) && (imeBottom == 0)
            if (showBanner) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    key(showBanner) {
                        BannerAdView(modifier = Modifier.fillMaxWidth())
                    }
                }
            } else {
                Spacer(modifier = Modifier.navigationBarsPadding())
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .padding(horizontal = 20.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Header Info (스마트복습플랜 뱃지 제거)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "새로운 복습 블록을\n설계해보세요.",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    lineHeight = 28.sp
                )

                Text(
                    text = "과목과 시험일을 설정하면 망각곡선 최적 주기로 자동 생성됩니다.",
                    fontSize = 13.sp,
                    color = Color(0xFF64748B),
                    lineHeight = 18.sp
                )
            }

            // Tip Banner
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF0F9FF),
                border = BorderStroke(1.dp, Color(0xFFBAE6FD).copy(alpha = 0.7f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "💡", fontSize = 14.sp, modifier = Modifier.padding(end = 8.dp))
                    Text(
                        text = "시험일까지 남은 일수에 맞춰 복습 일정이 균형있게 자동 배치됩니다.",
                        fontSize = 12.sp,
                        color = Color(0xFF0369A1),
                        lineHeight = 17.sp
                    )
                }
            }

            // ──────────────────────────────────────────────────────────
            // Google Stitch MCP: "첫 복습 블록 생성 프리뷰 카드 단독 화면" 블록 디자인
            // ──────────────────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFBAE6FD).copy(alpha = 0.8f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Field 1: 과목 / 시험명
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = "과목 / 시험명",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B)
                        )
                        BasicTextField(
                            value = examName,
                            onValueChange = { examName = it },
                            enabled = !isLoading,
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { focusState ->
                                    isExamNameFocused = focusState.isFocused
                                    if (focusState.isFocused && imeBottom > 0) {
                                        coroutineScope.launch {
                                            delay(50)
                                            val slightScrollPx = with(density) { 50.dp.roundToPx() }
                                            scrollState.animateScrollTo(slightScrollPx.coerceAtMost(scrollState.maxValue))
                                        }
                                    }
                                },
                            textStyle = TextStyle(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B),
                                letterSpacing = (-0.2).sp
                            ),
                            decorationBox = { innerTextField ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            width = 1.dp,
                                            color = Color(0xFFBAE6FD),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .background(Color.White, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 14.dp, vertical = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        if (examName.isEmpty()) {
                                            Text(
                                                text = "정보처리기사 실기",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Normal,
                                                color = Color(0xFF94A3B8)
                                            )
                                        }
                                        innerTextField()
                                    }
                                    if (examName.isNotEmpty() && !isLoading) {
                                        IconButton(
                                            onClick = { examName = "" },
                                            modifier = Modifier.size(18.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "입력 내용 지우기",
                                                tint = Color(0xFF94A3B8),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        )
                    }

                    // Field 2: 시험일 & D-Day
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = "시험일",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 1.dp,
                                    color = Color(0xFFBAE6FD),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .background(Color.White, RoundedCornerShape(12.dp))
                                .clickable(enabled = !isLoading) { showDatePicker = true }
                                .padding(horizontal = 14.dp, vertical = 11.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (examDateFormatted != null && dDayText != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = examDateFormatted,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF334155)
                                    )
                                    Text(
                                        text = dDayText,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0284C7)
                                    )
                                }
                            } else {
                                Text(
                                    text = "시험일을 선택해주세요",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                            Icon(
                                imageVector = Icons.Outlined.CalendarToday,
                                contentDescription = "시험일 선택 달력 열기",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Field 3: 복습 주기
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = "복습 주기",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B)
                        )

                        // 복습 주기 선택 상자 (누르면 하단 선택란 토글)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 1.dp,
                                    color = Color(0xFFBAE6FD),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .background(Color.White, RoundedCornerShape(12.dp))
                                .clickable(enabled = !isLoading) {
                                    isCycleDropdownExpanded = !isCycleDropdownExpanded
                                }
                                .padding(horizontal = 14.dp, vertical = 11.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val cycleText = if (selectedCycleOption == 0) {
                                "에빙하우스 망각곡선"
                            } else {
                                if (appliedCustomIntervalDays != null) {
                                    "직접세팅 (${appliedCustomIntervalDays}일 간격)"
                                } else {
                                    "직접세팅"
                                }
                            }

                            Text(
                                text = cycleText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B),
                                letterSpacing = (-0.2).sp
                            )

                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "복습 주기 변경",
                                tint = Color(0xFF64748B),
                                modifier = Modifier
                                    .size(24.dp)
                                    .rotate(if (isCycleDropdownExpanded) 180f else 0f)
                            )
                        }

                        // 복습 주기 선택 펼침 영역 (클릭해도 없어지지 않고 선택 상태 유지)
                        AnimatedVisibility(
                            visible = isCycleDropdownExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                                    .background(Color(0xFFF8FAFC), RoundedCornerShape(14.dp))
                                    .border(
                                        width = 1.dp,
                                        color = Color(0xFFBAE6FD).copy(alpha = 0.8f),
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // 옵션 1: 에빙하우스 망각곡선 (클릭 시 닫히지 않고 선택 유지)
                                val isEbbinghaus = selectedCycleOption == 0
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isEbbinghaus) Color(0xFFF0F9FF) else Color.White)
                                        .border(
                                            width = if (isEbbinghaus) 1.5.dp else 1.dp,
                                            color = if (isEbbinghaus) Color(0xFF0284C7) else Color(0xFFE2E8F0),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable(enabled = !isLoading) {
                                            selectedCycleOption = 0
                                        }
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isEbbinghaus,
                                        onClick = {
                                            selectedCycleOption = 0
                                        },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = Color(0xFF0284C7),
                                            unselectedColor = Color(0xFF94A3B8)
                                        ),
                                        enabled = !isLoading
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "에빙하우스 망각곡선",
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0F172A)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = Color(0xFFE0F2FE)
                                            ) {
                                                Text(
                                                    text = "기본 권장",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF0284C7),
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = "에빙하우스 망각곡선 원리 기반, 시험 일정에 맞춘 복습 주기 자동 배분",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF0284C7),
                                            lineHeight = 16.sp
                                        )
                                    }
                                }

                                // 옵션 2: 직접세팅 (클릭 시 선택 유지)
                                val isCustom = selectedCycleOption == 1
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isCustom) Color(0xFFF0F9FF) else Color.White)
                                        .border(
                                            width = if (isCustom) 1.5.dp else 1.dp,
                                            color = if (isCustom) Color(0xFF0284C7) else Color(0xFFE2E8F0),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable(enabled = !isLoading) {
                                            selectedCycleOption = 1
                                        }
                                        .padding(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = isCustom,
                                            onClick = { selectedCycleOption = 1 },
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = Color(0xFF0284C7),
                                                unselectedColor = Color(0xFF94A3B8)
                                            ),
                                            enabled = !isLoading
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "직접세팅",
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0F172A)
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "원하는 복습 주기 간격(일)을 직접 설정",
                                                fontSize = 11.5.sp,
                                                color = Color(0xFF64748B)
                                            )
                                        }
                                    }

                                    // 직접세팅 입력 컨트롤
                                    if (isCustom) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 6.dp, end = 4.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                BasicTextField(
                                                    value = customIntervalText,
                                                    onValueChange = { value ->
                                                        if (value.isEmpty() || value.all(Char::isDigit)) {
                                                            customIntervalText = value
                                                            appliedCustomIntervalDays = null
                                                        }
                                                    },
                                                    enabled = !isLoading,
                                                    singleLine = true,
                                                    keyboardOptions = KeyboardOptions(
                                                        keyboardType = KeyboardType.Number
                                                    ),
                                                    modifier = Modifier.onFocusChanged { focusState ->
                                                        isCustomIntervalFocused = focusState.isFocused
                                                        if (focusState.isFocused && imeBottom > 0) {
                                                            coroutineScope.launch {
                                                                delay(50)
                                                                scrollState.animateScrollTo(scrollState.maxValue)
                                                            }
                                                        }
                                                    },
                                                    textStyle = TextStyle(
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF1E293B)
                                                    ),
                                                    decorationBox = { innerTextField ->
                                                        Box(
                                                            modifier = Modifier
                                                                .width(80.dp)
                                                                .background(Color.White, RoundedCornerShape(8.dp))
                                                                .border(1.dp, Color(0xFFBAE6FD), RoundedCornerShape(8.dp))
                                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                                            contentAlignment = Alignment.CenterStart
                                                        ) {
                                                            if (customIntervalText.isEmpty()) {
                                                                Text(
                                                                    text = "예: 5",
                                                                    fontSize = 13.sp,
                                                                    color = Color(0xFF94A3B8)
                                                                )
                                                            }
                                                            innerTextField()
                                                        }
                                                    }
                                                )
                                                Text(
                                                    text = "일 간격",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFF334155)
                                                )
                                                Spacer(modifier = Modifier.weight(1f))
                                                Button(
                                                    onClick = {
                                                        val interval = customIntervalText.toIntOrNull()
                                                        val examDate = examDateMillis?.let {
                                                            Instant.ofEpochMilli(it)
                                                                .atZone(ZoneId.of("Asia/Seoul"))
                                                                .toLocalDate()
                                                        }
                                                        if (examDate == null) {
                                                            coroutineScope.launch {
                                                                snackbarHostState.showSnackbar("시험일을 먼저 선택해주세요.")
                                                            }
                                                        } else {
                                                            val validation = ReviewSchedulingEngine.validateCustomReviewInterval(
                                                                interval,
                                                                LocalDate.now(ZoneId.of("Asia/Seoul")),
                                                                examDate
                                                            )
                                                            if (validation.isValid) {
                                                                appliedCustomIntervalDays = interval
                                                                coroutineScope.launch {
                                                                    snackbarHostState.showSnackbar("복습 간격이 적용되었습니다.")
                                                                }
                                                            } else {
                                                                coroutineScope.launch {
                                                                    snackbarHostState.showSnackbar(
                                                                        validation.message ?: "복습 간격을 확인해주세요."
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    },
                                                    enabled = !isLoading && customIntervalText.isNotBlank(),
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = if (appliedCustomIntervalDays != null) Color(0xFF0284C7) else Color(0xFF0EA5E9),
                                                        disabledContainerColor = Color(0xFFBAE6FD)
                                                    ),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                    modifier = Modifier.height(36.dp)
                                                ) {
                                                    if (appliedCustomIntervalDays != null) {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = "적용됨",
                                                            tint = Color.White,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("적용 완료", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                    } else {
                                                        Text("간격 적용", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                    }
                                                }
                                            }
                                            Text(
                                                text = "1이상의 숫자를 입력하세요. 시험일까지 최소 2회의 복습 일정이 필요합니다.",
                                                fontSize = 11.sp,
                                                color = Color(0xFF64748B),
                                                lineHeight = 15.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // In-Card CTA: 블록 생성하기
                    Button(
                        onClick = {
                            val uid = currentUid
                            when {
                                uid.isNullOrBlank() -> {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("로그인 정보를 확인 중입니다. 잠시 후 다시 시도해주세요.")
                                    }
                                }
                                examName.isBlank() -> {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("과목 / 시험명을 입력해주세요.")
                                    }
                                }
                                examDateMillis == null -> {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("시험일을 선택해주세요.")
                                    }
                                }
                                selectedCycleOption == 1 && appliedCustomIntervalDays == null -> {
                                    isCycleDropdownExpanded = true
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("복습 간격을 적용해주세요.")
                                    }
                                }
                                else -> {
                                    val safeExamDateMillis = examDateMillis ?: return@Button
                                    reviewBlockViewModel.createReviewBlock(
                                        uid = uid,
                                        examName = examName.trim(),
                                        examDateMillis = safeExamDateMillis,
                                        cycleOption = selectedCycleOption,
                                        customIntervalDays = appliedCustomIntervalDays
                                    )
                                }
                            }
                        },
                        enabled = !isLoading && isUidReady,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            disabledContainerColor = Color(0xFFE2E8F0)
                        ),
                        contentPadding = PaddingValues(0.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 2.dp,
                            pressedElevation = 0.dp
                        )
                    ) {
                        val canCreateBlock = !isLoading && isUidReady
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = if (canCreateBlock) {
                                        DeepBlueLightBlueGradient
                                    } else {
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFFCBD5E1), Color(0xFFE2E8F0))
                                        )
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = "블록 생성하기",
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}