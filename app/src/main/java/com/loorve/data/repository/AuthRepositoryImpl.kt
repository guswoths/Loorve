package com.loorve.data.repository

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.loorve.domain.model.User
import com.loorve.domain.repository.AuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    @ApplicationContext private val context: Context
) : AuthRepository {

    // ──────────────────────────────────────────────
    // 인터페이스 구현
    // ──────────────────────────────────────────────

    /**
     * 이메일/패스워드 로그인.
     * 현재 AuthRepository 인터페이스 계약상 email/password 방식을 기본으로 구현.
     * Google 로그인은 [loginWithGoogle] 참고.
     *
     * ⚠️ 보안: password는 절대 로그에 출력하지 않음.
     */
    override suspend fun login(email: String, password: String): Result<User> {
        return try {
            val result = firebaseAuth
                .signInWithEmailAndPassword(email, password)
                .await()
            val firebaseUser = result.user
                ?: return Result.failure(Exception("로그인 실패: 사용자 정보 없음"))
            Result.success(firebaseUser.toDomainUser())
        } catch (e: java.io.IOException) {
            Result.failure(Exception("네트워크 연결을 확인해주세요.", e))
        } catch (e: com.google.firebase.auth.FirebaseAuthException) {
            // Firebase 에러 코드를 한국어 메시지로 변환 (내부 코드 노출 차단)
            Result.failure(Exception(mapFirebaseAuthError(e.errorCode), e))
        } catch (e: Exception) {
            Result.failure(Exception("로그인 중 오류가 발생했습니다. 다시 시도해주세요.", e))
        }
    }

    override suspend fun logout(): Result<Unit> {
        return try {
            firebaseAuth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getCurrentUser(): Flow<User?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser?.toDomainUser())
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    // ──────────────────────────────────────────────
    // Google 로그인 (Credential Manager API)
    // 인터페이스 확장 제안 — 현재는 구현체 내부 메서드로 제공.
    // ViewModel 또는 UseCase에서 (repository as AuthRepositoryImpl).loginWithGoogle(activity)
    // 형태로 호출하거나, AuthRepository 인터페이스에 추가를 팀에서 결정할 것.
    // ──────────────────────────────────────────────

    /**
     * Credential Manager API를 활용한 Google 로그인.
     * @param activityContext Activity Context 필요 (CredentialManager 요구 사항)
     *
     * ※ 확인 필요: WEB_CLIENT_ID 값을 google-services.json 기반
     *   BuildConfig 또는 strings.xml로 관리하는 것을 권장.
     */
    suspend fun loginWithGoogle(activityContext: Context): Result<User> {
        return try {
            val credentialManager = CredentialManager.create(activityContext)

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(WEB_CLIENT_ID)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val credentialResponse = credentialManager.getCredential(
                request = request,
                context = activityContext
            )

            val googleIdTokenCredential = GoogleIdTokenCredential
                .createFrom(credentialResponse.credential.data)

            val idToken = googleIdTokenCredential.idToken
            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseAuth.signInWithCredential(firebaseCredential).await()

            val user = authResult.user
                ?: return Result.failure(Exception("Google 로그인 실패: 사용자 정보 없음"))
            Result.success(user.toDomainUser())

        } catch (e: androidx.credentials.exceptions.GetCredentialCancellationException) {
            // 취소는 별도 예외 타입으로 그대로 전달 (ViewModel에서 분류)
            Result.failure(e)
        } catch (e: GetCredentialException) {
            // 네트워크 에러, 일반 Credential 에러
            Result.failure(e)
        } catch (e: java.io.IOException) {
            // 네트워크 타임아웃, 연결 불가
            Result.failure(e)
        } catch (e: Exception) {
            // 그 외 예외: 보안상 원본 메시지 래핑
            Result.failure(Exception("인증 처리 중 오류가 발생했습니다.", e))
        }
    }

    // ──────────────────────────────────────────────
    // 내부 매핑
    // ──────────────────────────────────────────────

    /**
     * FirebaseUser → Domain User 변환.
     * createdAt / updatedAt: FirebaseUser는 metadata.creationTimestamp를 제공하나
     * updatedAt에 해당하는 표준 필드가 없으므로 현재 시각으로 fallback 처리.
     * ※ 확인 필요: 서버 Firestore 등에서 실제 updatedAt을 가져오는 로직 추후 통합 권장.
     */
    private fun FirebaseUser.toDomainUser(): User {
        val now = System.currentTimeMillis()
        return User(
            id = uid,
            email = email ?: "",
            nickname = displayName ?: email?.substringBefore("@") ?: "사용자",
            profileImageUrl = photoUrl?.toString(),
            createdAt = metadata?.creationTimestamp ?: now,
            updatedAt = now
        )
    }

    private fun mapFirebaseAuthError(errorCode: String): String = when (errorCode) {
        "ERROR_INVALID_EMAIL"         -> "이메일 형식이 올바르지 않습니다."
        "ERROR_WRONG_PASSWORD"        -> "이메일 또는 비밀번호가 올바르지 않습니다."
        "ERROR_USER_NOT_FOUND"        -> "이메일 또는 비밀번호가 올바르지 않습니다."
        "ERROR_USER_DISABLED"         -> "비활성화된 계정입니다. 고객센터에 문의해주세요."
        "ERROR_TOO_MANY_REQUESTS"     -> "잠시 후 다시 시도해주세요."
        "ERROR_NETWORK_REQUEST_FAILED"-> "네트워크 연결을 확인해주세요."
        else                          -> "로그인에 실패했습니다. 다시 시도해주세요."
    }

    companion object {
        // ※ 확인 필요: 실제 WEB_CLIENT_ID를 BuildConfig 또는 strings.xml로 교체할 것.
        // Firebase Console > 프로젝트 설정 > 웹 앱의 OAuth 2.0 클라이언트 ID
        private const val WEB_CLIENT_ID = "YOUR_WEB_CLIENT_ID_HERE"
    }
}
