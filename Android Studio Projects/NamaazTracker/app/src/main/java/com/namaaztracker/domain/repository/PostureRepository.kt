package com.namaaztracker.domain.repository

import com.namaaztracker.domain.model.Posture

interface PostureRepository {
    suspend fun predictPosture(jpegBytes: ByteArray): Posture
}
