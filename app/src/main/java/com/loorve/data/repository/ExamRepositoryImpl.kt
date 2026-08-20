package com.loorve.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.loorve.domain.model.Exam
import com.loorve.domain.model.ExamResult
import com.loorve.domain.repository.ExamRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ExamRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth
) : ExamRepository {

    private val examsCollection = firestore.collection("exams")
    private val resultsCollection = firestore.collection("examResults")

    private suspend fun requireAuthFresh(): String {
        val user = firebaseAuth.currentUser
            ?: throw SecurityException("로그인이 필요합니다.")
        user.getIdToken(true).await()
        return user.uid
    }

    // ✅ addExam 단 하나만 유지 (중복 제거)
    override suspend fun addExam(exam: Exam): Result<Unit> = runCatching {
        val uid = requireAuthFresh()
        val examWithOwner = exam.copy(createdBy = uid)
        examsCollection.add(examWithOwner).await()
        Unit
    }

    override fun getExamList(): Flow<List<Exam>> = callbackFlow {
        val uid = firebaseAuth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            awaitClose()
            return@callbackFlow
        }
        val listener = examsCollection
            .whereEqualTo("createdBy", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "getExamList 오류: ${error.message}", error)
                    close(error)
                    return@addSnapshotListener
                }
                val exams = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Exam::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(exams)
            }
        awaitClose { listener.remove() }
    }

    override fun getExamById(examId: String): Flow<Exam> = callbackFlow {
        val listener = examsCollection.document(examId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val exam = snapshot
                    ?.toObject(Exam::class.java)
                    ?.copy(id = snapshot.id)
                    ?: run {
                        close(NoSuchElementException("Exam $examId not found"))
                        return@addSnapshotListener
                    }
                trySend(exam)
            }
        awaitClose { listener.remove() }
    }

    // ✅ requireAuth() → requireAuthFresh() 로 수정
    override suspend fun saveExamResult(result: ExamResult): Result<Unit> = runCatching {
        requireAuthFresh()
        resultsCollection.add(result).await()
        Unit
    }

    override fun getExamResults(userId: String): Flow<List<ExamResult>> = callbackFlow {
        val listener = resultsCollection
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val results = snapshot?.documents
                    ?.mapNotNull { it.toObject(ExamResult::class.java) }
                    ?: emptyList()
                trySend(results)
            }
        awaitClose { listener.remove() }
    }

    companion object {
        private const val TAG = "ExamRepository"
    }
}
