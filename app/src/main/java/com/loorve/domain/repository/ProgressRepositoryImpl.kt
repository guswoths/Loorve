package com.loorve.data.repository

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.loorve.domain.model.ProgressEntity
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

    override suspend fun saveProgress(uid: String, progress: ProgressEntity): Result<Unit> {
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

    override suspend fun getProgressById(uid: String, progressId: String): Result<ProgressEntity> {
        return try {
            require(uid.isNotBlank()) { "uid는 비어 있을 수 없습니다." }
            require(progressId.isNotBlank()) { "progressId는 비어 있을 수 없습니다." }

            val snapshot = progressDocument(uid, progressId).get().await()

            if (!snapshot.exists()) {
                Result.failure(NoSuchElementException("Progress not found: $progressId"))
            } else {
                val entity = snapshot.toProgressEntity()
                    ?: return Result.failure(IllegalStateException("Progress 매핑 실패: $progressId"))

                Result.success(entity)
            }
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "progress 단건 조회 실패 - 잘못된 파라미터 (uid=$uid, progressId=$progressId)", e)
            Result.failure(e)
        } catch (e: IOException) {
            Log.e(TAG, "progress 단건 조회 실패 - 네트워크 오류 (uid=$uid, progressId=$progressId)", e)
            Result.failure(Exception("네트워크 연결을 확인해주세요.", e))
        } catch (e: Exception) {
            Log.e(TAG, "progress 단건 조회 실패 (uid=$uid, progressId=$progressId)", e)
            Result.failure(Exception("progress 조회 중 오류가 발생했습니다.", e))
        }
    }

    override suspend fun getProgressList(uid: String): Result<List<ProgressEntity>> {
        return try {
            require(uid.isNotBlank()) { "uid는 비어 있을 수 없습니다." }

            val snapshot = userProgressCollection(uid)
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val progressList = snapshot.documents.mapNotNull { document ->
                document.toProgressEntity()
            }

            Result.success(progressList)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "progress 목록 조회 실패 - 잘못된 파라미터 (uid=$uid)", e)
            Result.failure(e)
        } catch (e: IOException) {
            Log.e(TAG, "progress 목록 조회 실패 - 네트워크 오류 (uid=$uid)", e)
            Result.failure(Exception("네트워크 연결을 확인해주세요.", e))
        } catch (e: Exception) {
            Log.e(TAG, "progress 목록 조회 실패 (uid=$uid)", e)
            Result.failure(Exception("progress 목록 조회 중 오류가 발생했습니다.", e))
        }
    }

    override suspend fun deleteProgress(uid: String, progressId: String): Result<Unit> {
        return try {
            require(uid.isNotBlank()) { "uid는 비어 있을 수 없습니다." }
            require(progressId.isNotBlank()) { "progressId는 비어 있을 수 없습니다." }

            progressDocument(uid, progressId).delete().await()
            Log.d(TAG, "progress 삭제 완료 (uid=$uid, progressId=$progressId)")
            Result.success(Unit)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "progress 삭제 실패 - 잘못된 파라미터 (uid=$uid, progressId=$progressId)", e)
            Result.failure(e)
        } catch (e: IOException) {
            Log.e(TAG, "progress 삭제 실패 - 네트워크 오류 (uid=$uid, progressId=$progressId)", e)
            Result.failure(Exception("네트워크 연결을 확인해주세요.", e))
        } catch (e: Exception) {
            Log.e(TAG, "progress 삭제 실패 (uid=$uid, progressId=$progressId)", e)
            Result.failure(Exception("progress 삭제 중 오류가 발생했습니다.", e))
        }
    }

    override fun observeProgressList(uid: String): Flow<List<ProgressEntity>> = callbackFlow {
        if (uid.isBlank()) {
            close(IllegalArgumentException("uid는 비어 있을 수 없습니다."))
            return@callbackFlow
        }

        val listenerRegistration = userProgressCollection(uid)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "progress 실시간 구독 실패 (uid=$uid)", error)
                    close(error)
                    return@addSnapshotListener
                }

                val progressList = snapshot?.documents?.mapNotNull { document ->
                    document.toProgressEntity()
                } ?: emptyList()

                trySend(progressList)
            }

        awaitClose { listenerRegistration.remove() }
    }

    private fun buildProgressMap(
        uid: String,
        progress: ProgressEntity,
        existingCreatedAt: Timestamp?
    ): Map<String, Any?> {
        val now = Timestamp.now()

        return mapOf(
            "uid" to uid,
            "examId" to progress.examId,
            "subjectName" to progress.subjectName,
            "content" to progress.content,
            "studyDate" to progress.studyDate,
            "reviewCount" to progress.reviewCount,
            "isCompleted" to progress.isCompleted,
            "createdAt" to (existingCreatedAt ?: progress.createdAt?.let { Timestamp(it.seconds, it.nanoseconds) } ?: now),
            "updatedAt" to now
        )
    }

    private fun DocumentSnapshot.toProgressEntity(): ProgressEntity? {
        val data = data ?: return null

        return ProgressEntity(
            progressId = id,
            uid = data["uid"] as? String ?: "",
            examId = data["examId"] as? String ?: "",
            subjectName = data["subjectName"] as? String ?: "",
            content = data["content"] as? String ?: "",
            studyDate = data["studyDate"] as? String ?: "",
            reviewCount = (data["reviewCount"] as? Long)?.toInt() ?: 0,
            isCompleted = data["isCompleted"] as? Boolean ?: false,
            createdAt = getTimestamp("createdAt"),
            updatedAt = getTimestamp("updatedAt")
        )
    }

    companion object {
        private const val TAG = "ProgressRepository"
    }
}
