package com.namaaztracker.data.local

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.graphics.BitmapFactory
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerOptions
import com.namaaztracker.domain.model.Posture
import dagger.hilt.android.qualifiers.ApplicationContext
import java.nio.FloatBuffer
import javax.inject.Inject
import javax.inject.Singleton

private const val MEDIAPIPE_MODEL = "pose_landmarker_lite.task"
private const val ONNX_MODEL      = "body_language.onnx"
private const val CONFIDENCE_THRESHOLD = 0.55f

@Singleton
class OnDevicePostureDetector @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    // Both are created lazily on the first inference call (background thread).
    private val landmarker: PoseLandmarker by lazy { createLandmarker() }
    private val ortEnv: OrtEnvironment     by lazy { OrtEnvironment.getEnvironment() }
    private val ortSession: OrtSession     by lazy { createOrtSession() }

    private fun createLandmarker(): PoseLandmarker {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath(MEDIAPIPE_MODEL)
            .build()
        val options = PoseLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.IMAGE)
            .setNumPoses(1)
            .setMinPoseDetectionConfidence(0.5f)
            .setMinPosePresenceConfidence(0.5f)
            .setMinTrackingConfidence(0.5f)
            .build()
        return PoseLandmarker.createFromOptions(context, options)
    }

    private fun createOrtSession(): OrtSession {
        val modelBytes = context.assets.open(ONNX_MODEL).use { it.readBytes() }
        return ortEnv.createSession(modelBytes)
    }

    /**
     * Runs MediaPipe pose estimation then the ONNX RandomForest classifier entirely
     * on-device. Returns [Posture.UNKNOWN] when no pose is detected or confidence is low.
     */
    fun detect(jpegBytes: ByteArray): Posture {
        val bitmap = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
            ?: return Posture.UNKNOWN

        return try {
            // ── 1. Pose landmark extraction ────────────────────────────────
            val mpImage = BitmapImageBuilder(bitmap).build()
            val result  = landmarker.detect(mpImage)
            if (result.poseLandmarks().isEmpty()) return Posture.UNKNOWN

            // ── 2. Build 132-feature vector (x,y,z,visibility × 33 landmarks)
            val landmarks = result.poseLandmarks()[0]
            val features  = FloatArray(132)
            for (i in landmarks.indices) {
                val lm = landmarks[i]
                features[i * 4 + 0] = lm.x()
                features[i * 4 + 1] = lm.y()
                features[i * 4 + 2] = lm.z()
                features[i * 4 + 3] = lm.visibility().orElse(0f)
            }

            // ── 3. ONNX RandomForest inference ─────────────────────────────
            val inputTensor = OnnxTensor.createTensor(
                ortEnv,
                FloatBuffer.wrap(features),
                longArrayOf(1, 132),
            )
            val outputs = ortSession.run(mapOf("float_input" to inputTensor))

            // Output 0 = label (string), Output 1 = probabilities float[1][4]
            val label   = (outputs[0].value as Array<*>)[0] as String
            val probs   = (outputs[1].value as Array<FloatArray>)[0]
            val maxProb = probs.max()

            inputTensor.close()

            if (maxProb < CONFIDENCE_THRESHOLD) Posture.UNKNOWN
            else Posture.fromLabel(label)

        } catch (_: Exception) {
            Posture.UNKNOWN
        } finally {
            bitmap.recycle()
        }
    }
}
