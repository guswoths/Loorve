// 경로: app/src/main/java/com/loorve/data/repository/ExamRepositoryImpl.kt
package com.loorve.data.repository

import com.google.firebase.auth.FirebaseAuth
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
    private val auth: FirebaseAuth          // ← FirebaseAuth 추가
) : ExamRepository {

    // users/{uid}/exams 경로를 동적으로 반환
    private fun userExamsCollection() = auth.currentUser?.uid
        ?.let { uid -> firestore.collection("users").document(uid).collection("exams") }
        ?: throw IllegalStateException("로그인 상태가 아닙니다.")

    private val resultsCollection = firestore.collection("examResults")

    override fun getExamList(): Flow<List<Exam>> = callbackFlow {
        val listener = userExamsCollection()
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
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
        val listener = userExamsCollection().document(examId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
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
            Result.failure(e)
        }
    }

    override fun getExamResults(userId: String): Flow<List<ExamResult>> = callbackFlow {
        val listener = resultsCollection
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val results = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ExamResult::class.java)
                } ?: emptyList()
                trySend(results)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun addExam(exam: Exam): Result<Unit> {
        return try {
            val data = hashMapOf(
                "subjectName" to exam.subjectName,
                "examDate"    to exam.examDate,
                "createdAt"   to com.google.firebase.Timestamp.now()
            )
            userExamsCollection().add(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
