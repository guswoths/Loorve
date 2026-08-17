package com.loorve.presentation.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

// 온보딩 각 페이지 데이터 모델
data class OnboardingPage(
    val imageRes: Int,       // drawable 리소스 ID (현재는 placeholder Box로 대체)
    val title: String,
    val description: String
)

// 온보딩 페이지 데이터 목록
private val onboardingPages = listOf(
    OnboardingPage(
        imageRes = 0,
        title = "복습 타이밍, 이제 앱이 계산해드려요",
        description = "공부한 내용과 시험일만 입력하면\n에빙하우스 망각곡선에 맞춰\n복습 일정이 자동으로 완성됩니다."
    ),
    OnboardingPage(
        imageRes = 0,
        title = "진도 기록 한 번으로\n복습 캘린더 완성",
        description = "오늘 배운 내용을 짧게 기록하면\n시험일까지 남은 기간에 맞춰\n최적의 복습 날짜를 자동으로 잡아드려요."
    ),
    OnboardingPage(
        imageRes = 0,
        title = "어디서든 이어서 공부하세요",
        description = "Google 계정으로 로그인하면\n기기를 바꿔도 복습 일정과\n진도 기록이 그대로 유지됩니다."
    )
)

/**
 * 온보딩 호스트 컴포저블
 * @param onOnboardingComplete 온보딩 완료 콜백 ("시작하기" 또는 "건너뛰기" 클릭 시 호출)
 */
@Composable
fun OnboardingScreen(
    onOnboardingComplete: () -> Unit
) {
    // 페이지 수를 람다로 전달 (Compose Foundation HorizontalPager 방식)
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    // 버튼 클릭 시 animateScrollToPage 호출을 위한 코루틴 스코프
    val coroutineScope = rememberCoroutineScope()

    val isLastPage = pagerState.currentPage == onboardingPages.lastIndex

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 건너뛰기 버튼 (마지막 페이지 제외)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            if (!isLastPage) {
                TextButton(onClick = onOnboardingComplete) {
                    Text(text = "건너뛰기")
                }
            }
        }

        // 수평 페이저 (온보딩 3페이지)
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { pageIndex ->
            OnboardingPageContent(page = onboardingPages[pageIndex])
        }

        // 하단 영역: Dot Indicator + 버튼
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 직접 구현한 Dot Indicator
            DotIndicator(
                totalDots = onboardingPages.size,
                selectedIndex = pagerState.currentPage
            )

            // 마지막 페이지 여부에 따라 버튼 분기
            if (isLastPage) {
                // "시작하기" 버튼
                Button(
                    onClick = onOnboardingComplete,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(text = "시작하기", fontSize = 16.sp)
                }
            } else {
                // "다음" 버튼
                Button(
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(text = "다음", fontSize = 16.sp)
                }
            }
        }
    }
}

/**
 * 개별 온보딩 페이지 UI 컴포저블
 * @param page 표시할 OnboardingPage 데이터
 */
@Composable
fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 이미지/일러스트 영역 (실제 imageRes 연동 전 placeholder Box 사용)
        Box(
            modifier = Modifier
                .size(240.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            // TODO: 실제 이미지 리소스 연동 시 Image(painter = painterResource(page.imageRes))로 교체
            Text(
                text = "🖼️",
                fontSize = 64.sp
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        // 타이틀 텍스트
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 설명 텍스트
        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            lineHeight = 24.sp
        )
    }
}

/**
 * 현재 페이지를 나타내는 Dot Indicator (직접 구현)
 * @param totalDots 전체 페이지 수
 * @param selectedIndex 현재 선택된 페이지 인덱스
 */
@Composable
fun DotIndicator(
    totalDots: Int,
    selectedIndex: Int
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalDots) { index ->
            val isSelected = index == selectedIndex
            Box(
                modifier = Modifier
                    .size(
                        width = if (isSelected) 24.dp else 8.dp,
                        height = 8.dp
                    )
                    .clip(CircleShape)
                    .background(
                        color = if (isSelected)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                    )
            )
        }
    }
}
