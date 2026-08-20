package com.loorve.domain.repository

import com.loorve.domain.model.Progress
import kotlinx.coroutines.flow.Flow

interface ProgressRepository {
    // 기존 메서드 (사용 여부 확인 필요 — 현재 Impl에 없으므로 제거 권장)
    // suspend fun getProgress(userId: String): Int
    // suspend fun updateProgress(userId: String, progress: Int)

    suspend fun saveProgress(uid: String, progress: Progress): Result<Unit>
    suspend fun getProgressById(uid: String, progressId: String): Result<Progress>
    suspend fun getProgressList(uid: String): Result<List<Progress>>
    suspend fun deleteProgress(uid: String, progressId: String): Result<Unit>
    fun observeProgressList(uid: String): Flow<List<Progress>>
}
