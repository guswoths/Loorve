package com.loorve.presentation.subscription

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.ProductDetails
import com.loorve.domain.subscription.SubscriptionRepository
import com.loorve.domain.subscription.SubscriptionState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val repository: SubscriptionRepository
) : ViewModel() {
    val state: StateFlow<SubscriptionState> = repository.state
    val isProSubscribed: StateFlow<Boolean> = repository.isProSubscribed

    init {
        repository.connect()
    }

    fun refresh() {
        repository.refresh()
    }

    fun launchPurchase(activity: Activity): Boolean {
        return repository.launchPurchase(activity)
    }

    fun querySubscriptionDetails(productId: String = "loorve_pro_monthly") {
        viewModelScope.launch {
            repository.querySubscriptionDetails(productId)
        }
    }

    fun launchBillingFlow(
        activity: Activity,
        productDetails: ProductDetails,
        offerToken: String
    ): Boolean {
        return repository.launchBillingFlow(activity, productDetails, offerToken)
    }

    fun refreshWhenResumed() {
        viewModelScope.launch {
            repository.refresh()
        }
    }
}
