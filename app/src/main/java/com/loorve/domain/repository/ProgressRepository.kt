package com.loorve.domain.repository

import com.loorve.domain.model.Progress
import kotlinx.coroutines.flow.Flow

interface ProgressRepository {

    /** Progress 저장 (신규 생성 또는 업데이트) */
    suspend fun saveProgress(uid: String, progress: Progress): Result<Unit>

    /** 단건 Progress 조회 */
    suspend fun getProgressById(uid: String, progressId: String): Result<Progress>

    /** 전체 Progress 목록 조회 (최신순) */
    suspend fun getProgressList(uid: String): Result<List<Progress>>

    /** Progress 삭제 */
    suspend fun deleteProgress(uid: String, progressId: String): Result<Unit>

    /** Progress 목록 실시간 스트림 */
    fun observeProgressList(uid: String): Flow<List<Progress>>
}
