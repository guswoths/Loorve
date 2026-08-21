plugins {
    id("com.android.application") version "..." apply false
    id("org.jetbrains.kotlin.android") version "..." apply false
    id("com.google.dagger.hilt.android") version "..." apply false
    id("com.google.devtools.ksp") version "..." apply false

    id("com.google.gms.google-services") version "4.5.0" apply false
}

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
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            val keystorePropertiesFile = rootProject.file("keystore.properties")

            if (keystorePropertiesFile.exists()) {
                val keystoreProperties = java.util.Properties().apply {
                    keystorePropertiesFile.inputStream().use(::load)
                }

                storeFile = rootProject.file(
                    checkNotNull(keystoreProperties.getProperty("storeFile")) {
                        "keystore.properties에 storeFile 값이 없습니다."
                    }
                )
                storePassword = checkNotNull(keystoreProperties.getProperty("storePassword")) {
                    "keystore.properties에 storePassword 값이 없습니다."
                }
                keyAlias = checkNotNull(keystoreProperties.getProperty("keyAlias")) {
                    "keystore.properties에 keyAlias 값이 없습니다."
                }
                keyPassword = checkNotNull(keystoreProperties.getProperty("keyPassword")) {
                    "keystore.properties에 keyPassword 값이 없습니다."
                }
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            val keystorePropertiesFile = rootProject.file("keystore.properties")
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlin {
        jvmToolchain(17)
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

dependencies {
    // Android Core / Lifecycle
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.1")
    implementation("androidx.activity:activity-compose:1.10.1")

    // Jetpack Compose
    implementation(platform("androidx.compose:compose-bom:2025.06.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.navigation:navigation-compose:2.9.0")

    // Hilt + KSP
    implementation("com.google.dagger:hilt-android:2.56.2")
    ksp("com.google.dagger:hilt-compiler:2.56.2")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Room + KSP
    implementation("androidx.room:room-runtime:2.7.1")
    implementation("androidx.room:room-ktx:2.7.1")
    ksp("androidx.room:room-compiler:2.7.1")

    // DataStore Preferences
    implementation("androidx.datastore:datastore-preferences:1.1.7")

    // Firebase BoM: 개별 Firebase 라이브러리에는 버전을 쓰지 않음
    implementation(platform("com.google.firebase:firebase-bom:34.0.0"))

    // Firebase 메인 모듈: -ktx 접미사 사용 금지
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-analytics")

    // Firebase Task.await() 지원
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")

    // Google Credential Manager + Google ID 로그인
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    // Google Mobile Ads SDK
    implementation("com.google.android.gms:play-services-ads:24.5.0")

    // Java 8+ API desugaring: java.time 사용을 위한 설정
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")

    // Test
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.06.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    // Debug 전용
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
