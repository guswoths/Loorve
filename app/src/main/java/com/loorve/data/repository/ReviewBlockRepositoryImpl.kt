package com.loorve.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.loorve.domain.model.ReviewBlock
import com.loorve.domain.repository.ReviewBlockRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ReviewBlockRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ReviewBlockRepository {

    private fun reviewBlocksCollection(uid: String) =
        firestore.collection("users")
            .document(uid)
            .collection("reviewBlocks")

    override suspend fun saveReviewBlock(
        uid: String,
        reviewBlock: ReviewBlock
    ): Result<Unit> = runCatching {
        require(uid.isNotBlank()) { "uid는 비어 있을 수 없습니다." }
        require(reviewBlock.blockId.isNotBlank()) {
            "reviewBlock.blockId는 비어 있을 수 없습니다."
        }

        reviewBlocksCollection(uid)
            .document(reviewBlock.blockId)
            .set(reviewBlock.copy(uid = uid))
            .await()
    }

    override fun getReviewBlocksByDate(
        uid: String,
        date: String
    ): Flow<List<ReviewBlock>> = callbackFlow {
        if (uid.isBlank()) {
            close(IllegalArgumentException("uid는 비어 있을 수 없습니다."))
            return@callbackFlow
        }

        if (date.isBlank()) {
            close(IllegalArgumentException("date는 비어 있을 수 없습니다."))
            return@callbackFlow
        }

        val listener = reviewBlocksCollection(uid)
            .whereEqualTo("date", date)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                trySend(
                    snapshot?.toObjects(ReviewBlock::class.java).orEmpty()
                )
            }

        awaitClose { listener.remove() }
    }

    override fun getReviewBlocksByDateRange(
        uid: String,
        startDate: String,
        endDate: String
    ): Flow<List<ReviewBlock>> = callbackFlow {
        if (uid.isBlank()) {
            close(IllegalArgumentException("uid는 비어 있을 수 없습니다."))
            return@callbackFlow
        }

        if (startDate.isBlank() || endDate.isBlank()) {
            close(IllegalArgumentException("날짜 범위는 비어 있을 수 없습니다."))
            return@callbackFlow
        }

        val listener = reviewBlocksCollection(uid)
            .whereGreaterThanOrEqualTo("date", startDate)
            .whereLessThanOrEqualTo("date", endDate)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                trySend(
                    snapshot?.toObjects(ReviewBlock::class.java).orEmpty()
                )
            }

        awaitClose { listener.remove() }
    }

    override suspend fun deleteReviewBlock(
        uid: String,
        reviewBlockId: String
    ): Result<Unit> = runCatching {
        require(uid.isNotBlank()) { "uid는 비어 있을 수 없습니다." }
        require(reviewBlockId.isNotBlank()) {
            "reviewBlockId는 비어 있을 수 없습니다."
        }

        reviewBlocksCollection(uid)
            .document(reviewBlockId)
            .delete()
            .await()
    }
}