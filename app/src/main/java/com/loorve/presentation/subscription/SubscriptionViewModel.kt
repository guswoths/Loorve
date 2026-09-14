package com.loorve.presentation.subscription

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    init {
        repository.connect()
    }

    fun refresh() {
        repository.refresh()
    }

    fun launchPurchase(activity: Activity): Boolean {
        return repository.launchPurchase(activity)
    }

    fun refreshWhenResumed() {
        viewModelScope.launch {
            repository.refresh()
        }
    }
}
