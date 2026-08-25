package com.loorve.presentation.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.loorve.ui.component.GradientButton
import com.loorve.ui.theme.*

private data class OnboardingPage(
    val title: String,
    val description: String,
    val emoji: String
)

private val pages = listOf(
    OnboardingPage("스마트 복습", "학습 내용을 체계적으로 기록하고\n에빙하우스 망각 곡선으로 복습하세요", "📚"),
    OnboardingPage("D-Day 관리", "시험일을 등록하면 D-Day를\n실시간으로 추적해드립니다", "🎯"),
    OnboardingPage("진도 추적", "학습 진도를 시각화하여\n목표 달성을 쉽게 확인하세요", "📈")
)

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { pages.size })

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // 건너뛰기 버튼
        TextButton(
            onClick = onFinished,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Text("건너뛰기", color = OnSurfaceVariant)
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = pages[page].emoji,
                    style = LoorveTypography.displayLarge.copy(fontSize = androidx.compose.ui.unit.TextUnit(72f, androidx.compose.ui.unit.TextUnitType.Sp))
                )
                Spacer(Modifier.height(32.dp))
                Text(
                    text = pages[page].title,
                    style = LoorveTypography.titleLarge,
                    color = OnBackground,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = pages[page].description,
                    style = LoorveTypography.bodyLarge,
                    color = OnSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        // 하단 영역
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 40.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Dot 인디케이터
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(pages.size) { index ->
                    Box(
                        modifier = Modifier
                            .size(if (pagerState.currentPage == index) 20.dp else 8.dp, 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (pagerState.currentPage == index) Primary else OnSurfaceVariant
                            )
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

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