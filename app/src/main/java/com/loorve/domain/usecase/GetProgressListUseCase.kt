package com.loorve.domain.usecase

import com.loorve.domain.model.Progress
import com.loorve.domain.repository.ProgressRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetProgressListUseCase @Inject constructor(
    private val progressRepository: ProgressRepository
) {
    operator fun invoke(uid: String): Flow<List<Progress>> =
        progressRepository.observeProgressList(uid)
}
