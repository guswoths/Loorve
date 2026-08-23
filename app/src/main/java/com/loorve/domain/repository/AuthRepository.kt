package com.loorve.domain.repository

import android.content.Context
import com.loorve.domain.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Domain Layer - 인증(Auth) Repository 인터페이스
 */
interface AuthRepository {

    suspend fun login(email: String, password: String): Result<User>

    /**
     * @deprecated logout() 대신 signOut()을 사용하세요.
     */
    @Deprecated(
        message = "signOut()으로 교체 예정. 마이페이지 로그아웃은 signOut()을 사용하세요.",
        replaceWith = ReplaceWith("signOut()")
    )
    suspend fun logout(): Result<Unit>

    /**
     * 현재 로그인된 사용자를 로그아웃합니다.
     * Google Sign-In 사용자는 credential revoke까지 처리합니다.
     *
     * @return 성공 시 [Result.success(Unit)], 실패 시 [Result.failure]
     */
    suspend fun signOut(): Result<Unit>

    fun getCurrentUser(): Flow<User?>

    suspend fun signInWithGoogle(idToken: String): Result<User>
    suspend fun launchGoogleSignIn(activityContext: Context): Result<User>
}