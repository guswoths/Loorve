package com.loorve.data.repository

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.loorve.domain.model.Progress
import com.loorve.domain.repository.ProgressRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProgressRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ProgressRepository {

    private fun userProgressCollection(uid: String) =
        firestore.collection("users")
            .document(uid)
            .collection("progress")

    private fun progressDocument(uid: String, progressId: String) =
        userProgressCollection(uid).document(progressId)

    override suspend fun saveProgress(uid: String, progress: Progress): Result<Unit> {
        return try {
            require(uid.isNotBlank()) { "uid는 비어 있을 수 없습니다." }
            require(progress.examId.isNotBlank()) { "시험을 선택해주세요." }
            require(progress.content.isNotBlank()) { "학습 내용을 입력해주세요." }
            require(progress.completedCount >= 0) { "완료 수는 0 이상이어야 합니다." }
            require(progress.totalCount >= 0) { "전체 수는 0 이상이어야 합니다." }
            require(progress.completedCount <= progress.totalCount) {
                "완료 수는 전체 수보다 클 수 없습니다."
            }

            val isNewProgress = progress.progressId.isBlank()
            val documentRef = if (isNewProgress) {
                userProgressCollection(uid).document()
            } else {
                progressDocument(uid, progress.progressId)
            }

            val existingCreatedAt = if (isNewProgress) {
                null
            } else {
                documentRef.get().await().getTimestamp("createdAt")
            }

            val data = buildProgressMap(
                uid = uid,
                progress = progress,
                existingCreatedAt = existingCreatedAt
            )

            documentRef.set(data, SetOptions.merge()).await()

            Log.d(
                TAG,
                "progress 저장 완료: uid=$uid, progressId=${documentRef.id}, isNew=$isNewProgress"
            )

            Result.success(Unit)
        } catch (exception: FirebaseFirestoreException) {
            Log.e(
                TAG,
                "Firestore progress 저장 실패: code=${exception.code}, uid=$uid",
                exception
            )

            val message = when (exception.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                    "저장 권한이 없습니다. Firestore 보안 규칙과 로그인 상태를 확인해주세요."

                FirebaseFirestoreException.Code.UNAVAILABLE ->
                    "서버에 연결할 수 없습니다. 네트워크 연결을 확인해주세요."

                else -> "학습 진도를 저장하지 못했습니다: ${exception.message}"
            }

            Result.failure(IllegalStateException(message, exception))
        } catch (exception: IOException) {
            Log.e(TAG, "네트워크 오류로 progress 저장 실패: uid=$uid", exception)
            Result.failure(IllegalStateException("네트워크 연결을 확인해주세요.", exception))
        } catch (exception: Exception) {
            Log.e(TAG, "progress 저장 실패: uid=$uid", exception)
            Result.failure(exception)
        }
    }

    override suspend fun getProgressById(
        uid: String,
        progressId: String
    ): Result<Progress> {
        return try {
            require(uid.isNotBlank()) { "uid는 비어 있을 수 없습니다." }
            require(progressId.isNotBlank()) { "progressId는 비어 있을 수 없습니다." }

            val snapshot = progressDocument(uid, progressId).get().await()

            if (!snapshot.exists()) {
                Result.failure(NoSuchElementException("Progress not found: $progressId"))
            } else {
                val progress = snapshot.toProgress()
                    ?: return Result.failure(
                        IllegalStateException("Progress 매핑 실패: $progressId")
                    )

                Result.success(progress)
            }
        } catch (exception: Exception) {
            Log.e(
                TAG,
                "progress 단건 조회 실패: uid=$uid, progressId=$progressId",
                exception
            )
            Result.failure(exception)
        }
    }

    override suspend fun getProgressList(uid: String): Result<List<Progress>> {
        return try {
            require(uid.isNotBlank()) { "uid는 비어 있을 수 없습니다." }

            val snapshot = userProgressCollection(uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            Result.success(snapshot.documents.mapNotNull { it.toProgress() })
        } catch (exception: Exception) {
            Log.e(TAG, "progress 목록 조회 실패: uid=$uid", exception)
            Result.failure(exception)
        }
    }

    override suspend fun deleteProgress(uid: String, progressId: String): Result<Unit> {
        return try {
            require(uid.isNotBlank()) { "uid는 비어 있을 수 없습니다." }
            require(progressId.isNotBlank()) { "progressId는 비어 있을 수 없습니다." }

            progressDocument(uid, progressId).delete().await()

            Log.d(TAG, "progress 삭제 완료: uid=$uid, progressId=$progressId")
            Result.success(Unit)
        } catch (exception: Exception) {
            Log.e(
                TAG,
                "progress 삭제 실패: uid=$uid, progressId=$progressId",
                exception
            )
            Result.failure(exception)
        }
    }

    override fun observeProgressList(uid: String): Flow<List<Progress>> = callbackFlow {
        if (uid.isBlank()) {
            close(IllegalArgumentException("uid는 비어 있을 수 없습니다."))
            return@callbackFlow
        }

        val registration = userProgressCollection(uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                trySend(snapshot?.documents?.mapNotNull { it.toProgress() } ?: emptyList())
            }

        awaitClose {
            registration.remove()
        }
    }

    private fun buildProgressMap(
        uid: String,
        progress: Progress,
        existingCreatedAt: Timestamp?
    ): Map<String, Any> {
        val now = Timestamp.now()

        val createdAt = existingCreatedAt
            ?: progress.createdAt.takeIf { it > 0L }?.let { epochMs ->
                Timestamp(
                    epochMs / 1_000,
                    ((epochMs % 1_000) * 1_000_000).toInt()
                )
            }
            ?: now

        return mapOf(
            "uid" to uid,
            "examId" to progress.examId,
            "content" to progress.content.trim(),
            "completedCount" to progress.completedCount,
            "totalCount" to progress.totalCount,
            "isCompleted" to progress.isCompleted,
            "createdAt" to createdAt,
            "updatedAt" to now
        )
    }

    private fun DocumentSnapshot.toProgress(): Progress? {
        val data = data ?: return null

        return Progress(
            progressId = id,
            examId = data["examId"] as? String ?: "",
            content = data["content"] as? String ?: "",
            completedCount = (data["completedCount"] as? Number)?.toInt() ?: 0,
            totalCount = (data["totalCount"] as? Number)?.toInt() ?: 0,
            isCompleted = data["isCompleted"] as? Boolean ?: false,
            createdAt = getTimestamp("createdAt")?.toDate()?.time ?: 0L
        )
    }

    companion object {
        private const val TAG = "ProgressRepository"
    }
}
