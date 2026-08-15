package com.loorve

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * LoorveApplication
 *
 * Hilt DI 프레임워크의 진입점.
 * @HiltAndroidApp 어노테이션이 KSP를 통해 Hilt 컴포넌트를 자동 생성한다.
 *
 * AndroidManifest.xml의 android:name=".LoorveApplication"과 반드시 일치해야 함.
 */
@HiltAndroidApp
class LoorveApplication : Application()
