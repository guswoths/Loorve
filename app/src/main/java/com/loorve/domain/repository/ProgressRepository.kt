package com.loorve.domain.repository

import com.loorve.domain.model.Progress
import kotlinx.coroutines.flow.Flow

interface ProgressRepository {
    suspend fun saveProgress(uid: String, progress: Progress): Result<Unit>
    suspend fun getProgressById(uid: String, progressId: String): Result<Progress>
    suspend fun getProgressList(uid: String): Result<List<Progress>>
    suspend fun deleteProgress(uid: String, progressId: String): Result<Unit>
    fun observeProgressList(uid: String): Flow<List<Progress>>
}
