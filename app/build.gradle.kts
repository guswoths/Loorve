// ─────────────────────────────────────────────
// 파일: app/build.gradle.kts
// Kotlin DSL 기반 Android 앱 빌드 스크립트
// ─────────────────────────────────────────────
import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// ── Keystore 서명 설정 로드 (보안: keystore.properties는 .gitignore에 반드시 추가) ──
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) {
        load(keystorePropsFile.inputStream())
    }
}

// ── 플러그인 선언: 버전 카탈로그(libs.versions.toml) alias 참조 ──
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)   // Compose 컴파일러 플러그인 (Kotlin 2.x+)
    alias(libs.plugins.ksp)             // ✅ kapt 완전 제거, KSP만 사용
    alias(libs.plugins.hilt)            // Hilt DI 플러그인
    alias(libs.plugins.google.services) // Firebase google-services.json 처리
}

// ── Android 빌드 설정 ──
android {
    namespace = "com.loorve"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.loorve"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // ── Release 서명 설정: keystorePropsFile 존재 여부 체크 후 적용 ──
    signingConfigs {
        create("release") {
            val storeFilePath = keystoreProps.getProperty("storeFile")
            if (!storeFilePath.isNullOrBlank()) {
                storeFile = file(storeFilePath)
            }
            storePassword = keystoreProps.getProperty("storePassword")
            keyAlias     = keystoreProps.getProperty("keyAlias")
            keyPassword  = keystoreProps.getProperty("keyPassword")
        }
    }

    // ── 빌드 타입: debug / release 분리 ──
    buildTypes {
        debug {
            applicationIdSuffix = ".debug"  // debug 빌드는 .debug suffix로 분리
            isDebuggable = true
        }
        release {
            isMinifyEnabled    = true       // 코드 난독화 및 축소
            isShrinkResources  = true       // 미사용 리소스 제거
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // keystorePropsFile 존재 시에만 서명 적용 (CI 환경 호환)
            if (keystorePropsFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    // ── Java / Kotlin 컴파일 타겟 ──
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // ── 빌드 기능 활성화 ──
    buildFeatures {
        compose      = true  // Jetpack Compose 활성화
        buildConfig  = true  // BuildConfig 클래스 생성 (환경 변수 접근용)
    }
}

// ── Kotlin 컴파일러 옵션 ──
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

// ── 의존성 선언 ──
dependencies {

    // ── [Compose] BOM으로 버전 통합 관리 ──
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)       // UI 그래픽 레이어
    implementation(libs.compose.ui.tooling.preview) // 프리뷰 지원 (런타임 경량)
    implementation(libs.compose.material3)          // Material You 디자인 시스템
    implementation(libs.compose.icons.extended)     // 확장 아이콘 세트
    implementation(libs.activity.compose)           // ComponentActivity + Compose
    implementation(libs.navigation.compose)         // Compose 내비게이션

    // ── [Lifecycle] ViewModel + Runtime Compose 연동 ──
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)

    // ── [Hilt] DI 프레임워크: KSP 컴파일러 처리 (annotationProcessor/kapt 사용 금지) ──
    // @HiltAndroidApp이 적용된 Application 클래스와 연동
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)                        // ✅ KSP로 처리
    implementation(libs.hilt.navigation.compose)   // Compose NavHost + Hilt 통합

    // ── [Firebase] BOM 패턴으로 버전 통합 관리 ──
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)             // Firebase 인증
    implementation(libs.firebase.firestore)        // Cloud Firestore DB
    implementation(libs.firebase.messaging)        // FCM 푸시 알림

    // ── [Google Sign-In] Credential Manager 기반 구글 로그인 ──
    implementation(libs.credential.manager)
    implementation(libs.credential.manager.play)
    implementation(libs.google.id)

    // ── [AdMob] 광고 SDK ──
    // ✅ ADDED: AndroidManifest.xml에 AD_ID 권한 선언 필요:
    // <uses-permission android:name="com.google.android.gms.permission.AD_ID"/>
    // (Android 13+ 타겟 시 또는 광고 식별자 접근이 필요한 경우 필수)
    implementation(libs.admob)

    // ── [Room] 로컬 SQLite DB: KSP로 컴파일러 처리 ──
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)                  // Coroutine/Flow 확장
    ksp(libs.room.compiler)                        // ✅ KSP로 처리

    // ── [Coroutines] 비동기 처리 ──
    implementation(libs.coroutines.android)

    // ── [UI Tooling] Debug 전용: release 빌드에 포함되지 않음 ──
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    // ── [Test] 단위 테스트 및 UI 테스트 ──
    testImplementation(libs.junit)
    androidTestImplementation(libs.junit.android)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.compose.ui.test.junit4)
}
