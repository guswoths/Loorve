package com.loorve.data.subscription

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.loorve.domain.subscription.LOORVE_PRO_MONTHLY_PRODUCT_ID
import com.loorve.domain.subscription.SubscriptionEntitlement
import com.loorve.domain.subscription.SubscriptionRepository
import com.loorve.domain.subscription.SubscriptionState
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class GooglePlaySubscriptionRepository @Inject constructor(
    @ApplicationContext context: Context
) : SubscriptionRepository, BillingClientStateListener {

    private val _state = MutableStateFlow(SubscriptionState())
    override val state: StateFlow<SubscriptionState> = _state.asStateFlow()
    private var purchaseInProgress = false
    private var connectionInProgress = false

    private val billingClient = BillingClient.newBuilder(context)
        .setListener { result, purchases -> handlePurchaseUpdate(result, purchases) }
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .enableAutoServiceReconnection()
        .build()

    override fun connect() {
        if (billingClient.isReady) {
            refresh()
        } else if (!connectionInProgress) {
            connectionInProgress = true
            _state.value = _state.value.copy(
                entitlement = SubscriptionEntitlement.Loading,
                isBillingReady = false
            )
            billingClient.startConnection(this)
        }
    }

    override fun onBillingSetupFinished(result: BillingResult) {
        connectionInProgress = false
        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
            _state.value = _state.value.copy(isBillingReady = true)
            refresh()
        } else {
            _state.value = SubscriptionState(
                entitlement = SubscriptionEntitlement.Error(result.debugMessage),
                isBillingReady = false
            )
        }
    }

    override fun onBillingServiceDisconnected() {
        connectionInProgress = false
        _state.value = _state.value.copy(
            entitlement = SubscriptionEntitlement.Error("Google Play 결제 서비스에 연결할 수 없습니다."),
            isBillingReady = false
        )
    }

    override fun refresh() {
        if (!billingClient.isReady) {
            connect()
            return
        }

        queryProductDetails()
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        ) { result, purchases ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                _state.value = _state.value.copy(
                    entitlement = SubscriptionEntitlement.Error(result.debugMessage)
                )
                return@queryPurchasesAsync
            }
            handlePurchases(purchases)
        }
    }

    private fun queryProductDetails() {
        billingClient.queryProductDetailsAsync(
            QueryProductDetailsParams.newBuilder()
                .setProductList(
                    listOf(
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(LOORVE_PRO_MONTHLY_PRODUCT_ID)
                            .setProductType(BillingClient.ProductType.SUBS)
                            .build()
                    )
                )
                .build()
        ) { result, details ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val productDetails = details.productDetailsList.firstOrNull()
                _state.value = _state.value.copy(
                    productDetails = productDetails,
                    entitlement = if (productDetails == null) {
                        SubscriptionEntitlement.Error("구독 상품을 불러오지 못했습니다.")
                    } else {
                        _state.value.entitlement
                    }
                )
            } else {
                _state.value = _state.value.copy(
                    entitlement = SubscriptionEntitlement.Error(result.debugMessage)
                )
            }
        }
    }

    override fun launchPurchase(activity: Activity): Boolean {
        if (purchaseInProgress) return false
        val product = _state.value.productDetails ?: return false
        val offer = product.subscriptionOfferDetails?.firstOrNull() ?: return false
        val params = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(product)
            .setOfferToken(offer.offerToken)
            .build()
        val result = billingClient.launchBillingFlow(
            activity,
            BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(params))
                .build()
        )
        purchaseInProgress = result.responseCode == BillingClient.BillingResponseCode.OK
        return result.responseCode == BillingClient.BillingResponseCode.OK
    }

    private fun handlePurchaseUpdate(result: BillingResult, purchases: List<Purchase>?) {
        purchaseInProgress = false
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> handlePurchases(purchases.orEmpty())
            BillingClient.BillingResponseCode.USER_CANCELED -> refresh()
            else -> {
                _state.value = _state.value.copy(
                    entitlement = SubscriptionEntitlement.Error(result.debugMessage)
                )
            }
        }
    }

    private fun handlePurchases(purchases: List<Purchase>) {
        val subscription = purchases.firstOrNull { purchase ->
            purchase.products.contains(LOORVE_PRO_MONTHLY_PRODUCT_ID)
        }
        when {
            subscription?.purchaseState == Purchase.PurchaseState.PENDING -> {
                _state.value = _state.value.copy(entitlement = SubscriptionEntitlement.Pending)
            }
            subscription?.purchaseState == Purchase.PurchaseState.PURCHASED -> {
                if (!subscription.isAcknowledged) {
                    billingClient.acknowledgePurchase(
                        AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(subscription.purchaseToken)
                            .build()
                    ) { result ->
                        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                            _state.value = _state.value.copy(
                                entitlement = SubscriptionEntitlement.Pro
                            )
                        } else {
                            _state.value = _state.value.copy(
                                entitlement = SubscriptionEntitlement.Error(result.debugMessage)
                            )
                        }
                    }
                } else {
                    _state.value = _state.value.copy(
                        entitlement = SubscriptionEntitlement.Pro
                    )
                }
            }
            else -> {
                _state.value = _state.value.copy(
                    entitlement = SubscriptionEntitlement.Free
                )
            }
        }
    }
}
