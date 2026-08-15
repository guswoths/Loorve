package com.loorve

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * LoorveApplication
 *
 * @HiltAndroidApp 어노테이션으로 Hilt DI 컨테이너 초기화.
 * AndroidManifest.xml의 android:name=".LoorveApplication"과 반드시 일치해야 합니다.
 */
@HiltAndroidApp
class LoorveApplication : Application()
