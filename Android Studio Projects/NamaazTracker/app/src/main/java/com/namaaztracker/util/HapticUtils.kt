package com.namaaztracker.util

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator

enum class HapticType { RAKAH_COMPLETE, PRAYER_COMPLETE }

fun triggerHaptic(context: Context, type: HapticType) {
    val vibrator = context.getSystemService(Vibrator::class.java) ?: return
    val duration = when (type) {
        HapticType.RAKAH_COMPLETE -> 60L
        HapticType.PRAYER_COMPLETE -> 300L
    }
    vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
}
