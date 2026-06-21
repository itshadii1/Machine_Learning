package com.namaaztracker.presentation.screens.tracker

import com.namaaztracker.domain.model.NamaazSession
import com.namaaztracker.domain.model.NamaazType
import com.namaaztracker.domain.model.Posture
import com.namaaztracker.domain.model.RakahState

data class TrackerUiState(
    val session: NamaazSession,
    val currentPosture: Posture = Posture.UNKNOWN,
    val apiConnected: Boolean = false,
)

val TrackerUiState.displayRakah: Int get() = session.currentRakah
val TrackerUiState.totalRakat: Int get() = session.namaazType.rakahCount
val TrackerUiState.isComplete: Boolean get() = session.currentState == RakahState.COMPLETE
val TrackerUiState.stateName: String
    get() = when (session.currentState) {
        RakahState.QIAM -> "Standing (Qiam)"
        RakahState.RUKOOH -> "Bowing (Rukooh)"
        RakahState.ITIDAL -> "Rising (I'tidal)"
        RakahState.SAJDAH_1 -> "First Prostration"
        RakahState.JULSA -> "Sitting (Julsa)"
        RakahState.TASHAHHUD -> "Attahiyat"
        RakahState.COMPLETE -> "Complete"
        RakahState.IDLE -> ""
    }

val TrackerUiState.stateInstruction: String
    get() = when (session.currentState) {
        RakahState.QIAM -> "Bow down for Rukooh"
        RakahState.RUKOOH -> "Rise back to standing"
        RakahState.ITIDAL -> "Prostrate for Sajdah"
        RakahState.SAJDAH_1 -> "Sit up between Sajdahs"
        RakahState.JULSA -> "Prostrate again for second Sajdah"
        RakahState.TASHAHHUD -> "Stand for next Rakah when ready"
        RakahState.COMPLETE -> "Alhamdulillah — prayer complete"
        RakahState.IDLE -> ""
    }

val TrackerUiState.stateEmoji: String
    get() = when (session.currentState) {
        RakahState.QIAM -> "🕴"
        RakahState.RUKOOH -> "🙇"
        RakahState.ITIDAL -> "🕴"
        RakahState.SAJDAH_1 -> "🤲"
        RakahState.JULSA -> "🧎"
        RakahState.TASHAHHUD -> "🧎"
        RakahState.COMPLETE -> "✅"
        RakahState.IDLE -> ""
    }
