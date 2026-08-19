package com.loorve.data.repository

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
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

            val documentRef = if (progress.progressId.isBlank()) {
                userProgressCollection(uid).document()
            } else {
                progressDocument(uid, progress.progressId)
            }

            val existingSnapshot = documentRef.get().await()
            val data = buildProgressMap(
                uid = uid,
                progress = progress,
                existingCreatedAt = existingSnapshot.getTimestamp("createdAt")
            )

            documentRef.set(data, SetOptions.merge()).await()
            Log.d(TAG, "progress 저장 완료 (uid=$uid, progressId=${documentRef.id})")
            Result.success(Unit)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "progress 저장 실패 - 잘못된 파라미터 (uid=$uid)", e)
            Result.failure(e)
        } catch (e: IOException) {
            Log.e(TAG, "progress 저장 실패 - 네트워크 오류 (uid=$uid)", e)
            Result.failure(Exception("네트워크 연결을 확인해주세요.", e))
        } catch (e: Exception) {
            Log.e(TAG, "progress 저장 실패 (uid=$uid)", e)
            Result.failure(Exception("progress 저장 중 오류가 발생했습니다.", e))
        }
    }

    override suspend fun getProgressById(uid: String, progressId: String): Result<Progress> {
        return try {
            require(uid.isNotBlank()) { "uid는 비어 있을 수 없습니다." }
            require(progressId.isNotBlank()) { "progressId는 비어 있을 수 없습니다." }
            val snapshot = progressDocument(uid, progressId).get().await()
            if (!snapshot.exists()) {
                Result.failure(NoSuchElementException("Progress not found: $progressId"))
            } else {
                val p = snapshot.toProgress()
                    ?: return Result.failure(IllegalStateException("Progress 매핑 실패: $progressId"))
                Result.success(p)
            }
        } catch (e: Exception) {
            Log.e(TAG, "progress 단건 조회 실패 (uid=$uid, progressId=$progressId)", e)
            Result.failure(e)
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
        } catch (e: Exception) {
            Log.e(TAG, "progress 목록 조회 실패 (uid=$uid)", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteProgress(uid: String, progressId: String): Result<Unit> {
        return try {
            require(uid.isNotBlank()) { "uid는 비어 있을 수 없습니다." }
            require(progressId.isNotBlank()) { "progressId는 비어 있을 수 없습니다." }
            progressDocument(uid, progressId).delete().await()
            Log.d(TAG, "progress 삭제 완료 (uid=$uid, progressId=$progressId)")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "progress 삭제 실패 (uid=$uid, progressId=$progressId)", e)
            Result.failure(e)
        }
    }

    override fun observeProgressList(uid: String): Flow<List<Progress>> = callbackFlow {
        if (uid.isBlank()) {
            close(IllegalArgumentException("uid는 비어 있을 수 없습니다."))
            return@callbackFlow
        }
        val reg = userProgressCollection(uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.documents?.mapNotNull { it.toProgress() } ?: emptyList())
            }
        awaitClose { reg.remove() }
    }

    // ── 내부 헬퍼 ──────────────────────────────────────────────────────────

    private fun buildProgressMap(
        uid: String,
        progress: Progress,
        existingCreatedAt: Timestamp?
    ): Map<String, Any?> {
        val nowTimestamp = Timestamp.now()
        val createdAtTimestamp = existingCreatedAt
            ?: if (progress.createdAt > 0L)
                Timestamp(progress.createdAt / 1000, ((progress.createdAt % 1000) * 1_000_000).toInt())
               else nowTimestamp

        return mapOf(
            "uid"            to uid,
            "examId"         to progress.examId,
            "content"        to progress.content,
            "completedCount" to progress.completedCount,
            "totalCount"     to progress.totalCount,
            "isCompleted"    to progress.isCompleted,
            "createdAt"      to createdAtTimestamp,
            "updatedAt"      to nowTimestamp
        )
    }

    private fun DocumentSnapshot.toProgress(): Progress? {
        val data = data ?: return null
        return Progress(
            progressId     = id,
            examId         = data["examId"] as? String ?: "",
            content        = data["content"] as? String ?: "",
            completedCount = (data["completedCount"] as? Long)?.toInt() ?: 0,
            totalCount     = (data["totalCount"] as? Long)?.toInt() ?: 0,
            isCompleted    = data["isCompleted"] as? Boolean ?: false,
            createdAt      = getTimestamp("createdAt")?.toDate()?.time ?: 0L
        )
    }

    companion object {
        private const val TAG = "ProgressRepository"
    }
}
