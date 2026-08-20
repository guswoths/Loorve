// ─────────────────────────────────────────────
// 파일: build.gradle.kts  ← 루트 프로젝트 빌드 스크립트
// 역할: 플러그인 버전 선언만 담당 (apply false 필수)
// ─────────────────────────────────────────────
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.google.services) apply false // ✅ 루트에서는 apply false
}
