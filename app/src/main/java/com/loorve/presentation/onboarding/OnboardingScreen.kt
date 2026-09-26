// app/src/main/java/com/loorve/presentation/onboarding/OnboardingScreen.kt

package com.loorve.presentation.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.painterResource
import com.loorve.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.loorve.ui.component.LoorveWordmark
import com.loorve.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

// ──────────────────────────────────────────────────────────
// Stitch 에어리 스카이 팔레트 & 그래디언트
// ──────────────────────────────────────────────────────────
private val AirySkyGradient = Brush.horizontalGradient(
    colors = listOf(
        Color(0xFF0284C7),
        Color(0xFF38BDF8),
        Color(0xFF7DD3FC)
    )
)

private val MiniCtaGradient = Brush.horizontalGradient(
    colors = listOf(
        Color(0xFF0284C7),
        Color(0xFF38BDF8)
    )
)

// ──────────────────────────────────────────────────────────
// 메인 OnboardingScreen — onFinished() 시그니처 유지
// ──────────────────────────────────────────────────────────
@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        containerColor = Color.White,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { _ ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            // 3단 온보딩 수평 페이저 (기본 레이어)
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> OnboardingStep1Content()
                    1 -> OnboardingStep2Content()
                    2 -> OnboardingStep3Content()
                }
            }

            // 상단 우측: 건너뛰기 액션 버튼 (페이저 위에 zIndex(10f)로 배치하여 터치 이벤트 우선 수신)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 2.dp)
                    .align(Alignment.TopEnd)
                    .zIndex(10f),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onFinished,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "건너뛰기",
                        color = Color(0xFF94A3B8),
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // 하단 컨트롤 (인디케이터 점 + CTA 버튼 — 하단 최적 밀착 배치)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 24.dp, end = 24.dp, bottom = 4.dp)
                    .zIndex(10f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 페이지 인디케이터 (현재 페이지 확장 캡슐)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    repeat(3) { index ->
                        val isSelected = pagerState.currentPage == index
                        val width by animateDpAsState(
                            targetValue = if (isSelected) 24.dp else 7.dp,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "indicatorWidth"
                        )
                        Box(
                            modifier = Modifier
                                .size(width = width, height = 7.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) {
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFF0284C7), Color(0xFF38BDF8))
                                        )
                                    } else {
                                        Brush.linearGradient(
                                            listOf(Color(0xFFCBD5E1), Color(0xFFCBD5E1))
                                        )
                                    }
                                )
                        )
                    }
                }

                // Airy Sky Gradient 버튼 (1, 2단계: "다음", 3단계: "시작하기")
                Button(
                    onClick = {
                        if (pagerState.currentPage < 2) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        } else {
                            onFinished()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .shadow(
                            elevation = 6.dp,
                            shape = RoundedCornerShape(25.dp),
                            ambientColor = Color(0xFF0284C7).copy(alpha = 0.35f),
                            spotColor = Color(0xFF38BDF8).copy(alpha = 0.45f)
                        ),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(AirySkyGradient),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (pagerState.currentPage == 2) "시작하기" else "다음",
                                color = Color.White,
                                fontSize = 16.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.2).sp
                            )
                            if (pagerState.currentPage < 2) {
                                Spacer(Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────
// 온보딩 1/3 (에빙하우스 복습일자 자동생성)
// ──────────────────────────────────────────────────────────
@Composable
private fun OnboardingStep1Content() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(start = 24.dp, end = 24.dp, top = 48.dp, bottom = 72.dp),
        horizontalAlignment = Alignment.Start
    ) {
        // Step Badge
        Surface(
            shape = RoundedCornerShape(100.dp),
            color = Color(0xFFE0F2FE).copy(alpha = 0.85f),
            border = BorderStroke(1.dp, Color(0xFFBAE6FD).copy(alpha = 0.8f))
        ) {
            Text(
                text = "SCIENTIFIC REVIEW",
                color = Color(0xFF0284C7),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        // Headline
        Text(
            text = "열심히 공부한 내용,\n며칠 뒤면 잊어버리셨나요?",
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF0F172A),
            lineHeight = 31.sp
        )

        Spacer(Modifier.height(16.dp))

        // Subtitle
        Text(
            text = "에빙하우스 망각 곡선 이론에 맞춰\n최적의 타이밍에 복습 알림을 보내드려요.",
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFF64748B),
            lineHeight = 20.sp
        )

        Spacer(Modifier.height(28.dp))

        // Retention Curve Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE0F2FE)),
            elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // Top Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(10.dp)
                                .height(3.dp)
                                .background(Color(0xFF0284C7), RoundedCornerShape(2.dp))
                        )
                        Text(
                            text = "주기적 복습 곡선",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0284C7)
                        )
                    }

                    Spacer(Modifier.width(14.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(10.dp)
                                .height(2.dp)
                                .background(Color(0xFF94A3B8), RoundedCornerShape(1.dp))
                        )
                        Text(
                            text = "자연 망각 곡선",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Chart Area: Y-Axis labels + Canvas
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(115.dp)
                ) {
                    // Y Axis (100%, 50%, 0%)
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(end = 5.dp, top = 2.dp, bottom = 4.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.End
                    ) {
                        Text("100%", fontSize = 8.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF94A3B8))
                        Text("50%", fontSize = 8.sp, fontWeight = FontWeight.Medium, color = Color(0xFF94A3B8))
                        Text("0%", fontSize = 8.sp, fontWeight = FontWeight.Medium, color = Color(0xFF94A3B8))
                    }

                    // Canvas Curve
                    Canvas(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        val w = size.width
                        val h = size.height

                        val y100 = 6.dp.toPx()
                        val y50 = h * 0.5f
                        val y0 = h - 6.dp.toPx()

                        // Grid dashed lines
                        val gridDash = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                        drawLine(
                            color = Color(0xFFE2E8F0),
                            start = Offset(0f, y100),
                            end = Offset(w, y100),
                            strokeWidth = 0.9.dp.toPx(),
                            pathEffect = gridDash
                        )
                        drawLine(
                            color = Color(0xFFE2E8F0),
                            start = Offset(0f, y50),
                            end = Offset(w, y50),
                            strokeWidth = 0.9.dp.toPx(),
                            pathEffect = gridDash
                        )
                        drawLine(
                            color = Color(0xFFCBD5E1),
                            start = Offset(0f, y0),
                            end = Offset(w, y0),
                            strokeWidth = 0.9.dp.toPx()
                        )

                        // 6 Points X coordinates
                        val x0 = 3.dp.toPx()
                        val x1 = w * 0.17f
                        val x2 = w * 0.36f
                        val x3 = w * 0.58f
                        val x4 = w * 0.79f
                        val x5 = w * 0.96f

                        // Natural Forgetting Curve (Drop)
                        val forgetPath = Path().apply {
                            moveTo(x0, y100)
                            cubicTo(
                                x0 + (x5 - x0) * 0.18f, y50 + 12f,
                                x0 + (x5 - x0) * 0.45f, y0 - 6f,
                                x5, y0
                            )
                        }
                        drawPath(
                            path = forgetPath,
                            color = Color(0xFF94A3B8),
                            style = Stroke(
                                width = 1.3.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                            )
                        )

                        // Retention drop target heights
                        val yDrop1 = y100 + (y0 - y100) * 0.48f
                        val yDrop2 = y100 + (y0 - y100) * 0.38f
                        val yDrop3 = y100 + (y0 - y100) * 0.26f
                        val yDrop4 = y100 + (y0 - y100) * 0.18f
                        val yDrop5 = y100 + (y0 - y100) * 0.10f

                        // Spaced Repetition Filled Area
                        val fillPath = Path().apply {
                            moveTo(x0, y0)
                            lineTo(x0, y100)
                            cubicTo(x0 + (x1 - x0) * 0.45f, y100 + (yDrop1 - y100) * 0.7f, x1 - 4f, yDrop1, x1, yDrop1)
                            lineTo(x1, y100)
                            cubicTo(x1 + (x2 - x1) * 0.45f, y100 + (yDrop2 - y100) * 0.7f, x2 - 4f, yDrop2, x2, yDrop2)
                            lineTo(x2, y100)
                            cubicTo(x2 + (x3 - x2) * 0.45f, y100 + (yDrop3 - y100) * 0.7f, x3 - 4f, yDrop3, x3, yDrop3)
                            lineTo(x3, y100)
                            cubicTo(x3 + (x4 - x3) * 0.45f, y100 + (yDrop4 - y100) * 0.7f, x4 - 4f, yDrop4, x4, yDrop4)
                            lineTo(x4, y100)
                            cubicTo(x4 + (x5 - x4) * 0.45f, y100 + (yDrop5 - y100) * 0.7f, x5 - 3f, yDrop5, x5, yDrop5)
                            lineTo(x5, y0)
                            close()
                        }
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF38BDF8).copy(alpha = 0.35f),
                                    Color(0xFFBAE6FD).copy(alpha = 0.10f),
                                    Color(0x00FFFFFF)
                                )
                            )
                        )

                        // Vertical recovery lines
                        val recoveryDash = PathEffect.dashPathEffect(floatArrayOf(3.5f, 3.5f), 0f)
                        val recoveryColor = Color(0xFF0284C7)
                        drawLine(recoveryColor, Offset(x1, yDrop1), Offset(x1, y100), strokeWidth = 1.1.dp.toPx(), pathEffect = recoveryDash)
                        drawLine(recoveryColor, Offset(x2, yDrop2), Offset(x2, y100), strokeWidth = 1.1.dp.toPx(), pathEffect = recoveryDash)
                        drawLine(recoveryColor, Offset(x3, yDrop3), Offset(x3, y100), strokeWidth = 1.1.dp.toPx(), pathEffect = recoveryDash)
                        drawLine(recoveryColor, Offset(x4, yDrop4), Offset(x4, y100), strokeWidth = 1.1.dp.toPx(), pathEffect = recoveryDash)

                        // Spaced Repetition Dynamic Curve Strokes
                        val curveBrush = Brush.horizontalGradient(
                            listOf(Color(0xFF0284C7), Color(0xFF38BDF8), Color(0xFF0284C7))
                        )
                        val curveStroke = Stroke(width = 2.2.dp.toPx())

                        val curvePath = Path().apply {
                            moveTo(x0, y100)
                            cubicTo(x0 + (x1 - x0) * 0.45f, y100 + (yDrop1 - y100) * 0.7f, x1 - 4f, yDrop1, x1, yDrop1)
                            moveTo(x1, y100)
                            cubicTo(x1 + (x2 - x1) * 0.45f, y100 + (yDrop2 - y100) * 0.7f, x2 - 4f, yDrop2, x2, yDrop2)
                            moveTo(x2, y100)
                            cubicTo(x2 + (x3 - x2) * 0.45f, y100 + (yDrop3 - y100) * 0.7f, x3 - 4f, yDrop3, x3, yDrop3)
                            moveTo(x3, y100)
                            cubicTo(x3 + (x4 - x3) * 0.45f, y100 + (yDrop4 - y100) * 0.7f, x4 - 4f, yDrop4, x4, yDrop4)
                            moveTo(x4, y100)
                            cubicTo(x4 + (x5 - x4) * 0.45f, y100 + (yDrop5 - y100) * 0.7f, x5 - 3f, yDrop5, x5, yDrop5)
                        }
                        drawPath(curvePath, brush = curveBrush, style = curveStroke)

                        // Node Circles
                        val nodes = listOf(
                            Offset(x0, y100),
                            Offset(x1, y100),
                            Offset(x2, y100),
                            Offset(x3, y100),
                            Offset(x4, y100),
                            Offset(x5, yDrop5)
                        )
                        nodes.forEachIndexed { i, pt ->
                            // Outer Halo
                            drawCircle(
                                color = Color(0xFF38BDF8).copy(alpha = if (i == 5) 0.35f else 0.25f),
                                radius = if (i == 5) 7.dp.toPx() else 5.5.dp.toPx(),
                                center = pt
                            )
                            // Inner Solid
                            drawCircle(
                                color = Color(0xFF0284C7),
                                radius = 3.dp.toPx(),
                                center = pt
                            )
                            // White border
                            drawCircle(
                                color = Color.White,
                                radius = 3.dp.toPx(),
                                center = pt,
                                style = Stroke(width = 1.2.dp.toPx())
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Timeline Markers below Canvas
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    TimelineItem(date = "9/12", label = "학습일", isBadge = true, isHighlighted = false)
                    TimelineItem(date = "9/13", label = "1일 뒤", isBadge = false, isHighlighted = false)
                    TimelineItem(date = "9/15", label = "3일 뒤", isBadge = false, isHighlighted = false)
                    TimelineItem(date = "9/19", label = "7일 뒤", isBadge = false, isHighlighted = false)
                    TimelineItem(date = "9/26", label = "14일 뒤", isBadge = false, isHighlighted = false)
                    TimelineItem(date = "10/12", label = "장기기억", isBadge = true, isHighlighted = true)
                }
            }
        }
    }
}

@Composable
private fun TimelineItem(
    date: String,
    label: String,
    isBadge: Boolean,
    isHighlighted: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = date,
            fontSize = 9.5.sp,
            fontWeight = if (isHighlighted) FontWeight.ExtraBold else FontWeight.Bold,
            color = if (isHighlighted) Color(0xFF0284C7) else Color(0xFF1E293B)
        )
        Spacer(Modifier.height(2.dp))
        if (isBadge) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color(0xFFE0F2FE),
                modifier = Modifier.padding(horizontal = 1.dp)
            ) {
                Text(
                    text = label,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0284C7),
                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.5.dp)
                )
            }
        } else {
            Text(
                text = label,
                fontSize = 8.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF64748B)
            )
        }
    }
}

// ──────────────────────────────────────────────────────────
// 온보딩 2/3 (에빙하우스 캘린더 자동생성 애니메이션)
// ──────────────────────────────────────────────────────────
@Composable
private fun OnboardingStep2Content() {
    val infiniteTransition = rememberInfiniteTransition(label = "calendarAutoGeneration")
    val animTimeMs by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 4600f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "animTimeMs"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(start = 24.dp, end = 24.dp, top = 48.dp, bottom = 72.dp),
        horizontalAlignment = Alignment.Start
    ) {
        // Step Badge
        Surface(
            shape = RoundedCornerShape(100.dp),
            color = Color(0xFFF0F9FF),
            border = BorderStroke(1.dp, Color(0xFFBAE6FD).copy(alpha = 0.8f))
        ) {
            Text(
                text = "SMART CALENDAR",
                color = Color(0xFF0284C7),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        // Headline
        Text(
            text = "학습일과 시험일만 등록하세요.\n복습 캘린더가 자동으로\n완성됩니다.",
            fontSize = 20.5.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A),
            lineHeight = 28.sp
        )

        Spacer(Modifier.height(16.dp))

        // Subtitle
        Text(
            text = "시험 날짜에 맞춰 일일 복습량이 자동으로 조절되어\n벼락치기 없이 편안하게 완주할 수 있어요.",
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFF64748B),
            lineHeight = 18.5.sp
        )

        Spacer(Modifier.height(26.dp))

        // Calendar Simulation Card (September 2026)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFBAE6FD).copy(alpha = 0.7f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // Calendar Month Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = null,
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "2026년 9월",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(Modifier.height(2.dp))

                Text(
                    text = "복습 일정이 있는 날짜를 선택해 오늘의 계획을 확인하세요",
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(6.dp))

                // Weekday Headers (일 월 화 수 목 금 토)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    val weekdays = listOf(
                        "일" to Color(0xFFF43F5E),
                        "월" to Color(0xFF475569),
                        "화" to Color(0xFF475569),
                        "수" to Color(0xFF475569),
                        "목" to Color(0xFF475569),
                        "금" to Color(0xFF475569),
                        "토" to Color(0xFF2563EB)
                    )
                    weekdays.forEach { (day, color) ->
                        Text(
                            text = day,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = color,
                            modifier = Modifier.width(32.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                Spacer(Modifier.height(4.dp))

                // Calendar Days Grid (2026년 9월 1일은 화요일, 총 30일)
                val reviewDaysSeq = listOf(4, 7, 10, 13, 16, 18, 19, 21, 22, 24, 25, 26, 27, 28, 29)
                val rows = listOf(
                    listOf(null, null, 1, 2, 3, 4, 5),
                    listOf(6, 7, 8, 9, 10, 11, 12),
                    listOf(13, 14, 15, 16, 17, 18, 19),
                    listOf(20, 21, 22, 23, 24, 25, 26),
                    listOf(27, 28, 29, 30, null, null, null)
                )

                rows.forEach { week ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 1.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        week.forEach { day ->
                            if (day == null) {
                                Box(modifier = Modifier.size(31.dp))
                            } else {
                                CalendarDayCell(
                                    day = day,
                                    seqIndex = reviewDaysSeq.indexOf(day),
                                    animTimeMs = animTimeMs
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    day: Int,
    seqIndex: Int,
    animTimeMs: Float
) {
    val isScheduled = seqIndex >= 0
    val isExamDay = day == 26
    val isRingDot = day in listOf(25, 27, 28, 29)

    val delayMs = if (isScheduled) 200f + seqIndex * 180f else 0f
    val isVisible = isScheduled && (animTimeMs >= delayMs && animTimeMs <= 4100f)

    val scale = if (!isVisible) {
        0f
    } else {
        val elapsed = animTimeMs - delayMs
        if (elapsed < 250f) {
            1f + 0.35f * sin((elapsed / 250f) * PI.toFloat())
        } else {
            1f
        }
    }

    Box(
        modifier = Modifier.size(31.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isExamDay) {
            // 26일: 선택된 목표 시험일
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2563EB)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "$day",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Box(
                        modifier = Modifier
                            .size(3.dp)
                            .scale(scale)
                            .clip(CircleShape)
                            .background(Color(0xFFBAE6FD))
                    )
                }
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "$day",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1E293B)
                )

                Spacer(Modifier.height(1.5.dp))

                // 자동 생성 복습 닷
                if (isScheduled) {
                    if (isRingDot) {
                        Box(
                            modifier = Modifier
                                .size(4.5.dp)
                                .scale(scale)
                                .border(1.dp, Color(0xFF2563EB), CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(4.5.dp)
                                .scale(scale)
                                .clip(CircleShape)
                                .background(Color(0xFF2563EB))
                        )
                    }
                } else {
                    Box(modifier = Modifier.size(4.5.dp))
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────
// 온보딩 3/3 (라이트블루-스카이블루)
// ──────────────────────────────────────────────────────────
@Composable
private fun OnboardingStep3Content() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(start = 24.dp, end = 24.dp, top = 48.dp, bottom = 96.dp),
        horizontalAlignment = Alignment.Start
    ) {
        // Step Badge
        Surface(
            shape = RoundedCornerShape(100.dp),
            color = Color(0xFFE0F2FE),
            border = BorderStroke(1.dp, Color(0xFFBAE6FD).copy(alpha = 0.8f))
        ) {
            Text(
                text = "GET STARTED",
                color = Color(0xFF0284C7),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        // Headline
        Text(
            text = "준비됐나요?\n복습블록을 만들어봐요.",
            fontSize = 23.5.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF0F172A),
            lineHeight = 32.sp
        )

        Spacer(Modifier.height(16.dp))

        // Subtitle
        Text(
            text = "목표 시험이나 공부할 과목을 입력하고,\n오늘부터 진짜 장기 기억을 만들어보세요.",
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFF64748B),
            lineHeight = 20.sp
        )

        Spacer(Modifier.height(12.dp))

        // Brand Header (Loorve App Logo)
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(R.drawable.loorve_wordmark),
                contentDescription = "Loorve",
                modifier = Modifier.width(80.dp)
            )
        }

        Spacer(Modifier.height(10.dp))

        // ──────────────────────────────────────────────────────────
        // 복습 블록 기입 카드 프리뷰 (과목/시험명, 시험일, 복습 주기)
        // ──────────────────────────────────────────────────────────
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFBAE6FD).copy(alpha = 0.85f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(11.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Field 1: 과목 / 시험명
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = "과목 / 시험명",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF64748B)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFBAE6FD), RoundedCornerShape(8.dp))
                            .background(Color.White, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "한국사능력검정시험",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "입력 내용 지우기",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }

                // Field 2: 시험일 & D-Day
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = "시험일",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF64748B)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFBAE6FD), RoundedCornerShape(8.dp))
                            .background(Color.White, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                text = "2026.10.17",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF334155)
                            )
                            Text(
                                text = "(D-21)",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0284C7)
                            )
                        }
                        Icon(
                            imageVector = Icons.Outlined.CalendarToday,
                            contentDescription = "달력",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                // Field 3: 복습 주기
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = "복습 주기",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF64748B)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFBAE6FD), RoundedCornerShape(8.dp))
                            .background(Color.White, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "에빙하우스 망각곡선",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "주기 선택",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // 에빙하우스 선택 옵션 상세 블록
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF0F9FF))
                            .border(1.dp, Color(0xFF0284C7), RoundedCornerShape(8.dp))
                            .padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .border(1.5.dp, Color(0xFF0284C7), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(Color(0xFF0284C7), CircleShape)
                            )
                        }
                        Spacer(Modifier.width(5.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "에빙하우스 망각곡선",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Spacer(Modifier.width(4.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFFE0F2FE)
                                ) {
                                    Text(
                                        text = "기본 권장",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0284C7),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(1.dp))
                            Text(
                                text = "에빙하우스 망각곡선 원리 기반, 시험 일정에 맞춘 복습 주기 자동 배분",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0284C7),
                                lineHeight = 11.5.sp
                            )
                        }
                    }
                }

                // In-Card CTA Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(DeepBlueLightBlueGradient),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "블록 생성하기",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}