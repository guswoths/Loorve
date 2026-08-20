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

// ── [수정 1] google-services.json CI 환경 폴백 처리 ──
// 로컬: app/google-services.json 직접 배치
// CI: 환경변수 GOOGLE_SERVICES_JSON에 파일 내용을 주입
val googleServicesFile = file("google-services.json")
if (!googleServicesFile.exists()) {
    val envJson = System.getenv("GOOGLE_SERVICES_JSON")
    require(envJson != null) {
        """
        ❌ google-services.json 파일이 없습니다.
        해결 방법:
          [로컬] Firebase Console → 프로젝트 설정 → google-services.json 다운로드 후
                 app/ 폴더에 배치: C:\Users\hjson\Loorve\app\google-services.json
          [CI]   환경변수 GOOGLE_SERVICES_JSON 에 파일 내용을 설정하세요.
        """.trimIndent()
    }
    googleServicesFile.writeText(envJson)
    logger.lifecycle("✅ google-services.json written from GOOGLE_SERVICES_JSON env var")
}

// ── 플러그인 선언: 버전 카탈로그(libs.versions.toml) alias 참조 ──
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)   // Compose 컴파일러 플러그인 (Kotlin 2.x+)
    alias(libs.plugins.ksp)              // ✅ kapt 완전 제거, KSP만 사용
    alias(libs.plugins.hilt)             // Hilt DI 플러그인
    alias(libs.plugins.google.services)  // Firebase google-services.json 처리
}

// ── Android 빌드 설정 ──
android {
    namespace = "com.loorve"
    compileSdk = 37

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
            isDebuggable = true
            // [수정 2] debug 빌드에서도 applicationIdSuffix 추가 권장
            applicationIdSuffix = ".debug"
            versionNameSuffix   = "-debug"
        }
        release {
            isMinifyEnabled  = true   // 코드 난독화 및 축소
            isShrinkResources = true  // 미사용 리소스 제거
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
        compose     = true  // Jetpack Compose 활성화
        buildConfig = true  // BuildConfig 클래스 생성 (환경 변수 접근용)
    }

    // [수정 3] Packaging: 중복 라이선스 파일 충돌 방지 (Firebase/Coroutines 혼용 시 필수)
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/versions/9/previous-compilation-data.bin"
        }
    }
}

// ── Kotlin 컴파일러 옵션 ──
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        // [수정 4] 명시적 API 모드 - 공개 API 노출 실수 방지 (선택사항, 팀 합의 후 적용)
        // explicitApi()
    }
}

// ── 의존성 선언 ──
dependencies {

    // ── [Compose] BOM으로 버전 통합 관리 ──
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.icons.extended)
    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)

    // ── [Lifecycle] ViewModel + Runtime Compose 연동 ──
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)

    // ── [Hilt] DI 프레임워크: KSP 컴파일러 처리 ──
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // ── [Firebase] BOM 패턴으로 버전 통합 관리 ──
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.messaging)

    // ── [Google Sign-In] Credential Manager 기반 구글 로그인 ──
    // [수정 5] 하드코딩 버전 → libs.versions.toml로 이관 권장
    //          단기 수정으로는 아래 버전 그대로 유지 (동작 확인된 버전)
    implementation(libs.credential.manager)
    implementation(libs.credential.manager.play)
    implementation(libs.google.id)
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    // ── [AdMob] 광고 SDK ──
    implementation(libs.admob)

    // ── [Room] 로컬 SQLite DB: KSP로 컴파일러 처리 ──
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // ── [DataStore] 온보딩 등 간단한 Key-Value 설정 저장 ──
    implementation(libs.datastore.preferences)

    // ── [Coroutines] 비동기 처리 ──
    implementation(libs.coroutines.android)

    // ── [UI Tooling] Debug 전용 ──
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    // ── [Test] 단위 테스트 및 UI 테스트 ──
    testImplementation(libs.junit)
    androidTestImplementation(libs.junit.android)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.compose.ui.test.junit4)

    // ── [Test] MockK + Coroutines Test ──
    testImplementation("io.mockk:mockk:1.13.10")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
}
