package com.loorve.domain.subscription

import android.app.Activity
import com.android.billingclient.api.ProductDetails
import kotlinx.coroutines.flow.StateFlow

/**
 * Must exactly match the Google Play Console subscription product ID.
 * Configure the monthly-plan and annual-plan auto-renewing base plans in Play Console.
 */
const val LOORVE_PRO_MONTHLY_PRODUCT_ID = "loorve_pro_monthly"
const val LOORVE_PRO_MONTHLY_BASE_PLAN_ID = "monthly-plan"
const val LOORVE_PRO_ANNUAL_BASE_PLAN_ID = "annual-plan"

class SubscriptionRequiredException : IllegalStateException("Loorve Pro 구독이 필요합니다.")

sealed interface SubscriptionEntitlement {
    data object Loading : SubscriptionEntitlement
    data object Free : SubscriptionEntitlement
    data object Pro : SubscriptionEntitlement
    data object Pending : SubscriptionEntitlement
    data class Error(val message: String) : SubscriptionEntitlement
}

data class SubscriptionState(
    val entitlement: SubscriptionEntitlement = SubscriptionEntitlement.Loading,
    val productDetails: ProductDetails? = null,
    val isBillingReady: Boolean = false,
    val lastBillingMessage: String? = null
)

interface SubscriptionRepository {
    val state: StateFlow<SubscriptionState>
    val isProSubscribed: StateFlow<Boolean>

    fun connect()

    fun refresh()

    suspend fun querySubscriptionDetails(
        productId: String = LOORVE_PRO_MONTHLY_PRODUCT_ID
    ): ProductDetails?

    fun launchBillingFlow(
        activity: Activity,
        productDetails: ProductDetails,
        offerToken: String
    ): Boolean

    fun launchPurchase(
        activity: Activity,
        basePlanId: String = LOORVE_PRO_MONTHLY_BASE_PLAN_ID
    ): Boolean
}
