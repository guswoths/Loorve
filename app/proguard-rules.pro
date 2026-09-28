# ==============================================================================
# Loorve App - ProGuard / R8 DEX Optimization Rules
# 기존 코드 작동에 문제가 생기지 않도록 완벽하고 안전한 Keep 규칙 정의
# ==============================================================================

# ------------------------------------------------------------------------------
# 1. 공통 기본 속성 유지 (디버깅 스택트레이스 및 리플렉션 안전성)
# ------------------------------------------------------------------------------
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable
-dontwarn **

# ------------------------------------------------------------------------------
# 2. Android 4대 컴포넌트 및 앱 진입점
# ------------------------------------------------------------------------------
-keep class com.loorve.LoorveApplication { *; }
-keep class com.loorve.MainActivity { *; }
-keep class com.loorve.service.** { *; }
-keep class com.loorve.receiver.** { *; }
-keep class * extends android.app.Activity
-keep class * extends android.app.Application
-keep class * extends android.app.Service
-keep class * extends android.content.BroadcastReceiver
-keep class * extends android.content.ContentProvider

# ------------------------------------------------------------------------------
# 3. 홈 위젯 (AppWidgetProvider & RemoteViewsService)
# ------------------------------------------------------------------------------
-keep class com.loorve.widget.** { *; }
-keep class * extends android.appwidget.AppWidgetProvider { *; }
-keep class * extends android.widget.RemoteViewsService { *; }

# ------------------------------------------------------------------------------
# 4. Hilt / Dagger 의존성 주입
# ------------------------------------------------------------------------------
-keep class * extends dagger.hilt.android.EntryPointAccessors { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keep @dagger.hilt.EntryPoint class * { *; }
-keep class com.loorve.di.** { *; }
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# ------------------------------------------------------------------------------
# 5. Firebase & Firestore 데이터 모델 (toObject() 리플렉션 100% 보존)
# ------------------------------------------------------------------------------
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.PropertyName <methods>;
    @com.google.firebase.firestore.ServerTimestamp <fields>;
    @com.google.firebase.firestore.ServerTimestamp <methods>;
    @com.google.firebase.firestore.Exclude <fields>;
    @com.google.firebase.firestore.Exclude <methods>;
}

# Firestore DTO 및 도메인 엔티티 클래스 전체 필드 및 생성자 보존
-keep class com.loorve.data.model.** { *; }
-keep class com.loorve.domain.model.** { *; }
-keep class com.loorve.domain.review.** { *; }
-keep class com.loorve.domain.subscription.** { *; }
-keep class com.loorve.domain.notification.** { *; }
-keep class com.loorve.presentation.home.NearestExamUiModel { *; }
-keep class com.loorve.presentation.home.ProgressUiModel { *; }
-keep class com.loorve.presentation.home.ReviewScheduleUiModel { *; }
-keep class com.loorve.presentation.home.ReviewBlockUiModel { *; }
-keep class com.loorve.presentation.home.CumulativeReviewCountPoint { *; }
-keep class com.loorve.presentation.calendar.ReviewCalendarUiState { *; }

# ------------------------------------------------------------------------------
# 6. Jetpack Compose & Navigation
# ------------------------------------------------------------------------------
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.material3.** { *; }
-keep class androidx.navigation.** { *; }
-dontwarn androidx.compose.**

# ------------------------------------------------------------------------------
# 7. Google Play Billing (인앱 결제 및 구독)
# ------------------------------------------------------------------------------
-keep class com.android.billingclient.api.** { *; }
-keep class com.android.billingclient.** { *; }

# ------------------------------------------------------------------------------
# 8. Google Mobile Ads (AdMob)
# ------------------------------------------------------------------------------
-keep public class com.google.android.gms.ads.** {
    public *;
}
-keep public class com.google.ads.** {
    public *;
}
-keep class com.google.ads.mediation.** { *; }
-keep class com.google.android.gms.ads.mediation.** { *; }

# ------------------------------------------------------------------------------
# 9. Google Identity & Credentials (Google 로그인)
# ------------------------------------------------------------------------------
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }
-keep class com.google.android.gms.auth.** { *; }

# ------------------------------------------------------------------------------
# 10. Kotlin Coroutines & DataStore
# ------------------------------------------------------------------------------
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-keep class androidx.datastore.** { *; }
