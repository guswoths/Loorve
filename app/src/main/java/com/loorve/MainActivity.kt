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
}
