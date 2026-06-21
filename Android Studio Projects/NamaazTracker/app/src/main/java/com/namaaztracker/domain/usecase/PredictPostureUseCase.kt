package com.namaaztracker.domain.usecase

import com.namaaztracker.domain.model.Posture
import com.namaaztracker.domain.repository.PostureRepository
import javax.inject.Inject

class PredictPostureUseCase @Inject constructor(
    private val repository: PostureRepository,
) {
    suspend operator fun invoke(jpegBytes: ByteArray): Posture =
        repository.predictPosture(jpegBytes)
}
