package com.loorve

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.loorve.presentation.navigation.LoorveNavHost
import com.loorve.ui.theme.LoorveTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LoorveTheme {
                LoorveNavHost()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // NotificationPermissionRoute 내부에서 LaunchedEffect + lifecycle 기반으로
        // 재확인하므로 여기서는 Activity 재생성 없이도 안전합니다.
    }
}