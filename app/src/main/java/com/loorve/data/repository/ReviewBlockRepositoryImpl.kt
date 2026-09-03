package com.loorve.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.loorve.domain.model.ReviewBlock
import com.loorve.domain.repository.ReviewBlockRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ReviewBlockRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ReviewBlockRepository {

    private fun reviewBlocksRef(uid: String) =
        firestore.collection("users")
            .document(uid)
            .collection("reviewBlocks")

    /** Firestore DocumentSnapshot → ReviewBlock 수동 매핑 (toObject 역직렬화 버그 방지) */
    private fun mapToReviewBlock(
        data: Map<String, Any?>,
        docId: String
    ): ReviewBlock = ReviewBlock(
        blockId      = docId,
        uid          = data["uid"]          as? String  ?: "",
        date         = data["date"]         as? String  ?: "",
        title        = data["title"]        as? String  ?: "",
        description  = data["description"]  as? String  ?: "",
        isCompleted  = data["isCompleted"]  as? Boolean ?: false,
        examDate     = (data["examDate"]    as? Number)?.toLong() ?: 0L,
        prepStartDate= (data["prepStartDate"] as? Number)?.toLong() ?: 0L,
        dailyCap     = (data["dailyCap"]    as? Number)?.toInt()  ?: 5,
        examName     = data["examName"]     as? String  ?: "",
        createdAt    = (data["createdAt"]   as? Number)?.toLong() ?: 0L,
        updatedAt    = (data["updatedAt"]   as? Number)?.toLong() ?: 0L
    )

    override suspend fun saveReviewBlock(reviewBlock: ReviewBlock): Result<Unit> {
        return runCatching {
            require(reviewBlock.uid.isNotBlank()) { "uid가 비어있습니다." }

            val docId = reviewBlock.blockId.ifBlank {
                reviewBlocksRef(reviewBlock.uid).document().id
            }

            val data = hashMapOf(
                "blockId"      to docId,
                "uid"          to reviewBlock.uid,
                "date"         to reviewBlock.date,
                "title"        to reviewBlock.title,
                "description"  to reviewBlock.description,
                "isCompleted"  to reviewBlock.isCompleted,
                "examDate"     to reviewBlock.examDate,
                "prepStartDate"to reviewBlock.prepStartDate,
                "dailyCap"     to reviewBlock.dailyCap,
                "examName"     to reviewBlock.examName,
                "createdAt"    to reviewBlock.createdAt,
                "updatedAt"    to reviewBlock.updatedAt
            )

            reviewBlocksRef(reviewBlock.uid)
                .document(docId)
                .set(data)
                .await()
        }
    }

    override suspend fun getReviewBlocks(uid: String): Result<List<ReviewBlock>> {
        return runCatching {
            reviewBlocksRef(uid)
                .get()
                .await()
                .documents
                .mapNotNull { snapshot ->
                    val data = snapshot.data ?: return@mapNotNull null
                    mapToReviewBlock(data, snapshot.id)
                }
        }
    }

    override suspend fun getReviewBlock(
        uid: String,
        blockId: String
    ): Result<ReviewBlock?> {
        return runCatching {
            require(uid.isNotBlank()) { "uid가 비어있습니다." }
            require(blockId.isNotBlank()) { "blockId가 비어있습니다." }

            val snapshot = reviewBlocksRef(uid)
                .document(blockId)
                .get()
                .await()

            if (!snapshot.exists()) null
            else {
                val data = snapshot.data ?: return@runCatching null
                mapToReviewBlock(data, snapshot.id)
            }
        }
    }

    override suspend fun deleteReviewBlock(uid: String, reviewBlockId: String): Result<Unit> {
        return runCatching {
            require(uid.isNotBlank()) { "uid가 비어있습니다." }
            require(reviewBlockId.isNotBlank()) { "reviewBlockId가 비어있습니다." }

            reviewBlocksRef(uid)
                .document(reviewBlockId)
                .delete()
                .await()
        }
    }
}