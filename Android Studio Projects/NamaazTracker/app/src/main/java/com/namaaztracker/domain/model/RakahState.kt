package com.namaaztracker.domain.model

enum class RakahState {
    IDLE,
    QIAM,       // Standing — waiting for Rukooh
    RUKOOH,     // Bowing — waiting for return to Qiam
    ITIDAL,     // Brief standing after bow — waiting for first Sajdah
    SAJDAH_1,   // First prostration — waiting for sitting
    JULSA,      // Sitting between two Sajdahs — waiting for second Sajdah
    TASHAHHUD,  // Sitting for Attahiyat — waiting for Qiam of next Rakah
    COMPLETE,   // All Rakat finished
}
