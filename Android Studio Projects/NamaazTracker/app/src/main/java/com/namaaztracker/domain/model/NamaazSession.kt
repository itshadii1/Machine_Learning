package com.namaaztracker.domain.model

data class NamaazSession(
    val namaazType: NamaazType,
    val currentState: RakahState = RakahState.QIAM,
    val completedRakat: Int = 0,
) {
    val currentRakah: Int
        get() = (completedRakat + 1).coerceAtMost(namaazType.rakahCount)

    val isComplete: Boolean
        get() = currentState == RakahState.COMPLETE
}
