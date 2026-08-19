package com.loorve.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.loorve.data.model.ExamDto
import com.loorve.domain.model.Exam
import com.loorve.domain.model.ExamResult
import com.loorve.domain.repository.ExamRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExamRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ExamRepository {

    private val resultsCollection = firestore.collection("examResults")

    /**
     * ✅ 수정 B: throw 대신 null을 반환하여 Flow caller가 close(error)로 안전하게 처리.
     * 비로그인 상태에서 callbackFlow 내부에서 throw 시 앱 크래시 방지.
     */
    private fun userExamsCollectionOrNull(): CollectionReference? =
        auth.currentUser?.uid
            ?.let { uid -> firestore.collection("users").document(uid).collection("exams") }

    override fun getExamList(): Flow<List<Exam>> = callbackFlow {
        // ✅ 수정 B: null 체크 후 close로 안전하게 에러 전파
        val collection = userExamsCollectionOrNull()
        if (collection == null) {
            Log.w(TAG, "getExamList 실패: 로그인 상태가 아닙니다.")
            close(IllegalStateException("로그인 상태가 아닙니다."))
            return@callbackFlow
        }

        val listener = collection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "getExamList 스냅샷 오류", error)
                    close(error)
                    return@addSnapshotListener
                }
                val exams = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ExamDto::class.java)?.copy(id = doc.id)?.toDomain()
                } ?: emptyList()
                trySend(exams)
            }
        awaitClose { listener.remove() }
    }

    override fun getExamById(examId: String): Flow<Exam> = callbackFlow {
        // ✅ 수정 B: null 체크 후 close로 안전하게 에러 전파
        val collection = userExamsCollectionOrNull()
        if (collection == null) {
            Log.w(TAG, "getExamById 실패: 로그인 상태가 아닙니다.")
            close(IllegalStateException("로그인 상태가 아닙니다."))
            return@callbackFlow
        }

        val listener = collection.document(examId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "getExamById 스냅샷 오류 (examId=$examId)", error)
                    close(error)
                    return@addSnapshotListener
                }
                val exam = snapshot?.toObject(ExamDto::class.java)
                    ?.copy(id = snapshot.id)
                    ?.toDomain()
                if (exam != null) trySend(exam)
                else close(NoSuchElementException("Exam not found: $examId"))
            }
        awaitClose { listener.remove() }
    }

    override suspend fun saveExamResult(result: ExamResult): Result<Unit> {
        return try {
            resultsCollection.add(result).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "saveExamResult 실패", e)
            Result.failure(e)
        }
    }

    override fun getExamResults(userId: String): Flow<List<ExamResult>> = callbackFlow {
        val listener = resultsCollection
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "getExamResults 스냅샷 오류 (userId=$userId)", error)
                    close(error)
                    return@addSnapshotListener
                }
                val results = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ExamResult::class.java)
                } ?: emptyList()
                trySend(results)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun addExam(exam: Exam): Result<Unit> {
        return try {
            // ✅ 수정 B: null 체크 후 Result.failure로 안전하게 처리
            val collection = userExamsCollectionOrNull()
                ?: return Result.failure(IllegalStateException("로그인 상태가 아닙니다."))

            val data = hashMapOf(
                "subjectName" to exam.subjectName,
                "examDate"    to exam.examDate,
                "createdAt"   to com.google.firebase.Timestamp.now()
            )
            collection.add(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "addExam 실패 (subjectName=${exam.subjectName})", e)
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "ExamRepository"
    }
}
