// app/src/main/java/com/loorve/data/repository/ReviewBlockRepositoryImpl.kt
package com.loorve.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.loorve.domain.model.ReviewBlock
import com.loorve.domain.repository.ReviewBlockRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReviewBlockRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ReviewBlockRepository {

    companion object {
        private const val TAG = "ReviewBlockRepo"
    }

    // ✅ uid 유효성 + 토큰 갱신
    private suspend fun requireValidAuth(): String {
        val user = FirebaseAuth.getInstance().currentUser
            ?: throw IllegalStateException("로그인 세션이 만료되었습니다. 다시 로그인해 주세요.")
        return try {
            // 토큰 강제 갱신으로 만료 토큰 방지
            user.getIdToken(true).await()
            user.uid  // ✅ 반드시 갱신 후 uid 반환
        } catch (e: Exception) {
            Log.w(TAG, "토큰 갱신 실패, 기존 uid 사용: ${e.message}")
            user.uid
        }
    }

    private fun blockCollection(uid: String) =
        firestore.collection("users").document(uid).collection("reviewBlocks")

    override suspend fun saveReviewBlock(block: ReviewBlock): Result<Unit> {
        return try {
            // ✅ 핵심: uid를 항상 Firebase에서 직접 가져옴 (파라미터 신뢰 X)
            val uid = requireValidAuth()

            // ✅ 경로의 uid와 문서 데이터의 uid가 일치하도록 강제
            val blockWithUid = block.copy(uid = uid)

            blockCollection(uid)
                .document(blockWithUid.blockId)
                .set(blockWithUid)
                .await()

            Log.d(TAG, "saveReviewBlock 완료: uid=$uid, blockId=${blockWithUid.blockId}")
            Result.success(Unit)
        } catch (e: IllegalStateException) {
            Log.e(TAG, "saveReviewBlock 인증 실패: ${e.message}")
            Result.failure(e)
        } catch (e: FirebaseFirestoreException) {
            Log.e(TAG, "saveReviewBlock Firestore 오류(${e.code}): ${e.message}", e)
            val msg = when (e.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                    "저장 실패: 권한이 없습니다. 로그인 상태를 확인해 주세요."
                FirebaseFirestoreException.Code.UNAVAILABLE ->
                    "서버에 연결할 수 없습니다. 네트워크를 확인해 주세요."
                else -> "Firestore 오류: ${e.message}"
            }
            Result.failure(IllegalStateException(msg, e))
        } catch (e: Exception) {
            Log.e(TAG, "saveReviewBlock 알 수 없는 오류: ${e.message}", e)
            Result.failure(e)
        }
    }
}