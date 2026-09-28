package com.loorve.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.loorve.data.model.StudyRecordDto
import com.loorve.data.model.toDto
import com.loorve.domain.model.StudyRecord
import com.loorve.domain.repository.StudyRecordRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class StudyRecordRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : StudyRecordRepository {

    private fun studyRecordsRef(uid: String) =
        firestore.collection("users").document(uid).collection("studyRecords")

    override suspend fun saveStudyRecord(record: StudyRecord): Result<String> = runCatching {
        val uid = auth.currentUser?.uid
            ?: throw SecurityException("인증되지 않은 사용자입니다.")
        require(record.uid == uid) { "본인의 학습기록만 저장할 수 있습니다." }

        val dto = record.toDto()
        val docRef = if (record.id.isBlank()) {
            studyRecordsRef(uid).document()
        } else {
            studyRecordsRef(uid).document(record.id)
        }

        val data = hashMapOf(
            "id" to docRef.id,
            "uid" to dto.uid,
            "blockId" to dto.blockId,
            "examId" to dto.examId,
            "title" to dto.title,
            "content" to dto.content,
            "learningDate" to dto.learningDate,
            "examDate" to dto.examDate,
            "prepStartDate" to dto.prepStartDate,
            "recommendedCompletionDate" to dto.recommendedCompletionDate,
            "stage" to dto.stage,
            "successCount" to dto.successCount,
            "stability" to dto.stability,
            "completionRate" to dto.completionRate,
            "plannedReviewCount" to dto.plannedReviewCount,
            "completedReviewCount" to dto.completedReviewCount,
            "isAtRisk" to dto.isAtRisk,
            "difficulty" to dto.difficulty,
            "importance" to dto.importance,
            "initialMastery" to dto.initialMastery,
            "estimatedReviewMinutes" to dto.estimatedReviewMinutes,
            "optionalMinReviewCount" to dto.optionalMinReviewCount,
            "createdAt" to if (record.id.isBlank()) FieldValue.serverTimestamp()
            else FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        docRef.set(data).await()
        docRef.id
    }

    override suspend fun getStudyRecords(
        uid: String,
        blockId: String
    ): Result<List<StudyRecord>> = runCatching {
        val currentUid = auth.currentUser?.uid
            ?: throw SecurityException("인증되지 않은 사용자입니다.")
        require(currentUid == uid) { "본인의 학습기록만 조회할 수 있습니다." }

        studyRecordsRef(uid)
            .whereEqualTo("blockId", blockId)
            .get().await()
            .documents
            .mapNotNull { doc ->
                doc.toObject(StudyRecordDto::class.java)
                    ?.copy(id = doc.id)
                    ?.toDomain()
            }
    }

    override suspend fun updateStudyRecord(record: StudyRecord): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid
            ?: throw SecurityException("인증되지 않은 사용자입니다.")
        require(record.uid == uid) { "본인의 학습기록만 수정할 수 있습니다." }

        studyRecordsRef(uid).document(record.id)
            .update(
                mapOf(
                    "stage" to record.stage,
                    "successCount" to record.successCount,
                    "stability" to record.stability,
                    "completedReviewCount" to record.completedReviewCount,
                    "isAtRisk" to record.isAtRisk,
                    "difficulty" to record.difficulty.name,
                    "importance" to record.importance.name,
                    "initialMastery" to record.initialMastery,
                    "estimatedReviewMinutes" to record.estimatedReviewMinutes,
                    "optionalMinReviewCount" to record.optionalMinReviewCount,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
    }

    // ✅ [추가] 개별 학습기록 및 연관된 복습 일정, 캘린더 기록, 알림 일괄 삭제
    override suspend fun deleteStudyRecord(
        uid: String,
        record: StudyRecord
    ): Result<Unit> = runCatching {
        val currentUid = auth.currentUser?.uid
            ?: throw SecurityException("인증되지 않은 사용자입니다.")
        require(currentUid == uid) { "본인의 학습기록만 삭제할 수 있습니다." }
        require(record.id.isNotBlank()) { "삭제할 학습기록의 ID가 없습니다." }

        val toDeleteRefs = mutableListOf<DocumentReference>()

        // 1) 학습기록 문서
        toDeleteRefs.add(studyRecordsRef(uid).document(record.id))

        // 2) 연관된 복습 일정 항목(reviewScheduleItems) 일괄 조회 (studyRecordId 기준)
        val schedulesSnapshot = firestore.collection("users")
            .document(uid)
            .collection("reviewScheduleItems")
            .whereEqualTo("studyRecordId", record.id)
            .get()
            .await()
        toDeleteRefs.addAll(schedulesSnapshot.documents.map { it.reference })

        // 3) 혹시 originProgressId로 저장된 reviewScheduleItems도 조회
        val schedulesByOriginSnapshot = firestore.collection("users")
            .document(uid)
            .collection("reviewScheduleItems")
            .whereEqualTo("originProgressId", record.id)
            .get()
            .await()
        toDeleteRefs.addAll(schedulesByOriginSnapshot.documents.map { it.reference })

        // 4) 연관된 구버전 복습 일정(reviewSchedules) 일괄 조회 (originProgressId 기준)
        val legacySchedulesSnapshot = firestore.collection("users")
            .document(uid)
            .collection("reviewSchedules")
            .whereEqualTo("originProgressId", record.id)
            .get()
            .await()
        toDeleteRefs.addAll(legacySchedulesSnapshot.documents.map { it.reference })

        // 5) 혹시 studyRecordId로 저장된 구버전 복습 일정(reviewSchedules)도 조회
        val legacySchedulesByRecordIdSnapshot = firestore.collection("users")
            .document(uid)
            .collection("reviewSchedules")
            .whereEqualTo("studyRecordId", record.id)
            .get()
            .await()
        toDeleteRefs.addAll(legacySchedulesByRecordIdSnapshot.documents.map { it.reference })

        // 6) 연관된 알림 이벤트(reviewNotificationOutbox) 일괄 조회 (studyRecordId 기준)
        val outboxSnapshot = firestore.collection("users")
            .document(uid)
            .collection("reviewNotificationOutbox")
            .whereEqualTo("studyRecordId", record.id)
            .get()
            .await()
        toDeleteRefs.addAll(outboxSnapshot.documents.map { it.reference })

        // 7) 일괄 원자적(Atomic) 배치 삭제 수행 (중복 참조 제거 후 500개씩 청크 분할 커밋)
        val distinctRefs = toDeleteRefs.distinctBy { it.path }
        distinctRefs.chunked(500).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { batch.delete(it) }
            batch.commit().await()
        }
    }

    // ✅ [추가] 기간별 학습기록 조회 (홈 캘린더 dot 연동용)
    override suspend fun getStudyRecordsByDateRange(
        uid: String,
        startDateMillis: Long,
        endDateMillis: Long
    ): Result<List<StudyRecord>> = runCatching {
        val currentUid = auth.currentUser?.uid
            ?: throw SecurityException("인증되지 않은 사용자입니다.")
        require(currentUid == uid) { "본인의 학습기록만 조회할 수 있습니다." }

        studyRecordsRef(uid)
            .whereGreaterThanOrEqualTo("learningDate", startDateMillis)
            .whereLessThanOrEqualTo("learningDate", endDateMillis)
            .get().await()
            .documents
            .mapNotNull { doc ->
                doc.toObject(StudyRecordDto::class.java)
                    ?.copy(id = doc.id)
                    ?.toDomain()
            }
    }

    // ✅ [추가] 사용자 전체 학습기록 조회
    override suspend fun getAllStudyRecords(
        uid: String
    ): Result<List<StudyRecord>> = runCatching {
        val currentUid = auth.currentUser?.uid
            ?: throw SecurityException("인증되지 않은 사용자입니다.")
        require(currentUid == uid) { "본인의 학습기록만 조회할 수 있습니다." }

        studyRecordsRef(uid)
            .get().await()
            .documents
            .mapNotNull { doc ->
                doc.toObject(StudyRecordDto::class.java)
                    ?.copy(id = doc.id)
                    ?.toDomain()
            }
    }

    // ✅ [추가] 실시간 학습기록 관찰 (스냅샷 리스너)
    override fun observeStudyRecords(uid: String): Flow<List<StudyRecord>> = callbackFlow {
        if (uid.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val registration = studyRecordsRef(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val records = snapshot?.documents.orEmpty().mapNotNull { doc ->
                    doc.toObject(StudyRecordDto::class.java)
                        ?.copy(id = doc.id)
                        ?.toDomain()
                }
                trySend(records)
            }

        awaitClose {
            registration.remove()
        }
    }

    // ✅ [추가] 특정 블록에 속한 모든 학습기록 및 연관 복습일정/알림 일괄 삭제
    override suspend fun deleteStudyRecordsByBlockId(
        uid: String,
        blockId: String
    ): Result<Unit> = runCatching {
        val currentUid = auth.currentUser?.uid
            ?: throw SecurityException("인증되지 않은 사용자입니다.")
        require(currentUid == uid) { "본인의 학습기록만 삭제할 수 있습니다." }
        require(blockId.isNotBlank()) { "blockId가 비어있습니다." }

        val snapshot = studyRecordsRef(uid)
            .whereEqualTo("blockId", blockId)
            .get()
            .await()

        val toDeleteRefs = mutableListOf<DocumentReference>()
        toDeleteRefs.addAll(snapshot.documents.map { it.reference })

        val recordIds = snapshot.documents.map { it.id }.toSet()

        for (recordId in recordIds) {
            val schedulesSnapshot = firestore.collection("users")
                .document(uid)
                .collection("reviewScheduleItems")
                .whereEqualTo("studyRecordId", recordId)
                .get()
                .await()
            toDeleteRefs.addAll(schedulesSnapshot.documents.map { it.reference })

            val schedulesByOrigin = firestore.collection("users")
                .document(uid)
                .collection("reviewScheduleItems")
                .whereEqualTo("originProgressId", recordId)
                .get()
                .await()
            toDeleteRefs.addAll(schedulesByOrigin.documents.map { it.reference })

            val legacySnapshot = firestore.collection("users")
                .document(uid)
                .collection("reviewSchedules")
                .whereEqualTo("originProgressId", recordId)
                .get()
                .await()
            toDeleteRefs.addAll(legacySnapshot.documents.map { it.reference })

            val legacyByRecordId = firestore.collection("users")
                .document(uid)
                .collection("reviewSchedules")
                .whereEqualTo("studyRecordId", recordId)
                .get()
                .await()
            toDeleteRefs.addAll(legacyByRecordId.documents.map { it.reference })

            val outboxSnapshot = firestore.collection("users")
                .document(uid)
                .collection("reviewNotificationOutbox")
                .whereEqualTo("studyRecordId", recordId)
                .get()
                .await()
            toDeleteRefs.addAll(outboxSnapshot.documents.map { it.reference })
        }

        val distinctRefs = toDeleteRefs.distinctBy { it.path }
        distinctRefs.chunked(500).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { batch.delete(it) }
            batch.commit().await()
        }
    }
}