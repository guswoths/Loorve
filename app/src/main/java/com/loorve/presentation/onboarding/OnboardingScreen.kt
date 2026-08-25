// app/src/main/java/com/loorve/presentation/onboarding/OnboardingScreen.kt

package com.loorve.presentation.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loorve.ui.component.GradientButton
import com.loorve.ui.theme.*

// ──────────────────────────────────────────────────────────
// 목업 기준 온보딩 페이지 데이터
// ──────────────────────────────────────────────────────────
private data class OnboardingPage(
    val stepLabel: String,
    val title: String,
    val description: String,
    val showEbbinghausCard: Boolean = false
)

private val pages = listOf(
    OnboardingPage(
        stepLabel = "ONBOARDING 1 / 3",
        title = "Loorve에 오신 걸 환영해요",
        description = "시험일 기반으로 복습 블록을 자동으로 생성해드립니다.\n에빙하우스 망각 곡선 기반으로 최적 타이밍에 알려드려요."
    ),
    OnboardingPage(
        stepLabel = "ONBOARDING 2 / 3",
        title = "시험 이름과 종료일만 잡으면,\n복습 블록이 자동으로 깔립니다.",
        description = "기본은 에빙하우스 주기, 필요하면 직접 주기를 선택할 수 있습니다.",
        showEbbinghausCard = true
    ),
    OnboardingPage(
        stepLabel = "ONBOARDING 3 / 3",
        title = "준비됐나요?",
        description = "복습 블록을 만들고, 오늘부터 조금씩 오래 기억해보세요."
    )
)

// 에빙하우스 간격 칩 데이터
private val ebbinghausIntervals = listOf("1일", "3일", "7일", "14일", "30일")

// ──────────────────────────────────────────────────────────
// 메인 OnboardingScreen — onFinished() 시그니처 유지
// ──────────────────────────────────────────────────────────
@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { pages.size })

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // 건너뛰기 버튼 (기능 유지)
        TextButton(
            onClick = onFinished,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 16.dp)
        ) {
            Text("건너뛰기", color = OnSurfaceVariant, fontSize = 14.sp)
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            OnboardingPageContent(page = pages[page])
        }

        // 하단 영역 (Dot 인디케이터 + 시작 버튼)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 40.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Dot 인디케이터 (기존 로직 유지, 스타일만 목업에 맞춤)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(pages.size) { index ->
                    Box(
                        modifier = Modifier
                            .size(
                                width = if (pagerState.currentPage == index) 24.dp else 8.dp,
                                height = 8.dp
                            )
                            .clip(CircleShape)
                            .background(
                                if (pagerState.currentPage == index) Primary else OnSurfaceVariant.copy(alpha = 0.4f)
                            )
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // 마지막 페이지에서만 시작 버튼 표시 (기존 기능 유지)
            if (pagerState.currentPage == pages.lastIndex) {
                GradientButton(
                    text = "시작하기",
                    onClick = onFinished,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────
// 페이지 내부 콘텐츠 Composable
// ──────────────────────────────────────────────────────────
@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp)
            .padding(top = 72.dp, bottom = 160.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top
    ) {
        // 상단 스텝 뱃지 (목업: "ONBOARDING 2 / 3")
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Primary.copy(alpha = 0.15f),
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            Text(
                text = page.stepLabel,
                color = Primary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.8.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
            )
        }

        // 페이지 제목
        Text(
            text = page.title,
            style = LoorveTypography.titleLarge.copy(
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 32.sp
            ),
            color = OnBackground,
            textAlign = TextAlign.Start
        )

        Spacer(Modifier.height(12.dp))

        // 페이지 설명
        Text(
            text = page.description,
            style = LoorveTypography.bodyLarge.copy(lineHeight = 24.sp),
            color = OnSurfaceVariant,
            textAlign = TextAlign.Start
        )

        // 에빙하우스 카드 (2페이지에만 표시, 목업 기준)
        if (page.showEbbinghausCard) {
            Spacer(Modifier.height(28.dp))
            EbbinghausCard()
        }
    }
}

// ──────────────────────────────────────────────────────────
// 에빙하우스 복습 흐름 카드
// ──────────────────────────────────────────────────────────
@Composable
private fun EbbinghausCard() {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Surface.copy(alpha = 0.6f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "자동 배치되는 복습 흐름",
                style = LoorveTypography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = OnBackground
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "학습 후 1일 · 3일 · 7일 · 14일 · 30일 간격을\n시험일에 맞춰 조정합니다.",
                style = LoorveTypography.bodySmall.copy(lineHeight = 20.sp),
                color = OnSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))

            // 간격 칩 Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ebbinghausIntervals.forEach { interval ->
                    IntervalChip(label = interval)
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────
// 간격 칩 컴포넌트
// ──────────────────────────────────────────────────────────
@Composable
private fun IntervalChip(label: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Primary.copy(alpha = 0.12f),
        modifier = Modifier.border(
            width = 1.dp,
            color = Primary.copy(alpha = 0.3f),
            shape = RoundedCornerShape(8.dp)
        )
    ) {
        Text(
            text = label,
            color = Primary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}