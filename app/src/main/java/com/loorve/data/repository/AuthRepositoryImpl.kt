package com.loorve.data.repository

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
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
    @ApplicationContext private val context: Context,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    // ──────────────────────────────────────────────
    // 인터페이스 구현
    // ──────────────────────────────────────────────

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
    // ──────────────────────────────────────────────

    override suspend fun signInWithGoogle(idToken: String): Result<User> {
        return try {
            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseAuth.signInWithCredential(firebaseCredential).await()
            val user = authResult.user
                ?: return Result.failure(Exception("Google 로그인 실패: 사용자 정보 없음"))
            val domainUser = user.toDomainUser()
            createOrUpdateUserDocument(domainUser)
            Result.success(domainUser)
        } catch (e: com.google.firebase.auth.FirebaseAuthException) {
            Result.failure(Exception(mapFirebaseAuthError(e.errorCode), e))
        } catch (e: java.io.IOException) {
            Result.failure(Exception("네트워크 연결을 확인해주세요.", e))
        } catch (e: Exception) {
            Result.failure(Exception("인증 처리 중 오류가 발생했습니다.", e))
        }
    }

    // ──────────────────────────────────────────────
    // Firestore 사용자 문서 최초 생성
    // ──────────────────────────────────────────────

    private suspend fun createOrUpdateUserDocument(user: User) {
        try {
            val docRef = firestore.collection("users").document(user.id)

            // 최초 생성 여부 확인 (createdAt은 최초 1회만 기록)
            val snapshot = docRef.get().await()
            val isNewUser = !snapshot.exists()

            val data = buildMap<String, Any?> {
                put("uid",         user.id)
                put("email",       user.email)
                put("displayName", user.nickname)      // 도메인 nickname → Firestore displayName
                put("photoUrl",    user.profileImageUrl) // null 허용
                put("lastLoginAt", FieldValue.serverTimestamp()) // 매 로그인마다 갱신
                if (isNewUser) {
                    put("createdAt", FieldValue.serverTimestamp()) // 최초 생성 시만 기록
                }
            }

            // SetOptions.merge() → 기존 필드 보존, 새 필드만 추가/갱신 (중복 방지)
            docRef.set(data, com.google.firebase.firestore.SetOptions.merge()).await()

            Log.d(TAG, "Firestore users 문서 ${if (isNewUser) "생성" else "업데이트"} 완료 (uid=${user.id})")

        } catch (e: com.google.firebase.firestore.FirebaseFirestoreException) {
            // Firestore 권한/가용성 오류 — 로그만 남기고 로그인 흐름은 중단하지 않음
            Log.e(TAG, "Firestore 권한 오류 (uid=${user.id}): ${e.code}", e)
        } catch (e: java.io.IOException) {
            // 네트워크 오류 — 재시도 없이 로그만 남김 (오프라인 캐시로 나중에 처리 가능)
            Log.e(TAG, "Firestore 네트워크 오류 (uid=${user.id})", e)
        } catch (e: Exception) {
            Log.e(TAG, "Firestore 문서 저장 실패 (uid=${user.id})", e)
        }
    }

    // ──────────────────────────────────────────────
    // 내부 매핑
    // ──────────────────────────────────────────────

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
        "ERROR_INVALID_EMAIL"          -> "이메일 형식이 올바르지 않습니다."
        "ERROR_WRONG_PASSWORD"         -> "이메일 또는 비밀번호가 올바르지 않습니다."
        "ERROR_USER_NOT_FOUND"         -> "이메일 또는 비밀번호가 올바르지 않습니다."
        "ERROR_USER_DISABLED"          -> "비활성화된 계정입니다. 고객센터에 문의해주세요."
        "ERROR_TOO_MANY_REQUESTS"      -> "잠시 후 다시 시도해주세요."
        "ERROR_NETWORK_REQUEST_FAILED" -> "네트워크 연결을 확인해주세요."
        else                           -> "로그인에 실패했습니다. 다시 시도해주세요."
    }

    companion object {
        private const val TAG = "AuthRepository"
        private const val WEB_CLIENT_ID = "YOUR_WEB_CLIENT_ID_HERE"
    }
}

