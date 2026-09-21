package com.loorve.presentation.subscription

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.loorve.R
import com.loorve.domain.subscription.SubscriptionEntitlement
import com.loorve.domain.subscription.SubscriptionState

private val MidnightCanvas = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF0D0A22),
        Color(0xFF170D38),
        Color(0xFF0A0C1E)
    )
)
private val CtaGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF2563EB), Color(0xFF7C3AED), Color(0xFFC026D3))
)
private val ProGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF6366F1), Color(0xFFA855F7))
)
private val SaleGradient = Brush.linearGradient(
    colors = listOf(Color(0xFFEC4899), Color(0xFF8B5CF6))
)

@Composable
fun ProPaywallDialog(
    viewModel: SubscriptionViewModel,
    onDismiss: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.querySubscriptionDetails()
        viewModel.refreshWhenResumed()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.96f)
                .background(MidnightCanvas)
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                TopBar(onDismiss = onDismiss)
                HeroHeader()
                SubscriptionContent(state)
                PlanSelector()
                Spacer(Modifier.height(18.dp))
                PrimaryAction(
                    state = state,
                    context = context,
                    viewModel = viewModel,
                    onDismiss = onDismiss
                )
                Text(
                    text = "무료 체험 종료 후 자동 갱신됩니다. 24시간 전 알림 · 해지 수수료 0원",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    color = Color.White.copy(alpha = 0.45f),
                    textAlign = TextAlign.Center
                )
                LegalFooter(
                    onRefresh = viewModel::refresh,
                    onDismiss = onDismiss
                )
            }
        }
    }
}

@Composable
private fun TopBar(onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TextButton(
            onClick = onDismiss,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f)),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
        ) {
            Text("✕", color = Color.White, fontWeight = FontWeight.Bold)
        }
        Text(
            text = "Loorve Pro",
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Text("👤", fontSize = androidx.compose.ui.unit.TextUnit.Unspecified)
        }
    }
}

@Composable
private fun HeroHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "✦ LOORVE PRO MEMBERSHIP",
                color = Color(0xFFC084FC),
                fontWeight = FontWeight.Bold
            )
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
                    .padding(horizontal = 9.dp, vertical = 5.dp)
            ) {
                Text("● 7일 무료", color = Color.White.copy(alpha = 0.8f))
            }
        }
        Spacer(Modifier.height(18.dp))
        Box(
            modifier = Modifier
                .size(78.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(ProGradient)
                .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("✦", color = Color.White, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = "한계를 뛰어넘는\n초개인화 AI 학습",
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "에빙하우스 망각곡선 엔진과 Gemini Pro 취약점 진단으로\n당신에게 꼭 맞는 학습 루틴을 완성하세요.",
            color = Color.White.copy(alpha = 0.75f),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
                .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("복습 달성률 98.4%", color = Color.White, fontWeight = FontWeight.SemiBold)
            Text("|", color = Color.White.copy(alpha = 0.3f))
            Text("기억 보존율 3.2배", color = Color(0xFFC084FC), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SubscriptionContent(state: SubscriptionState) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "핵심 기능",
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Color.White.copy(alpha = 0.04f))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(22.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ComparisonHeader()
            HorizontalDivider(color = Color.White.copy(alpha = 0.14f))
            ComparisonRow("복습 블록 생성", "최대 5개", "무제한 생성")
            ComparisonRow("광고 노출", "광고 있음", "100% 클린")
            ComparisonRow("망각 주기 분석", "기본 (1·7일)", "AI 실시간 맞춤")
            ComparisonRow("심층 메타인지 진단", "✕", "무제한 리포트")
        }
        when (val entitlement = state.entitlement) {
            SubscriptionEntitlement.Loading -> CircularProgressIndicator(
                color = Color(0xFFC084FC),
                modifier = Modifier.size(22.dp)
            )
            SubscriptionEntitlement.Pro -> Text(
                stringResource(R.string.pro_active),
                color = Color(0xFFC084FC),
                fontWeight = FontWeight.SemiBold
            )
            SubscriptionEntitlement.Pending -> Text(
                stringResource(R.string.pro_pending),
                color = Color.White.copy(alpha = 0.75f)
            )
            SubscriptionEntitlement.Free -> Unit
            is SubscriptionEntitlement.Error -> Text(
                stringResource(R.string.pro_error, entitlement.message),
                color = Color(0xFFFFB4AB)
            )
        }
    }
}

@Composable
private fun ComparisonHeader() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("핵심 기능", modifier = Modifier.weight(1.4f), color = Color.White.copy(alpha = 0.75f))
        Text("Basic 무료", modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.45f), textAlign = TextAlign.Center)
        Box(
            modifier = Modifier
                .weight(1.2f)
                .clip(CircleShape)
                .background(ProGradient)
                .padding(horizontal = 7.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("✪ Loorve Pro", color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun ComparisonRow(feature: String, basic: String, pro: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(feature, modifier = Modifier.weight(1.4f), color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(basic, modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.55f), textAlign = TextAlign.Center)
        Box(
            modifier = Modifier
                .weight(1.2f)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0x387C3AED))
                .border(1.dp, Color(0x4DC084FC), RoundedCornerShape(10.dp))
                .padding(horizontal = 5.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("✓ $pro", color = Color(0xFFE9D5FF), fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun PlanSelector() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("플랜 선택", color = Color.White, fontWeight = FontWeight.Bold)
            Text("언제든지 해지 가능", color = Color.White.copy(alpha = 0.45f))
        }
        PlanCard(
            badge = "🔥 BEST 45% 할인",
            title = "연간 플랜",
            subtitle = "1년 권장 · 연 ₩70,800 결제",
            price = "₩5,900 / 월",
            active = true
        )
        PlanCard(
            badge = null,
            title = "월간 플랜",
            subtitle = "정기 결제 · 부담 없이 매월",
            price = "₩10,900 / 월",
            active = false
        )
    }
}

@Composable
private fun PlanCard(
    badge: String?,
    title: String,
    subtitle: String,
    price: String,
    active: Boolean
) {
    Box {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(
                    if (active) Brush.linearGradient(
                        listOf(Color(0x297C3AED), Color(0x142563EB))
                    ) else Brush.linearGradient(
                        listOf(Color.White.copy(alpha = 0.02f), Color.White.copy(alpha = 0.02f))
                    )
                )
                .border(
                    BorderStroke(
                        if (active) 1.5.dp else 1.dp,
                        if (active) Color(0xFFA855F7) else Color.White.copy(alpha = 0.08f)
                    ),
                    RoundedCornerShape(18.dp)
                )
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(if (active) Color(0xFFA855F7) else Color.Transparent)
                        .border(2.dp, if (active) Color(0xFFA855F7) else Color.White.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (active) Text("✓", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(11.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, color = Color.White, fontWeight = FontWeight.Bold)
                    Text(subtitle, color = Color.White.copy(alpha = 0.55f))
                }
                Text(price, color = Color.White, fontWeight = FontWeight.ExtraBold)
            }
        }
        if (badge != null) {
            Text(
                text = badge,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(y = (-11).dp)
                    .clip(CircleShape)
                    .background(SaleGradient)
                    .padding(horizontal = 11.dp, vertical = 5.dp),
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun PrimaryAction(
    state: SubscriptionState,
    context: android.content.Context,
    viewModel: SubscriptionViewModel,
    onDismiss: () -> Unit
) {
    when (state.entitlement) {
        SubscriptionEntitlement.Pro -> {
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
            ) {
                Text(stringResource(R.string.pro_close), color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        else -> {
            val productReady = state.isBillingReady &&
                state.productDetails?.subscriptionOfferDetails?.isNotEmpty() == true
            Button(
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                enabled = productReady,
                onClick = {
                    val productDetails = state.productDetails ?: return@Button
                    val offerToken = productDetails.subscriptionOfferDetails
                        ?.firstOrNull()
                        ?.offerToken
                        ?: return@Button
                    (context as? Activity)?.let { activity ->
                        viewModel.launchBillingFlow(
                            activity = activity,
                            productDetails = productDetails,
                            offerToken = offerToken
                        )
                    }
                },
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(CircleShape)
                        .background(CtaGradient),
                    contentAlignment = Alignment.Center
                ) {
                    Text("⚡ 7일 무료 체험 후 시작하기", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun LegalFooter(
    onRefresh: () -> Unit,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        TextButton(onClick = onRefresh) {
            Text("구매 내역 복원", color = Color.White.copy(alpha = 0.65f))
        }
        Text("•", modifier = Modifier.padding(top = 12.dp), color = Color.White.copy(alpha = 0.3f))
        TextButton(onClick = onDismiss) {
            Text("이용약관", color = Color.White.copy(alpha = 0.65f))
        }
        Text("•", modifier = Modifier.padding(top = 12.dp), color = Color.White.copy(alpha = 0.3f))
        TextButton(onClick = onDismiss) {
            Text("개인정보처리방침", color = Color.White.copy(alpha = 0.65f))
        }
    }
}
