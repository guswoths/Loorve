// 경로: app/src/main/java/com/loorve/data/repository/AuthRepositoryImpl.kt
package com.loorve.data.repository

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialCustomException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.loorve.R
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
    @param:ApplicationContext private val context: Context,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    // ─────────────────────────────────────────────────────────────
    // 이메일 로그인
    // ─────────────────────────────────────────────────────────────
    override suspend fun login(email: String, password: String): Result<User> {
        return try {
            val result = firebaseAuth
                .signInWithEmailAndPassword(email, password)
                .await()
            val firebaseUser = result.user
                ?: return Result.failure(Exception("로그인 실패: 사용자 정보 없음"))
            val domainUser = firebaseUser.toDomainUser()
            createOrUpdateUserDocument(domainUser)
            Result.success(domainUser)
        } catch (e: java.io.IOException) {
            Result.failure(Exception("네트워크 연결을 확인해주세요.", e))
        } catch (e: com.google.firebase.auth.FirebaseAuthException) {
            Result.failure(Exception(mapFirebaseAuthError(e.errorCode), e))
        } catch (e: Exception) {
            Result.failure(Exception("로그인 중 오류가 발생했습니다. 다시 시도해주세요.", e))
        }
    }

    // ─────────────────────────────────────────────────────────────
    // @Deprecated logout() — signOut() 사용 권장
    // ─────────────────────────────────────────────────────────────
    @Deprecated(
        message = "signOut()으로 교체 예정. 마이페이지 로그아웃은 signOut()을 사용하세요.",
        replaceWith = ReplaceWith("signOut()")
    )
    override suspend fun logout(): Result<Unit> {
        return try {
            firebaseAuth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 로그아웃 — 로그인 방식 무관하게 항상 credential 초기화
    // ─────────────────────────────────────────────────────────────
    override suspend fun signOut(): Result<Unit> {
        return try {
            val currentUser = firebaseAuth.currentUser
            try {
                val credentialManager = CredentialManager.create(context)
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w(TAG, "clearCredentialState 실패 (계속 진행): ${e.message}")
            }
            firebaseAuth.signOut()
            Log.d(TAG, "signOut 완료 (uid=${currentUser?.uid})")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "signOut 실패", e)
            Result.failure(Exception("로그아웃에 실패했습니다. 다시 시도해주세요.", e))
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 계정 삭제
    // 순서: Firestore 데이터 삭제 → credential 초기화 → Auth 계정 삭제
    // ─────────────────────────────────────────────────────────────
    override suspend fun deleteAccount(): Result<Unit> {
        return try {
            val currentUser = firebaseAuth.currentUser
                ?: return Result.failure(Exception("로그인 상태가 아닙니다."))
            val uid = currentUser.uid

            val progressDocs = firestore
                .collection("users").document(uid)
                .collection("progress")
                .get().await()
            progressDocs.documents.forEach { it.reference.delete().await() }
            Log.d(TAG, "progress 삭제 완료 (uid=$uid, count=${progressDocs.size()})")

            val scheduleDocs = firestore
                .collection("users").document(uid)
                .collection("reviewSchedules")
                .get().await()
            scheduleDocs.documents.forEach { it.reference.delete().await() }
            Log.d(TAG, "reviewSchedules 삭제 완료 (uid=$uid, count=${scheduleDocs.size()})")

            // ✅ [원인3 수정] reviewBlocks 서브컬렉션 삭제 추가 (기존 누락)
            val reviewBlockDocs = firestore
                .collection("users").document(uid)
                .collection("reviewBlocks")
                .get().await()
            reviewBlockDocs.documents.forEach { it.reference.delete().await() }
            Log.d(TAG, "reviewBlocks 삭제 완료 (uid=$uid, count=${reviewBlockDocs.size()})")

            val examDocs = firestore
                .collection("exams")
                .whereEqualTo("createdBy", uid)
                .get().await()
            examDocs.documents.forEach { it.reference.delete().await() }
            Log.d(TAG, "exams 삭제 완료 (uid=$uid, count=${examDocs.size()})")

            val resultDocs = firestore
                .collection("examResults")
                .whereEqualTo("userId", uid)
                .get().await()
            resultDocs.documents.forEach { it.reference.delete().await() }
            Log.d(TAG, "examResults 삭제 완료 (uid=$uid, count=${resultDocs.size()})")

            firestore.collection("users").document(uid).delete().await()
            Log.d(TAG, "users 문서 삭제 완료 (uid=$uid)")

            try {
                CredentialManager.create(context)
                    .clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w(TAG, "deleteAccount: clearCredentialState 실패: ${e.message}")
            }

            currentUser.delete().await()
            Log.d(TAG, "계정 삭제 완료 (uid=$uid)")

            Result.success(Unit)

        } catch (e: FirebaseAuthRecentLoginRequiredException) {
            Log.w(TAG, "계정 삭제: 재로그인 필요", e)
            Result.failure(Exception("보안을 위해 재로그인 후 다시 시도해주세요."))
        } catch (e: Exception) {
            Log.e(TAG, "계정 삭제 실패", e)
            Result.failure(Exception("계정 삭제 중 오류가 발생했습니다. 다시 시도해주세요.", e))
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 현재 유저 상태 Flow
    // ─────────────────────────────────────────────────────────────
    override fun getCurrentUser(): Flow<User?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser?.toDomainUser())
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    // ─────────────────────────────────────────────────────────────
    // Google 로그인 (Credential Manager)
    // ─────────────────────────────────────────────────────────────
    override suspend fun launchGoogleSignIn(activityContext: Context): Result<Pair<User, Boolean>> {
        return try {
            val serverClientId = activityContext
                .getString(R.string.default_web_client_id)
                .trim()
            if (serverClientId.isBlank()) {
                Log.e(TAG, "Google 로그인 설정 오류: default_web_client_id가 비어 있습니다.")
                return Result.failure(
                    IllegalStateException("Google 로그인 설정이 올바르지 않습니다.")
                )
            }
            Log.d(TAG, "Google Credential 요청 시작 (serverClientId=${maskClientId(serverClientId)})")
            val credentialManager = CredentialManager.create(activityContext)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setServerClientId(serverClientId)
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .setPreferImmediatelyAvailableCredentials(false)
                .build()
            val credentialResponse = try {
                credentialManager.getCredential(
                    request = request,
                    context = activityContext
                )
            } catch (e: NoCredentialException) {
                logGoogleAuthException(
                    "Google ID credential을 사용할 수 없어 Sign in with Google 옵션으로 fallback합니다",
                    e
                )
                return signInWithGoogleCredentialOption(
                    credentialManager = credentialManager,
                    activityContext = activityContext,
                    serverClientId = serverClientId
                )
            }
            Log.d(TAG, "Google Credential 응답 수신: type=${credentialResponse.credential.type}")
            val googleIdTokenCredential = GoogleIdTokenCredential
                .createFrom(credentialResponse.credential.data)
            val idToken = googleIdTokenCredential.idToken
            if (idToken.isBlank()) {
                Log.e(TAG, "Google 로그인 실패: 빈 ID token이 반환되었습니다.")
                return Result.failure(IllegalStateException("Google 인증 토큰이 비어 있습니다."))
            }
            Log.d(TAG, "Google Credential 수신 완료")
            signInWithGoogle(idToken)
        } catch (e: GetCredentialCancellationException) {
            if (isAccountReauthFailure(e)) {
                logGoogleAuthException(
                    "Google 계정 재인증 실패: OAuth Web Client ID와 Android OAuth " +
                        "클라이언트의 package name/SHA-1 일치를 확인하세요. " +
                        "현재 package=${activityContext.packageName}, " +
                        "serverClientId=${
                            maskClientId(
                                activityContext.getString(R.string.default_web_client_id).trim()
                            )
                        }",
                    e,
                    Log.WARN
                )
                Result.failure(
                    Exception(
                        "Google 계정 재인증에 실패했습니다. OAuth Client ID, package name, SHA-1 설정을 확인해주세요.",
                        e
                    )
                )
            } else {
                logGoogleAuthException("Google 로그인 사용자가 취소했습니다", e)
                Result.failure(Exception("CANCELLED", e))
            }
        } catch (e: NoCredentialException) {
            val diagnostic = googleAuthDiagnostic(e)
            logGoogleAuthException("Google credential provider가 credential을 반환하지 않았습니다", e)
            Result.failure(
                Exception(diagnostic, e)
            )
        } catch (e: GetCredentialCustomException) {
            val diagnostic = googleAuthDiagnostic(e)
            logGoogleAuthException("Google Credential custom provider 오류", e)
            Result.failure(Exception(diagnostic, e))
        } catch (e: GetCredentialException) {
            val diagnostic = googleAuthDiagnostic(e)
            logGoogleAuthException("Google Credential 요청 실패", e)
            Result.failure(Exception(diagnostic, e))
        } catch (e: Exception) {
            val diagnostic = googleAuthDiagnostic(e)
            logGoogleAuthException("Google 로그인 실행 오류", e)
            Result.failure(Exception(diagnostic, e))
        }
    }

    private suspend fun signInWithGoogleCredentialOption(
        credentialManager: CredentialManager,
        activityContext: Context,
        serverClientId: String
    ): Result<Pair<User, Boolean>> {
        return try {
            val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(serverClientId)
                .build()
            val fallbackRequest = GetCredentialRequest.Builder()
                .addCredentialOption(signInWithGoogleOption)
                .setPreferImmediatelyAvailableCredentials(false)
                .build()
            val credentialResponse = credentialManager.getCredential(
                request = fallbackRequest,
                context = activityContext
            )
            Log.d(
                TAG,
                "Sign in with Google fallback credential 응답 수신: " +
                    "type=${credentialResponse.credential.type}"
            )
            val googleIdTokenCredential = GoogleIdTokenCredential
                .createFrom(credentialResponse.credential.data)
            val idToken = googleIdTokenCredential.idToken
            if (idToken.isBlank()) {
                Log.e(TAG, "Google 로그인 fallback 실패: 빈 ID token이 반환되었습니다.")
                return Result.failure(IllegalStateException("Google 인증 토큰이 비어 있습니다."))
            }
            signInWithGoogle(idToken)
        } catch (e: GetCredentialCancellationException) {
            if (isAccountReauthFailure(e)) {
                logGoogleAuthException(
                    "Google 계정 재인증 실패(fallback): OAuth Web Client ID와 Android OAuth " +
                        "클라이언트의 package name/SHA-1 일치를 확인하세요. " +
                        "현재 package=${activityContext.packageName}, " +
                        "serverClientId=${maskClientId(serverClientId)}",
                    e,
                    Log.WARN
                )
                Result.failure(
                    Exception(
                        "Google 계정 재인증에 실패했습니다. OAuth Client ID, package name, SHA-1 설정을 확인해주세요.",
                        e
                    )
                )
            } else {
                logGoogleAuthException("Google 로그인 fallback을 사용자가 취소했습니다", e)
                Result.failure(Exception("CANCELLED", e))
            }
        } catch (e: GetCredentialException) {
            logGoogleAuthException("Google 로그인 fallback 요청 실패", e)
            Result.failure(Exception(googleAuthDiagnostic(e), e))
        } catch (e: Exception) {
            logGoogleAuthException("Google 로그인 fallback 실행 오류", e)
            Result.failure(Exception(googleAuthDiagnostic(e), e))
        }
    }

    override suspend fun signInWithGoogle(idToken: String): Result<Pair<User, Boolean>> {
        return try {
            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseAuth.signInWithCredential(firebaseCredential).await()
            val user = authResult.user
                ?: return Result.failure(Exception("Google 로그인 실패: 사용자 정보 없음"))
            val domainUser = user.toDomainUser()
            val isNewUser = createOrUpdateUserDocument(domainUser)
            Result.success(Pair(domainUser, isNewUser))
        } catch (e: com.google.firebase.auth.FirebaseAuthException) {
            Log.e(TAG, "Firebase Google 인증 실패: code=${e.errorCode}, message=${e.message}", e)
            Result.failure(Exception(mapFirebaseAuthError(e.errorCode), e))
        } catch (e: java.io.IOException) {
            Log.e(TAG, "Firebase Google 인증 네트워크 실패: ${e.message}", e)
            Result.failure(Exception("네트워크 연결을 확인해주세요.", e))
        } catch (e: Exception) {
            Log.e(TAG, "Firebase Google 인증 처리 오류: ${e.message}", e)
            Result.failure(Exception("인증 처리 중 오류가 발생했습니다.", e))
        }
    }

    // ─────────────────────────────────────────────────────────────
    // ✅ [원인3 수정] 앱 재시작 후 users 문서 누락 시 복구용 public 함수
    // — 로그인 후 초기 진입 시 ViewModel 등에서 호출하여 문서 존재를 보장
    // ─────────────────────────────────────────────────────────────
    suspend fun ensureUserDocument(): Result<Unit> {
        val user = firebaseAuth.currentUser
            ?: return Result.failure(Exception("로그인 상태가 아닙니다."))
        return try {
            val docRef = firestore.collection("users").document(user.uid)
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) {
                Log.w(TAG, "users 문서 누락 감지 → 복구 생성 (uid=${user.uid})")
                val data = mapOf(
                    "uid"         to user.uid,
                    "email"       to (user.email ?: ""),
                    "displayName" to (user.displayName ?: user.email?.substringBefore("@") ?: "사용자"),
                    "photoUrl"    to (user.photoUrl?.toString()),
                    "createdAt"   to FieldValue.serverTimestamp(),
                    "lastLoginAt" to FieldValue.serverTimestamp()
                )
                docRef.set(data, SetOptions.merge()).await()
                Log.d(TAG, "users 문서 복구 완료 (uid=${user.uid})")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "ensureUserDocument 실패 (uid=${user.uid})", e)
            Result.failure(e)
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────

    /**
     * @return isNewUser — true: Firestore 문서 신규 생성, false: 기존 문서 업데이트
     */
    private suspend fun createOrUpdateUserDocument(user: User): Boolean {
        return try {
            val docRef = firestore.collection("users").document(user.id)
            val snapshot = docRef.get().await()
            val isNewUser = !snapshot.exists()

            val data = buildMap<String, Any?> {
                put("uid",         user.id)
                put("email",       user.email)
                put("displayName", user.nickname)
                put("photoUrl",    user.profileImageUrl)
                put("lastLoginAt", FieldValue.serverTimestamp())
                if (isNewUser) {
                    put("createdAt", FieldValue.serverTimestamp())
                }
            }

            docRef.set(data, SetOptions.merge()).await()
            Log.d(TAG, "Firestore users 문서 ${if (isNewUser) "생성" else "업데이트"} 완료 (uid=${user.id})")
            isNewUser

        } catch (e: com.google.firebase.firestore.FirebaseFirestoreException) {
            Log.e(TAG, "Firestore 권한 오류 (uid=${user.id}): ${e.code}", e)
            false
        } catch (e: java.io.IOException) {
            Log.e(TAG, "Firestore 네트워크 오류 (uid=${user.id})", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Firestore 문서 저장 실패 (uid=${user.id})", e)
            false
        }
    }

    private fun FirebaseUser.toDomainUser(): User {
        val now = System.currentTimeMillis()
        return User(
            id              = uid,
            email           = email ?: "",
            nickname        = displayName ?: email?.substringBefore("@") ?: "사용자",
            profileImageUrl = photoUrl?.toString(),
            createdAt       = metadata?.creationTimestamp ?: now,
            updatedAt       = now
        )
    }

    private fun mapFirebaseAuthError(errorCode: String): String = when (errorCode) {
        "ERROR_INVALID_EMAIL"          -> "이메일 형식이 올바르지 않습니다."
        "ERROR_WRONG_PASSWORD"         -> "이메일 또는 비밀번호가 올바르지 않습니다."
        "ERROR_USER_NOT_FOUND"         -> "이메일 또는 비밀번호가 올바르지 않습니다."
        "ERROR_USER_DISABLED"          -> "비활성화된 계정입니다. 고객센터에 문의해주세요."
        "ERROR_TOO_MANY_REQUESTS"      -> "잠시 후 다시 시도해주세요."
        "ERROR_NETWORK_REQUEST_FAILED" -> "네트워크 연결을 확인해주세요."
        "ERROR_INVALID_CREDENTIAL"     -> "이메일 또는 비밀번호가 올바르지 않습니다."
        "ERROR_OPERATION_NOT_ALLOWED"  -> "이 로그인 방식은 현재 비활성화되어 있습니다."
        "ERROR_EMAIL_ALREADY_IN_USE"   -> "이미 사용 중인 이메일입니다."
        else -> "로그인에 실패했습니다. (코드: $errorCode)"
    }

    private fun maskClientId(clientId: String): String {
        return if (clientId.length > 12) {
            "${clientId.take(8)}...${clientId.takeLast(12)}"
        } else {
            "***"
        }
    }

    private fun googleAuthDiagnostic(error: Throwable): String {
        return "${error::class.java.name}: " +
            "${error.localizedMessage ?: error.message ?: "no localized message"}"
    }

    private fun isAccountReauthFailure(error: GetCredentialCancellationException): Boolean {
        return error.message?.contains("Account reauth failed", ignoreCase = true) == true ||
            error.type.contains("16", ignoreCase = true)
    }

    private fun logGoogleAuthException(
        message: String,
        error: Throwable,
        priority: Int = Log.ERROR
    ) {
        Log.println(
            priority,
            GOOGLE_AUTH_TAG,
            "$message\n" +
                "exceptionClass=${error::class.java.name}\n" +
                "localizedMessage=${error.localizedMessage}\n" +
                "stackTrace:\n${Log.getStackTraceString(error)}",
        )
    }

    /*
     * OAuth troubleshooting:
     * Register the local release keystore SHA-1 and the Google Play App Signing
     * SHA-1 for applicationId com.loorve_2 in the same Firebase/Google Cloud
     * Android OAuth client configuration. Keep serverClientId as the Web client
     * ID (default_web_client_id), not the Android client ID.
     */
    companion object {
        private const val TAG = "AuthRepository"
        private const val GOOGLE_AUTH_TAG = "GoogleAuth"
    }
}