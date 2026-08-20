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

    private fun requireAuth(): String =
        firebaseAuth.currentUser?.uid
            ?: throw SecurityException("로그인이 필요합니다.")

    /**
     * 현재 로그인 사용자가 생성한 시험 목록만 실시간 스트림으로 반환.
     * Firestore 보안 규칙의 createdBy == request.auth.uid 조건과 쿼리를 일치시킴.
     */
    override fun getExamList(): Flow<List<Exam>> = callbackFlow {
        val uid = firebaseAuth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            awaitClose()
            return@callbackFlow
        }

        val listener = examsCollection
            .whereEqualTo("createdBy", uid)   // ← 핵심 수정: 소유자 필터 추가
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

    override suspend fun saveExamResult(result: ExamResult): Result<Unit> = runCatching {
        requireAuth()
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

    override suspend fun addExam(exam: Exam): Result<Unit> = runCatching {
        val uid = requireAuth()
        val examWithOwner = exam.copy(createdBy = uid)
        examsCollection.add(examWithOwner).await()
        Unit
    }

    companion object {
        private const val TAG = "ExamRepository"
    }
}
