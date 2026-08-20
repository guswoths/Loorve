package com.loorve.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.loorve.domain.repository.ProgressRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ProgressRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ProgressRepository {

    override suspend fun getProgress(userId: String): Int {
        return try {
            val snapshot = firestore
                .collection("users")
                .document(userId)
                .get()
                .await()
            snapshot.getLong("progress")?.toInt() ?: 0
        } catch (e: Exception) {
            0
        }
    }

    override suspend fun updateProgress(userId: String, progress: Int) {
        firestore
            .collection("users")
            .document(userId)
            .update("progress", progress)
            .await()
    }
}
