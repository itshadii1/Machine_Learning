package com.namaaztracker.data.remote.dto

import com.google.gson.annotations.SerializedName

data class PosturePredictionDto(
    @SerializedName("posture") val posture: String,
    @SerializedName("confidence") val confidence: Float,
)
