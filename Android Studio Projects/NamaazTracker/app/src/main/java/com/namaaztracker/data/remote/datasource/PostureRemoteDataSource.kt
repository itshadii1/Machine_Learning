package com.namaaztracker.data.remote.datasource

import com.namaaztracker.data.remote.api.PostureApi
import com.namaaztracker.data.remote.dto.PosturePredictionDto
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

class PostureRemoteDataSource @Inject constructor(
    private val api: PostureApi,
) {
    suspend fun predict(jpegBytes: ByteArray): PosturePredictionDto {
        val requestBody = jpegBytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("frame", "frame.jpg", requestBody)
        return api.predict(part)
    }
}
