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

        try {
            val info = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageManager.getPackageInfo(packageName, android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, android.content.pm.PackageManager.GET_SIGNATURES)
            }

            val signatures = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                val signingInfo = info.signingInfo
                if (signingInfo != null) {
                    if (signingInfo.hasMultipleSigners()) {
                        signingInfo.apkContentsSigners
                    } else {
                        signingInfo.signingCertificateHistory
                    }
                } else {
                    null
                }
            } else {
                @Suppress("DEPRECATION")
                info.signatures
            }

            if (signatures != null) {
                for (signature in signatures) {
                    val md = java.security.MessageDigest.getInstance("SHA-1")
                    md.update(signature.toByteArray())
                    val digest = md.digest()
                    val hexString = digest.joinToString(":") { String.format("%02X", it) }
                    android.util.Log.e("REAL_SHA1", "실제 런타임 SHA-1: $hexString")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

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