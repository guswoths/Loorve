package com.loorve.presentation.subscription

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Surface
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
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "• 결제 즉시 청구되며, 무료 체험 기간은 제공되지 않습니다.\n• 구독 만료 전 취소하지 않으면 동일한 조건으로 자동 갱신됩니다.",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.5.sp,
                    lineHeight = 16.5.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                )
                Spacer(Modifier.height(14.dp))
                PrimaryAction(
                    state = state,
                    context = context,
                    viewModel = viewModel,
                    selectedBasePlanId = selectedBasePlanId,
                    onDismiss = onDismiss
                )
                LegalFooter(
                    onManageSubscription = {
                        legalContent = LegalContent(
                            title = "구독 해지 및 관리 안내",
                            body = "구독 해지 및 관리는 Google Play 스토어의 [결제 및 정기 결제]에서 언제든지 직접 진행하실 수 있습니다.\n\n구독 기간 만료 전 언제든지 해지하실 수 있으며, 해지하더라도 남은 구독 기간 동안은 Loorve Pro 혜택이 정상 유지됩니다.",
                            isSubscriptionManagement = true
                        )
                    },
                    onRestore = {
                        viewModel.refresh()
                        legalContent = LegalContent(
                            title = "구매 내역 복원",
                            body = "이전에 구매한 Loorve Pro 구독 정보를 확인하고 있습니다. 확인이 완료되면 구독 상태가 자동으로 업데이트됩니다."
                        )
                    },
                    onTerms = {
                        legalContent = LegalContent(
                            title = "서비스 이용약관",
                            body = """
[Loorve Pro 서비스 이용약관]

제1조 (유료 서비스 및 구독 안내)
1. Loorve Pro는 결제 즉시 기능이 활성화되며, 무료 체험 기간은 제공되지 않습니다.
2. 현재 구독 기간이 만료되기 전까지 Google Play 스토어에서 직접 해지하지 않는 한, 동일한 금액과 주기로 자동 갱신 청구됩니다.

제2조 (청약철회 및 환불 조건 - Google Play 환불 정책 연동)
1. 본 서비스는 Google Play 결제 시스템을 통해 처리되며, Google Play의 환불 정책을 준수합니다. 구매 후 48시간 이내에는 Google Play 웹사이트 또는 고객센터를 통해 환불 요청이 가능합니다.
2. 본 유료 서비스는 결제 즉시 디지털 콘텐츠의 이용이 개시되므로, 전자상거래 등에서의 소비자보호에 관한 법률 제17조 제2항에 따라 이미 이용이 개시된 당월(당기) 구독 기간에 대한 중도 일할 계산 환불은 원칙적으로 제공되지 않습니다.
3. 정기결제를 해지하더라도 이미 결제된 남은 구독 기간 동안은 Pro 혜택이 정상 유지되며, 다음 결제일부터 추가 요금이 청구되지 않습니다.
4. 회사의 귀책 사유로 영구적으로 서비스를 제공할 수 없게 된 경우 잔여 기간에 대해 적법한 절차에 따라 환불 처리됩니다.

제3조 (서비스 중단 및 이용 제한 규정)
1. 회사는 이용자가 다음 각 호에 해당하는 경우 사전 통지 없이 서비스 이용을 제한하거나 계정을 정지할 수 있습니다.
   • 타인의 결제 수단 또는 계정 정보를 도용한 경우
   • 비정상적인 방법으로 결제를 진행하거나 시스템을 해킹, 변조하는 경우
   • 서비스의 정상적인 운영을 방해하거나 관련 법령 및 본 약관을 위반한 경우
2. 정기 점검, 시스템 교체, 통신망 장애 등 불가피한 사유 발생 시 서비스 제공이 일시적으로 중단될 수 있습니다.
                            """.trimIndent()
                        )
                    },
                    onPrivacy = {
                        legalContent = LegalContent(
                            title = "개인정보처리방침",
                            body = """
[Loorve 개인정보처리방침]

• 공식 웹페이지 전문:
  https://living-nitrogen-cfb.notion.site/3d8da8c67899800cb78be40cd9efdf97

제1조 (수집하는 개인정보 항목 및 이용 목적)
1. Google 로그인 수집 항목:
   • 수집 항목: Google 계정 고유 식별자(UID), 이메일 주소, 프로필 이름(닉네임), 프로필 사진 URL
   • 수집 목적: 회원 식별, 계정 연동 및 로그인 관리
2. 광고 SDK(AdMob 등)를 통한 광고 ID(AD_ID) 수집 고지:
   • 수집 항목: Google 광고 ID(AD_ID / GAID), 기기 모델, OS 버전, 네트워크 상태
   • 수집 목적: 맞춤형 광고 송출, 비정상 트래픽 감지 및 앱 서비스 품질 분석
   • 맞춤형 광고 거부 방법: Android 설정 > 보안 및 개인정보 보호 > 개인정보 보호 > 광고 > 광고 ID 재설정 또는 삭제

제2조 (결제/구매 내역 기록 목적 및 보관 기간)
1. 수집 항목: Google Play 주문 번호(Order ID), 구매 일시, 구독 상품 ID(SKU)
   (주의: 신용카드 번호 등 실제 결제 금융 정보는 Google LLC가 직접 처리하며 Loorve 앱 및 서버에 일체 수집·저장되지 않습니다.)
2. 기록 목적: 유료 구독 권한 부여, 결제 내역 확인 및 고객 지원
3. 보유 및 보관 기간:
   • 회원 탈퇴 시 즉시 파기 원칙
   • 전자상거래 등에서의 소비자보호에 관한 법률에 따른 법정 의무 보관:
     - 계약 또는 청약철회 등에 관한 기록: 5년
     - 대금결제 및 재화 등의 공급에 관한 기록: 5년
     - 소비자의 불만 또는 분쟁처리에 관한 기록: 3년

제3조 (개인정보 보호책임자 및 고객 문의 창구)
• 상호: guswoths
• 개인정보 보호책임자(CPO): 손현재 (대표)
• 공식 문의 이메일: hjson7585@gmail.com
                            """.trimIndent(),
                            webUrl = "https://living-nitrogen-cfb.notion.site/3d8da8c67899800cb78be40cd9efdf97"
                        )
                    }
                )
            }
        }
    }
    legalContent?.let { content ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { legalContent = null },
            title = {
                Text(
                    text = content.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (content.webUrl != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFEFF6FF),
                            border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    runCatching {
                                        context.startActivity(
                                            Intent(Intent.ACTION_VIEW, Uri.parse(content.webUrl))
                                        )
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "🌐 웹페이지에서 전문 보기",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF1D4ED8)
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = content.webUrl,
                                        fontSize = 11.sp,
                                        color = Color(0xFF3B82F6),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = "열기 ➔",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2563EB)
                                )
                            }
                        }
                    }

                    Text(
                        text = content.body,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        color = Color(0xFF334155)
                    )
                }
            },
            confirmButton = {
                if (content.isSubscriptionManagement) {
                    Button(
                        onClick = {
                            val playStoreUri = Uri.parse("https://play.google.com/store/account/subscriptions")
                            val intent = Intent(Intent.ACTION_VIEW, playStoreUri).apply {
                                setPackage("com.android.vending")
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                context.startActivity(Intent(Intent.ACTION_VIEW, playStoreUri))
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                    ) {
                        Text("Google Play 정기결제 관리", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                } else if (content.webUrl != null) {
                    Button(
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(content.webUrl))
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                    ) {
                        Text("웹페이지 이동", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                } else {
                    TextButton(onClick = { legalContent = null }) {
                        Text("확인", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                if (content.isSubscriptionManagement || content.webUrl != null) {
                    TextButton(onClick = { legalContent = null }) {
                        Text("닫기")
                    }
                }
            }
        )
    }
}

private data class LegalContent(
    val title: String,
    val body: String,
    val isSubscriptionManagement: Boolean = false,
    val webUrl: String? = null
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
            ComparisonRow("복습 블록 생성", "1개", "무제한 생성", "시험/목표별 생성 개수")
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
            badge = "16% 할인",
            title = "연간 플랜",
            price = "₩29,000/년",
            active = selectedBasePlanId == LOORVE_PRO_ANNUAL_BASE_PLAN_ID,
            onClick = { onPlanSelected(LOORVE_PRO_ANNUAL_BASE_PLAN_ID) }
        )
        PlanCard(
            badge = null,
            title = "월간 플랜",
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
    price: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Box(
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
            .padding(horizontal = 14.dp, vertical = 15.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
            Spacer(Modifier.width(10.dp))
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                if (badge != null) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(SaleGradient)
                            .padding(horizontal = 7.dp, vertical = 2.5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = badge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.5.sp,
                            letterSpacing = (-0.2).sp
                        )
                    }
                }
            }
            Text(
                text = price,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp
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
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
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
                        text = stringResource(R.string.pro_close),
                        color = Color.White,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
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
    onManageSubscription: () -> Unit,
    onRestore: () -> Unit,
    onTerms: () -> Unit,
    onPrivacy: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onManageSubscription,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "구독 해지 및 관리",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text("•", color = Color(0xFF475569), fontSize = 10.sp)
            TextButton(
                onClick = onRestore,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "구매 내역 복원",
                    color = Color(0xFF64748B),
                    fontSize = 11.sp
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onTerms,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "서비스 이용약관",
                    color = Color(0xFF64748B),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Text("•", color = Color(0xFF475569), fontSize = 10.sp)
            TextButton(
                onClick = onPrivacy,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "개인정보처리방침",
                    color = Color(0xFF64748B),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
