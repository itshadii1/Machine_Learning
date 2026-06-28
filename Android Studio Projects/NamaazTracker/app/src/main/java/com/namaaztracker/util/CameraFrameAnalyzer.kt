package com.namaaztracker.util

import android.graphics.Bitmap
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

class CameraFrameAnalyzer(
    private val onFrameCaptured: (ByteArray) -> Unit,
) : ImageAnalysis.Analyzer {

    private var lastCaptureTimeMs = 0L
    private val captureIntervalMs = 250L // 4 fps

    // MediaPipe only needs ~480px to detect landmarks accurately.
    // Sending full phone-camera resolution (often 1080p+) wastes bandwidth and slows the API call.
    private val targetWidth = 480

    override fun analyze(image: ImageProxy) {
        val now = System.currentTimeMillis()
        if (now - lastCaptureTimeMs >= captureIntervalMs) {
            lastCaptureTimeMs = now

            val bitmap = image.toBitmap()
            val scale = targetWidth.toFloat() / bitmap.width
            val scaled = Bitmap.createScaledBitmap(
                bitmap,
                targetWidth,
                (bitmap.height * scale).toInt(),
                false,
            )
            val jpegBytes = ImageUtils.bitmapToJpeg(scaled)
            bitmap.recycle()
            scaled.recycle()

            onFrameCaptured(jpegBytes)
        }
        image.close()
    }
}
