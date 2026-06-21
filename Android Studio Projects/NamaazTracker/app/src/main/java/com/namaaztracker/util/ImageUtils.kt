package com.namaaztracker.util

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream

object ImageUtils {
    fun bitmapToJpeg(bitmap: Bitmap, quality: Int = 75): ByteArray =
        ByteArrayOutputStream().also { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        }.toByteArray()
}
