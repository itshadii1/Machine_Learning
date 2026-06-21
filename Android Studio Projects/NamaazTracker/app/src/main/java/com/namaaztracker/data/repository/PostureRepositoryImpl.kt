package com.namaaztracker.data.repository

import com.namaaztracker.data.remote.datasource.PostureRemoteDataSource
import com.namaaztracker.domain.model.Posture
import com.namaaztracker.domain.repository.PostureRepository
import javax.inject.Inject

class PostureRepositoryImpl @Inject constructor(
    private val dataSource: PostureRemoteDataSource,
) : PostureRepository {
    override suspend fun predictPosture(jpegBytes: ByteArray): Posture {
        val dto = dataSource.predict(jpegBytes)
        return Posture.fromLabel(dto.posture)
    }
}
