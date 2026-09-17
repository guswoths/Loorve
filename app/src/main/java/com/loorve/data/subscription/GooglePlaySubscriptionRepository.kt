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
import com.android.billingclient.api.QueryProductDetailsResult
import com.android.billingclient.api.QueryPurchasesParams
import com.loorve.domain.subscription.LOORVE_PRO_MONTHLY_PRODUCT_ID
import com.loorve.domain.subscription.SubscriptionEntitlement
import com.loorve.domain.subscription.SubscriptionRepository
import com.loorve.domain.subscription.SubscriptionState
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.coroutines.resume

@Singleton
class GooglePlaySubscriptionRepository @Inject constructor(
    @ApplicationContext context: Context
) : SubscriptionRepository, BillingClientStateListener {

    private val _state = MutableStateFlow(SubscriptionState())
    override val state: StateFlow<SubscriptionState> = _state.asStateFlow()
    private val _isProSubscribed = MutableStateFlow(false)
    override val isProSubscribed: StateFlow<Boolean> = _isProSubscribed.asStateFlow()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var purchaseInProgress = false
    private var connectionInProgress = false

    private val billingClient = BillingClient.newBuilder(context)
        .setListener { result, purchases -> handlePurchaseUpdate(result, purchases) }
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
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
                isBillingReady = false,
                lastBillingMessage = result.debugMessage
            )
        }
    }

    override fun onBillingServiceDisconnected() {
        connectionInProgress = false
        _state.value = _state.value.copy(
            entitlement = SubscriptionEntitlement.Error("Google Play 결제 서비스에 연결할 수 없습니다."),
            isBillingReady = false,
            lastBillingMessage = "Google Play 결제 서비스에 연결할 수 없습니다."
        )
    }

    override fun refresh() {
        if (!billingClient.isReady) {
            connect()
            return
        }

        scope.launch {
            querySubscriptionDetails()
        }
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        ) { result, purchases ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                _state.value = _state.value.copy(
                    entitlement = SubscriptionEntitlement.Error(result.debugMessage),
                    lastBillingMessage = result.debugMessage
                )
                return@queryPurchasesAsync
            }
            handlePurchases(purchases)
        }
    }

    override suspend fun querySubscriptionDetails(productId: String): ProductDetails? {
        if (!billingClient.isReady) {
            connect()
            return null
        }

        return suspendCancellableCoroutine { continuation ->
            billingClient.queryProductDetailsAsync(
                QueryProductDetailsParams.newBuilder()
                    .setProductList(
                        listOf(
                            QueryProductDetailsParams.Product.newBuilder()
                                .setProductId(productId)
                                .setProductType(BillingClient.ProductType.SUBS)
                                .build()
                        )
                    )
                    .build()
            ) { result, details: QueryProductDetailsResult ->
                val productDetails = details.productDetailsList.firstOrNull()
                if (result.responseCode == BillingClient.BillingResponseCode.OK &&
                    productDetails != null
                ) {
                    _state.value = _state.value.copy(
                        productDetails = productDetails,
                        lastBillingMessage = null
                    )
                    continuation.resume(productDetails)
                } else {
                    val message = result.debugMessage.ifBlank {
                        "구독 상품을 불러오지 못했습니다."
                    }
                    _state.value = _state.value.copy(
                        entitlement = SubscriptionEntitlement.Error(message),
                        lastBillingMessage = message
                    )
                    continuation.resume(null)
                }
            }
        }
    }

    override fun launchBillingFlow(
        activity: Activity,
        productDetails: ProductDetails,
        offerToken: String
    ): Boolean {
        if (purchaseInProgress) return false
        if (!billingClient.isReady || offerToken.isBlank()) return false
        val params = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
            .setOfferToken(offerToken)
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

    override fun launchPurchase(activity: Activity): Boolean {
        val product = _state.value.productDetails ?: return false
        val offerToken = product.subscriptionOfferDetails
            ?.firstOrNull()
            ?.offerToken
            ?: return false
        return launchBillingFlow(activity, product, offerToken)
    }

    private fun handlePurchaseUpdate(result: BillingResult, purchases: List<Purchase>?) {
        purchaseInProgress = false
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> handlePurchases(purchases.orEmpty())
            BillingClient.BillingResponseCode.USER_CANCELED -> refresh()
            else -> {
                _state.value = _state.value.copy(
                    entitlement = SubscriptionEntitlement.Error(result.debugMessage),
                    lastBillingMessage = result.debugMessage
                )
            }
        }
    }

    private fun handlePurchases(purchases: List<Purchase>) {
        val subscription = purchases.firstOrNull { purchase ->
            purchase.products.contains(LOORVE_PRO_MONTHLY_PRODUCT_ID) &&
                purchase.purchaseToken.isNotBlank()
        }
        when {
            subscription?.purchaseState == Purchase.PurchaseState.PENDING -> {
                _isProSubscribed.value = false
                _state.value = _state.value.copy(
                    entitlement = SubscriptionEntitlement.Pending,
                    lastBillingMessage = null
                )
            }
            subscription?.purchaseState == Purchase.PurchaseState.PURCHASED -> {
                if (!subscription.isAcknowledged) {
                    billingClient.acknowledgePurchase(
                        AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(subscription.purchaseToken)
                            .build()
                    ) { result ->
                        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                            _isProSubscribed.value = true
                            _state.value = _state.value.copy(
                                entitlement = SubscriptionEntitlement.Pro,
                                lastBillingMessage = null
                            )
                        } else {
                            _isProSubscribed.value = false
                            _state.value = _state.value.copy(
                                entitlement = SubscriptionEntitlement.Error(result.debugMessage),
                                lastBillingMessage = result.debugMessage
                            )
                        }
                    }
                } else {
                    _isProSubscribed.value = true
                    _state.value = _state.value.copy(
                        entitlement = SubscriptionEntitlement.Pro,
                        lastBillingMessage = null
                    )
                }
            }
            else -> {
                _isProSubscribed.value = false
                _state.value = _state.value.copy(
                    entitlement = SubscriptionEntitlement.Free,
                    lastBillingMessage = null
                )
            }
        }
    }
}
