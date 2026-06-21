package com.namaaztracker.domain.model

enum class NamaazType(val displayName: String, val rakahCount: Int) {
    FAJR("Fajr", 2),
    ZUHR("Zuhr", 4),
    ASR("Asr", 4),
    MAGHRIB("Maghrib", 3),
    ISHA("Isha", 4),
}
