package com.loorve.domain.repository

import android.content.Context
import com.loorve.domain.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Domain Layer - 인증(Auth) Repository 인터페이스
 */
interface AuthRepository {

    suspend fun login(email: String, password: String): Result<User>

    @Deprecated(
        message = "signOut()으로 교체 예정. 마이페이지 로그아웃은 signOut()을 사용하세요.",
        replaceWith = ReplaceWith("signOut()")
    )
    suspend fun logout(): Result<Unit>

    suspend fun signOut(): Result<Unit>

    fun getCurrentUser(): Flow<User?>

    /** Pair.second = isNewUser (Firestore users 문서 미존재 = 신규) */
    suspend fun signInWithGoogle(idToken: String): Result<Pair<User, Boolean>>
    suspend fun launchGoogleSignIn(activityContext: Context): Result<Pair<User, Boolean>>

    suspend fun deleteAccount(): Result<Unit>
}