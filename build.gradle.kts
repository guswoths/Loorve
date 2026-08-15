plugins {
    // Android 앱 모듈 플러그인
    alias(libs.plugins.android.application) apply false

    // Kotlin Android 플러그인 (확인 필요: libs.versions.toml에 kotlin.android 항목 있는 경우 추가)
    // alias(libs.plugins.kotlin.android) apply false

    // Jetpack Compose Kotlin 컴파일러 플러그인
    alias(libs.plugins.kotlin.compose) apply false

    // KSP (Kotlin Symbol Processing) — Hilt, Room 등에서 사용
    alias(libs.plugins.ksp) apply false

    // Hilt 의존성 주입
    alias(libs.plugins.hilt) apply false

    // Firebase Google Services
    alias(libs.plugins.google.services) apply false
}
