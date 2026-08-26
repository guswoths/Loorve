package com.loorve.data.repository

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.loorve.BuildConfig
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
    // (재로그인 시 계정 선택 팝업 반드시 표시)
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
            val credentialManager = CredentialManager.create(activityContext)

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
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

            signInWithGoogle(idToken)

        } catch (e: GetCredentialException) {
            Log.w(TAG, "Credential 취소 또는 실패: ${e.type}", e)
            Result.failure(Exception("CANCELLED"))
        } catch (e: Exception) {
            Log.e(TAG, "Google 로그인 실행 오류", e)
            Result.failure(Exception("인증 처리 중 오류가 발생했습니다."))
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
            Result.failure(Exception(mapFirebaseAuthError(e.errorCode), e))
        } catch (e: java.io.IOException) {
            Result.failure(Exception("네트워크 연결을 확인해주세요.", e))
        } catch (e: Exception) {
            Result.failure(Exception("인증 처리 중 오류가 발생했습니다.", e))
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────

    /**
     * @return isNewUser — true: Firestore 문서 신규 생성, false: 기존 문서 업데이트
     * Firestore 오류 발생 시 false를 반환해 기존 사용자로 안전하게 처리
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

    companion object {
        private const val TAG = "AuthRepository"
    }
}