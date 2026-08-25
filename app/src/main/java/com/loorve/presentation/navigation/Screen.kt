package com.loorve.presentation.navigation

sealed class Screen(val route: String) {
    object Home       : Screen("home")
    object Calendar   : Screen("calendar")
    object Exam       : Screen("exam")
    object MyPage     : Screen("mypage")
    object Login      : Screen("login")
    object Onboarding : Screen("onboarding")
    data class ProgressDetail(val progressId: String) : Screen("progress/{progressId}") {
        fun createRoute(id: String) = "progress/$id"
    }
}