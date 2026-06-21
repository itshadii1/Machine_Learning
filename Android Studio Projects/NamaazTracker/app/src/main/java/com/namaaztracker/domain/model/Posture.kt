package com.namaaztracker.domain.model

enum class Posture(val displayName: String, val apiLabel: String) {
    QIAM("Qiam", "qiam"),
    RUKOOH("Rukooh", "rukooh"),
    SAJDAH("Sajdah", "sajdah"),
    JULSA("Julsa", "julsa"),
    UNKNOWN("–", "unknown");

    companion object {
        fun fromLabel(label: String): Posture =
            entries.firstOrNull { it.apiLabel == label.lowercase() } ?: UNKNOWN
    }
}
