package com.loorve.presentation.subscription

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.loorve.R
import com.loorve.domain.subscription.SubscriptionEntitlement
import com.loorve.domain.subscription.SubscriptionState
import com.loorve.domain.subscription.LOORVE_PRO_ANNUAL_BASE_PLAN_ID
import com.loorve.domain.subscription.LOORVE_PRO_MONTHLY_BASE_PLAN_ID

private val MidnightCanvas = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF0B1930),
        Color(0xFF0F172A),
        Color(0xFF020617)
    )
)
private val CtaGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF0B1930), Color(0xFF1E3A8A), Color(0xFF2563EB), Color(0xFF38BDF8))
)
private val ProGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF1E3A8A), Color(0xFF38BDF8))
)
private val SaleGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF2563EB), Color(0xFF7DD3FC))
)

@Composable
private fun PaywallAmbientBackground() {
    Canvas(modifier = Modifier.fillMaxWidth().height(680.dp)) {
        fun aura(center: Offset, radius: Float, color: Color) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        color.copy(alpha = 0.18f),
                        color.copy(alpha = 0.06f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
        }
        aura(Offset(size.width * 0.04f, size.height * 0.04f), size.minDimension * 0.82f, Color(0xFF0B1930))
        aura(Offset(size.width * 0.98f, size.height * 0.42f), size.minDimension * 0.86f, Color(0xFF1E3A8A))
        aura(Offset(size.width * 0.40f, size.height * 0.98f), size.minDimension * 0.78f, Color(0xFF38BDF8))
    }
}

@Composable
fun ProPaywallDialog(
    viewModel: SubscriptionViewModel,
    onDismiss: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var legalContent by remember { mutableStateOf<LegalContent?>(null) }
    var selectedBasePlanId by remember { mutableStateOf(LOORVE_PRO_ANNUAL_BASE_PLAN_ID) }

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
                .widthIn(min = 300.dp, max = 390.dp)
            .heightIn(max = 760.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0B1930),
                        Color(0xFF0F172A),
                        Color(0xFF020617)
                    )
                )
            )
            .navigationBarsPadding()
        ) {
            PaywallAmbientBackground()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                TopBar(onDismiss = onDismiss)
                HeroHeader()
                SubscriptionContent(state)
                PlanSelector(
                    selectedBasePlanId = selectedBasePlanId,
                    onPlanSelected = { selectedBasePlanId = it }
                )
                Spacer(Modifier.height(18.dp))
                PrimaryAction(
                    state = state,
                    context = context,
                    viewModel = viewModel,
                    selectedBasePlanId = selectedBasePlanId,
                    onDismiss = onDismiss
                )
                LegalFooter(
                    onRestore = {
                        viewModel.refresh()
                        legalContent = LegalContent(
                            title = "구매 내역 복원",
                            body = "이전에 구매한 Loorve Pro 구독 정보를 확인하고 있습니다. 확인이 완료되면 구독 상태가 자동으로 업데이트됩니다."
                        )
                    },
                    onTerms = {
                        legalContent = LegalContent(
                            title = "이용약관",
                            body = "Loorve Pro는 복습 블록과 학습 분석 기능을 제공하는 구독 서비스입니다. 구독은 결제 확인 후 적용되며, 결제 및 갱신은 Google Play 계정 설정에 따라 처리됩니다. 사용자는 언제든지 Google Play 구독 관리에서 갱신을 취소할 수 있습니다."
                        )
                    },
                    onPrivacy = {
                        legalContent = LegalContent(
                            title = "개인정보처리방침",
                            body = "Loorve는 구독 상태 확인과 서비스 제공에 필요한 정보만 처리합니다. 결제 정보는 Google Play가 관리하며 Loorve가 카드 번호를 직접 저장하지 않습니다. 서비스 이용 및 문의에 필요한 정보는 안전하게 보호하고, 법령에 정해진 경우를 제외하고 제3자에게 제공하지 않습니다."
                        )
                    }
                )
            }
        }
    }
    legalContent?.let { content ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { legalContent = null },
            title = { Text(content.title) },
            text = { Text(content.body) },
            confirmButton = {
                TextButton(onClick = { legalContent = null }) {
                    Text("확인")
                }
            }
        )
    }
}

private data class LegalContent(
    val title: String,
    val body: String
)

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
            Text("×", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        }
        Spacer(modifier = Modifier.width(40.dp))
        Spacer(modifier = Modifier.width(40.dp))
    }
}

@Composable
private fun HeroHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
        .padding(top = 4.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(Color(0xFF0B1930).copy(alpha = 0.85f))
            .border(1.dp, Color(0xFF1E3A8A).copy(alpha = 0.6f), CircleShape)
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        Text(
            text = "LOORVE PRO MEMBERSHIP",
            style = androidx.compose.ui.text.TextStyle(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF38BDF8),
                        Color(0xFF7DD3FC)
                    )
                ),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.4.sp
            )
        )
    }
    Spacer(Modifier.height(14.dp))
    Image(
        painter = painterResource(com.loorve.R.mipmap.ic_launcher),
        contentDescription = "Loorve app icon",
        modifier = Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(24.dp))
    )
    Spacer(Modifier.height(14.dp))
    Text(
        text = "효율을 극대화하는",
        color = Color.White,
        fontSize = 28.sp,
        fontWeight = FontWeight.ExtraBold,
        textAlign = TextAlign.Center
    )
    Text(
        text = "최적의 맞춤 복습",
        style = androidx.compose.ui.text.TextStyle(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color(0xFF2563EB),
                    Color(0xFF38BDF8),
                    Color(0xFF7DD3FC)
                )
            ),
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = "에빙하우스 망각곡선 엔진으로 복습 효율을 높이세요.",
        color = Color(0xFFC1C6D6),
        fontSize = 13.sp,
        textAlign = TextAlign.Center
    )
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
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF0B1930).copy(alpha = 0.85f))
                .border(1.dp, Color(0xFF1E3A8A).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ComparisonHeader()
            HorizontalDivider(color = Color.White.copy(alpha = 0.14f))
            ComparisonRow("복습 블록 생성", "최대 1개", "무제한 생성", "시험/목표별 생성 개수")
            ComparisonRow("광고 노출", "광고 노출됨", "완전 제거", "하단 배너 및 팝업 광고")
        }
        when (val entitlement = state.entitlement) {
            SubscriptionEntitlement.Loading -> CircularProgressIndicator(
                color = Color(0xFF38BDF8),
                modifier = Modifier.size(22.dp)
            )
            SubscriptionEntitlement.Pro -> Text(
                stringResource(R.string.pro_active),
                color = Color(0xFF7DD3FC),
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
        Text("핵심 기능", modifier = Modifier.weight(1.4f), color = Color(0xFFC1C6D6), fontSize = 12.sp)
        Text("Basic 무료", modifier = Modifier.weight(1f), color = Color(0xFFC1C6D6), textAlign = TextAlign.Center, fontSize = 11.sp)
        Box(
            modifier = Modifier
                .weight(1.2f)
                .clip(CircleShape)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xFF1E3A8A), Color(0xFF2563EB), Color(0xFF38BDF8))
                    )
                )
                .padding(horizontal = 7.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Loorve Pro", color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontSize = 11.sp)
        }
    }
}

@Composable
private fun ComparisonRow(
    feature: String,
    basic: String,
    pro: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1.4f)) {
            Text(feature, color = Color(0xFFE2E8F0), fontWeight = FontWeight.Medium)
            Text(description, color = Color(0xFF94A3B8), fontSize = 10.sp)
        }
        Text(basic, modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.55f), textAlign = TextAlign.Center)
        Box(
            modifier = Modifier
                .weight(1.2f)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0x292563EB))
                .border(1.dp, Color(0x6638BDF8), RoundedCornerShape(10.dp))
                .padding(horizontal = 5.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("✓ $pro", color = Color(0xFF7DD3FC), fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun PlanSelector(
    selectedBasePlanId: String,
    onPlanSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("플랜 선택", color = Color.White, fontWeight = FontWeight.Bold)
        }
        PlanCard(
            badge = "BEST 16% 할인",
            title = "연간 플랜",
            subtitle = "1년 권장",
            price = "₩29,000/년",
            active = selectedBasePlanId == LOORVE_PRO_ANNUAL_BASE_PLAN_ID,
            onClick = { onPlanSelected(LOORVE_PRO_ANNUAL_BASE_PLAN_ID) }
        )
        PlanCard(
            badge = null,
            title = "월간 플랜",
            subtitle = "정기 결제",
            price = "₩2,900/월",
            active = selectedBasePlanId == LOORVE_PRO_MONTHLY_BASE_PLAN_ID,
            onClick = { onPlanSelected(LOORVE_PRO_MONTHLY_BASE_PLAN_ID) }
        )
    }
}

@Composable
private fun PlanCard(
    badge: String?,
    title: String,
    subtitle: String,
    price: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Box {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (active) Brush.linearGradient(
                        listOf(Color(0x331E3A8A), Color(0x242563EB))
                    ) else Brush.linearGradient(
                        listOf(Color.White.copy(alpha = 0.02f), Color.White.copy(alpha = 0.02f))
                    )
                )
                .border(
                    BorderStroke(
                        if (active) 1.5.dp else 1.dp,
                        if (active) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.08f)
                    ),
                    RoundedCornerShape(16.dp)
                )
                .clickable(onClick = onClick)
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(if (active) Color(0xFF2563EB) else Color.Transparent)
                        .border(2.dp, if (active) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (active) Text("✓", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(9.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    if (subtitle.isNotBlank()) {
                        Text(subtitle, color = Color(0xFF7DD3FC), fontSize = 10.sp)
                    }
                }
                Text(price, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
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
                    .padding(horizontal = 9.dp, vertical = 4.dp),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun PrimaryAction(
    state: SubscriptionState,
    context: android.content.Context,
    viewModel: SubscriptionViewModel,
    selectedBasePlanId: String,
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
                state.productDetails?.subscriptionOfferDetails
                    ?.any { it.basePlanId == selectedBasePlanId } == true
            Button(
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                enabled = productReady,
                onClick = {
                    val productDetails = state.productDetails ?: return@Button
                    val offerToken = productDetails.subscriptionOfferDetails
                        ?.firstOrNull { it.basePlanId == selectedBasePlanId }
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
                    Text(
                        text = "구독하기",
                        color = Color.White,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun LegalFooter(
    onRestore: () -> Unit,
    onTerms: () -> Unit,
    onPrivacy: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(horizontalArrangement = Arrangement.Center) {
        TextButton(onClick = onRestore) {
            Text("구매 내역 복원", color = Color(0xFF64748B), fontSize = 10.5.sp)
        }
        Text("•", modifier = Modifier.padding(top = 12.dp), color = Color(0xFF64748B))
        TextButton(onClick = onTerms) {
            Text("이용약관", color = Color(0xFF64748B), fontSize = 10.5.sp)
        }
        Text("•", modifier = Modifier.padding(top = 12.dp), color = Color(0xFF64748B))
        TextButton(onClick = onPrivacy) {
            Text("개인정보처리방침", color = Color(0xFF64748B), fontSize = 10.5.sp)
        }
        }
    }
}
