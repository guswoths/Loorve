package com.loorve.data.repository

import com.google.firebase.firestore.FirebaseFirestore
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
    private val firestore: FirebaseFirestore
) : ExamRepository {

    private val examsCollection = firestore.collection("exams")
    private val resultsCollection = firestore.collection("examResults")

    override fun getExamList(): Flow<List<Exam>> = callbackFlow {
        val listener = examsCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
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
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val exam = snapshot?.toObject(Exam::class.java)?.copy(id = snapshot.id)
                if (exam != null) {
                    trySend(exam)
                } else {
                    close(NoSuchElementException("Exam not found: $examId"))
                }
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
                if (error != null) {
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

    // ✅ 누락된 함수 - 핵심 수정 지점
    override suspend fun addExam(exam: Exam): Result<Unit> {
        return try {
            examsCollection.add(exam).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
