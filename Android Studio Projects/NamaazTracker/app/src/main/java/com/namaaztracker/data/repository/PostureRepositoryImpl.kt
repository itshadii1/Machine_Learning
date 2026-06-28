package com.namaaztracker.data.repository

import com.namaaztracker.data.local.OnDevicePostureDetector
import com.namaaztracker.domain.model.Posture
import com.namaaztracker.domain.repository.PostureRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class PostureRepositoryImpl @Inject constructor(
    private val detector: OnDevicePostureDetector,
) : PostureRepository {

    override suspend fun predictPosture(jpegBytes: ByteArray): Posture =
        withContext(Dispatchers.Default) {
            detector.detect(jpegBytes)
        }
}
