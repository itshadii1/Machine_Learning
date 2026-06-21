package com.namaaztracker.util

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

class CameraFrameAnalyzer(
    private val onFrameCaptured: (ByteArray) -> Unit,
) : ImageAnalysis.Analyzer {

    private var lastCaptureTimeMs = 0L
    private val captureIntervalMs = 250L // 4 fps

    override fun analyze(image: ImageProxy) {
        val now = System.currentTimeMillis()
        if (now - lastCaptureTimeMs >= captureIntervalMs) {
            lastCaptureTimeMs = now
            val bitmap = image.toBitmap()
            val jpegBytes = ImageUtils.bitmapToJpeg(bitmap)
            onFrameCaptured(jpegBytes)
        }
        image.close()
    }
}
