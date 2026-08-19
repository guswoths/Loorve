// app/src/main/java/com/loorve/data/repository/ExamRepositoryImpl.kt
package com.loorve.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.loorve.domain.model.Exam
import com.loorve.domain.model.ExamResult
import com.loorve.domain.repository.ExamRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ExamRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ExamRepository {

    private val examsCollection = firestore.collection("exams")
    private val resultsCollection = firestore.collection("examResults")

    override fun getExamList(): Flow<List<Exam>> = callbackFlow {
        val listener = examsCollection.addSnapshotListener { snapshot, error ->
            if (error != null) { close(error); return@addSnapshotListener }
            val exams = snapshot?.documents?.mapNotNull { it.toObject(Exam::class.java) } ?: emptyList()
            trySend(exams)
        }
        awaitClose { listener.remove() }
    }

    override fun getExamById(examId: String): Flow<Exam> = callbackFlow {
        val listener = examsCollection.document(examId).addSnapshotListener { snapshot, error ->
            if (error != null) { close(error); return@addSnapshotListener }
            val exam = snapshot?.toObject(Exam::class.java)
                ?: run { close(NoSuchElementException("Exam $examId not found")); return@addSnapshotListener }
            trySend(exam)
        }
        awaitClose { listener.remove() }
    }

    override suspend fun saveExamResult(result: ExamResult): Result<Unit> = runCatching {
        resultsCollection.add(result).await()
        Unit
    }

    override fun getExamResults(userId: String): Flow<List<ExamResult>> = callbackFlow {
        val listener = resultsCollection
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val results = snapshot?.documents?.mapNotNull { it.toObject(ExamResult::class.java) } ?: emptyList()
                trySend(results)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun addExam(exam: Exam): Result<Unit> = runCatching {
        examsCollection.add(exam).await()
        Unit
    }
}
