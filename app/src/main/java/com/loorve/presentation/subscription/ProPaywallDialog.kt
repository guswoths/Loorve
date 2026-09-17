package com.loorve.presentation.subscription

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.loorve.R
import com.loorve.domain.subscription.SubscriptionEntitlement
import com.loorve.domain.subscription.SubscriptionState

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
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pro_title)) },
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
                TextButton(onClick = viewModel::refresh) {
                    Text(stringResource(R.string.pro_refresh))
                }
                Spacer(Modifier.width(4.dp))
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.pro_cancel)) }
            }
        }
    )
}

@Composable
private fun SubscriptionContent(state: SubscriptionState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(stringResource(R.string.pro_free_benefits))
        Text(stringResource(R.string.pro_paid_benefits))
        when (val entitlement = state.entitlement) {
            SubscriptionEntitlement.Loading -> CircularProgressIndicator()
            SubscriptionEntitlement.Pro -> Text(stringResource(R.string.pro_active))
            SubscriptionEntitlement.Pending -> Text(stringResource(R.string.pro_pending))
            SubscriptionEntitlement.Free -> Unit
            is SubscriptionEntitlement.Error -> Text(
                stringResource(R.string.pro_error, entitlement.message)
            )
        }
    }
}
