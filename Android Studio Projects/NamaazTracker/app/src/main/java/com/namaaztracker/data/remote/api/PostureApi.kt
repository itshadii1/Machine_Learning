package com.namaaztracker.data.remote.api

import com.namaaztracker.data.remote.dto.PosturePredictionDto
import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface PostureApi {
    @Multipart
    @POST("predict")
    suspend fun predict(@Part frame: MultipartBody.Part): PosturePredictionDto
}
