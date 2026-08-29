package com.loorve.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.loorve.data.local.OnboardingPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SplashDestination {
    data object Loading : SplashDestination
    data object Home : SplashDestination
    data object Login : SplashDestination
    data object Onboarding : SplashDestination
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val onboardingPreferences: OnboardingPreferences
) : ViewModel() {

    private val _destination = MutableStateFlow<SplashDestination>(SplashDestination.Loading)
    val destination: StateFlow<SplashDestination> = _destination

    fun resolveDestination() {
        viewModelScope.launch {
            val isOnboardingComplete = onboardingPreferences.isOnboardingComplete.first()
            val isLoggedIn = FirebaseAuth.getInstance().currentUser != null

            _destination.value = when {
                !isOnboardingComplete -> SplashDestination.Onboarding  // 최초 유저
                isLoggedIn -> SplashDestination.Home                   // 기존 유저, 로그인됨
                else -> SplashDestination.Login                        // 기존 유저, 재로그인 필요
            }
        }
    }
}