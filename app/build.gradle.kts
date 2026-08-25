// app/build.gradle.kts
import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id("com.google.gms.google-services")
}

val keystoreProperties = Properties()
val keystorePropertiesFile = rootProject.file("keystore.properties")

if (keystorePropertiesFile.exists()) {
    keystorePropertiesFile.inputStream().use { input ->
        keystoreProperties.load(input)
    }
}

// ✅ local.properties 로드 (WEB_CLIENT_ID, ADMOB_BANNER_UNIT_ID 등 민감 정보 관리)
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { input ->
        localProperties.load(input)
    }
}

android {
    namespace = "com.loorve"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.loorve"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // ✅ BuildConfig 필드 — local.properties에서 읽어옴
        buildConfigField(
            "String",
            "GOOGLE_WEB_CLIENT_ID",
            "\"${localProperties.getProperty("GOOGLE_WEB_CLIENT_ID", "")}\""
        )

        // ✅ AdMob 배너 광고 단위 ID
        // local.properties에 ADMOB_BANNER_UNIT_ID 미설정 시 → 테스트 ID 자동 폴백
        // local.properties에 실제 ID 설정 시 → 실제 ID 사용 (배포 직전 교체)
        buildConfigField(
            "String",
            "ADMOB_BANNER_UNIT_ID",
            "\"${localProperties.getProperty("ADMOB_BANNER_UNIT_ID", "ca-app-pub-3940256099942544/6300978111")}\""
        )
    }

    buildTypes {
        debug {
            // debug 빌드는 defaultConfig의 테스트 ID 폴백을 그대로 사용
            // 별도 buildConfigField 불필요 (defaultConfig 상속)
        }
        release {
            // [추가] local.properties에 실제 ID 미설정 시 Gradle 빌드 경고 출력
            // 실수로 테스트 ID가 배포되는 것을 사전에 감지
            val releaseAdUnitId = localProperties.getProperty("ADMOB_BANNER_UNIT_ID", "")
            if (releaseAdUnitId.isEmpty()) {
                logger.warn(
                    "⚠️ WARNING: ADMOB_BANNER_UNIT_ID is not set in local.properties. " +
                            "Test ID will be used in release build! " +
                            "Set the real Ad Unit ID before publishing to the Play Store."
                )
            }

            // [권장] 배포 전 isMinifyEnabled = true 로 변경 후 하단 ProGuard 규칙 확인
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // ProGuard 활성화 시 proguard-rules.pro에 아래 규칙 추가 필요:
            // -keep class com.google.android.gms.ads.** { *; }
            // -dontwarn com.google.android.gms.ads.**
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    jvmToolchain(17)
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        optIn.add("androidx.compose.foundation.ExperimentalFoundationApi")
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.3")
    implementation("androidx.activity:activity-compose:1.9.0")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")

    // ✅ Google AdMob — 배너 광고 SDK
    implementation("com.google.android.gms:play-services-ads:23.6.0")

    implementation("com.google.dagger:hilt-android:2.59.2")
    ksp("com.google.dagger:hilt-compiler:2.59.2")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("androidx.compose.material:material-icons-extended")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}