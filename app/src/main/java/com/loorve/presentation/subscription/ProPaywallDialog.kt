package com.loorve.presentation.subscription

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.loorve.R
import com.loorve.domain.subscription.SubscriptionEntitlement
import com.loorve.domain.subscription.SubscriptionState
import com.loorve.ui.theme.AuroraViolet

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

    AlertDialog(
        modifier = Modifier
            .widthIn(min = 380.dp, max = 440.dp)
            .heightIn(min = 420.dp),
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF18113C),
        titleContentColor = Color.White,
        textContentColor = Color(0xFFD8D5E8),
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                text = stringResource(R.string.pro_title),
                fontWeight = FontWeight.ExtraBold
            )
        },
        text = {
            SubscriptionContent(state)
        },
        confirmButton = {
            when (state.entitlement) {
                SubscriptionEntitlement.Pro -> {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.pro_close)) }
                }
                else -> {
                    val productReady = state.isBillingReady &&
                        state.productDetails?.subscriptionOfferDetails?.isNotEmpty() == true
                    Button(
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AuroraViolet,
                            contentColor = Color.White
                        ),
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
                        }
                    ) {
                        Text(
                            state.productDetails?.subscriptionOfferDetails
                                ?.firstOrNull()
                                ?.pricingPhases
                                ?.pricingPhaseList
                                ?.firstOrNull()
                                ?.formattedPrice
                                ?.let { stringResource(R.string.pro_subscribe, it) }
                                ?: stringResource(R.string.pro_subscribe_unavailable)
                        )
                    }
                }
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFC4B5FD)),
                    onClick = viewModel::refresh
                ) {
                    Text(stringResource(R.string.pro_refresh))
                }
                Spacer(Modifier.width(4.dp))
                TextButton(
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFC4B5FD)),
                    onClick = onDismiss
                ) {
                    Text(stringResource(R.string.pro_cancel))
                }
            }
        }
    )
}

@Composable
private fun SubscriptionContent(state: SubscriptionState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Choose the plan that fits your review routine.",
            color = Color(0xFFD8D5E8)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    BorderStroke(1.dp, Color(0x66C4B5FD)),
                    RoundedCornerShape(18.dp)
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "FEATURES",
                    modifier = Modifier.weight(1.8f),
                    color = Color(0xFFC4B5FD),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "BASIC",
                    modifier = Modifier.weight(1f),
                    color = Color(0xFFB8B8C8),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "PRO",
                    modifier = Modifier.weight(1f),
                    color = Color(0xFFE9D5FF),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
            ComparisonRow(
                feature = "Unlimited Review Block Generation\n무제한 복습블록 생성",
                basic = "Limited",
                pro = "Unlimited"
            )
            ComparisonRow(
                feature = "No Banner Ads\n배너광고 없음",
                basic = "Ads",
                pro = "No banner ads"
            )
        }
        when (val entitlement = state.entitlement) {
            SubscriptionEntitlement.Loading -> CircularProgressIndicator(color = Color(0xFFC4B5FD))
            SubscriptionEntitlement.Pro -> Text(
                stringResource(R.string.pro_active),
                color = Color(0xFFE9D5FF),
                fontWeight = FontWeight.SemiBold
            )
            SubscriptionEntitlement.Pending -> Text(
                stringResource(R.string.pro_pending),
                color = Color(0xFFD8D5E8)
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
private fun ComparisonRow(
    feature: String,
    basic: String,
    pro: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = feature,
            modifier = Modifier.weight(1.8f),
            color = Color.White
        )
        Text(
            text = basic,
            modifier = Modifier.weight(1f),
            color = Color(0xFFB8B8C8),
            textAlign = TextAlign.Center
        )
        Text(
            text = pro,
            modifier = Modifier.weight(1f),
            color = Color(0xFFE9D5FF),
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
    }
}
