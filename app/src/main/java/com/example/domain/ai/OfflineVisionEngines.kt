package com.example.domain.ai

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.example.domain.catalog.OfflineFoodCatalog
import com.example.domain.model.FoodCatalogItem
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.abs
import kotlin.math.hypot

data class PoseAngleMetrics(
    val angleName: String,
    val shoulderToTorsoRatio: Float,
    val hipToTorsoRatio: Float,
    val legToTorsoRatio: Float,
    val postureAlignment: Float,
    val averageConfidence: Float
)

sealed class PoseAnalysisResult {
    data class Success(val metrics: PoseAngleMetrics) : PoseAnalysisResult()
    data class Incomplete(val reason: String) : PoseAnalysisResult()
}

data class FoodRecognitionResult(
    val matchedItem: FoodCatalogItem,
    val detectedLabels: List<String>,
    val topConfidencePercent: Int,
    val recognizedByAi: Boolean
)

object OfflineVisionEngines {

    private val poseDetector by lazy {
        val options = PoseDetectorOptions.Builder()
            .setDetectorMode(PoseDetectorOptions.SINGLE_IMAGE_MODE)
            .build()
        PoseDetection.getClient(options)
    }

    private val imageLabeler by lazy {
        val options = ImageLabelerOptions.Builder()
            .setConfidenceThreshold(0.45f)
            .build()
        ImageLabeling.getClient(options)
    }

    suspend fun analyzeBodyPose(bitmap: Bitmap, angleName: String): PoseAnalysisResult {
        val image = InputImage.fromBitmap(bitmap, 0)
        return analyzePoseInput(image, angleName)
    }

    suspend fun analyzeBodyPoseUri(context: Context, uri: Uri, angleName: String): PoseAnalysisResult {
        return try {
            val image = InputImage.fromFilePath(context, uri)
            analyzePoseInput(image, angleName)
        } catch (e: Exception) {
            PoseAnalysisResult.Incomplete("No se pudo leer la imagen: ${e.message ?: "error de archivo"}")
        }
    }

    private suspend fun analyzePoseInput(image: InputImage, angleName: String): PoseAnalysisResult =
        suspendCancellableCoroutine { cont ->
            poseDetector.process(image)
                .addOnSuccessListener { pose ->
                    if (!cont.isActive) return@addOnSuccessListener
                    cont.resume(extractPoseMetrics(pose, angleName))
                }
                .addOnFailureListener { e ->
                    if (!cont.isActive) return@addOnFailureListener
                    cont.resume(
                        PoseAnalysisResult.Incomplete(
                            "El detector local no pudo procesar la postura: ${e.localizedMessage ?: "error interno"}"
                        )
                    )
                }
        }

    private fun extractPoseMetrics(pose: Pose, angleName: String): PoseAnalysisResult {
        val ls = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val rs = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val lh = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val rh = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val la = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)
        val ra = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)
        val le = pose.getPoseLandmark(PoseLandmark.LEFT_EAR)
        val re = pose.getPoseLandmark(PoseLandmark.RIGHT_EAR)

        val coreLandmarks = listOfNotNull(ls, rs, lh, rh)
        if (coreLandmarks.size < 4) {
            return PoseAnalysisResult.Incomplete(
                "No se detectó el torso completo en la toma ($angleName). Ubícate de cuerpo entero con buena iluminación."
            )
        }

        val avgConf = coreLandmarks.map { it.inFrameLikelihood }.average().toFloat()
        if (avgConf < 0.45f) {
            return PoseAnalysisResult.Incomplete(
                "Confianza insuficiente (${(avgConf * 100).toInt()}%) en la toma ($angleName). Acércate o mejora la luz."
            )
        }

        val shoulderMidX = (ls!!.position.x + rs!!.position.x) / 2f
        val shoulderMidY = (ls.position.y + rs.position.y) / 2f
        val hipMidX = (lh!!.position.x + rh!!.position.x) / 2f
        val hipMidY = (lh.position.y + rh.position.y) / 2f

        val torsoLen = hypot(hipMidX - shoulderMidX, hipMidY - shoulderMidY).coerceAtLeast(1f)
        val shoulderSpan = hypot(rs.position.x - ls.position.x, rs.position.y - ls.position.y)
        val hipSpan = hypot(rh.position.x - lh.position.x, rh.position.y - lh.position.y)

        val legLen = if (la != null && ra != null) {
            val ankleMidX = (la.position.x + ra.position.x) / 2f
            val ankleMidY = (la.position.y + ra.position.y) / 2f
            hypot(ankleMidX - hipMidX, ankleMidY - hipMidY)
        } else {
            torsoLen * 1.45f
        }

        // Lateral alignment check using ear-shoulder-hip vertical offset
        val earMidX = when {
            le != null && re != null -> (le.position.x + re.position.x) / 2f
            le != null -> le.position.x
            re != null -> re.position.x
            else -> shoulderMidX
        }
        val lateralOffset = abs(earMidX - shoulderMidX) + abs(shoulderMidX - hipMidX)
        val postureScore = (1f - (lateralOffset / (torsoLen * 1.6f))).coerceIn(0.70f, 1.0f)

        return PoseAnalysisResult.Success(
            PoseAngleMetrics(
                angleName = angleName,
                shoulderToTorsoRatio = (shoulderSpan / torsoLen).coerceIn(0.35f, 1.15f),
                hipToTorsoRatio = (hipSpan / torsoLen).coerceIn(0.25f, 0.95f),
                legToTorsoRatio = (legLen / torsoLen).coerceIn(0.9f, 2.1f),
                postureAlignment = postureScore,
                averageConfidence = avgConf
            )
        )
    }

    suspend fun recognizeFoodFromBitmap(bitmap: Bitmap): FoodRecognitionResult {
        val image = InputImage.fromBitmap(bitmap, 0)
        return labelFoodImage(image)
    }

    suspend fun recognizeFoodFromUri(context: Context, uri: Uri): FoodRecognitionResult {
        return try {
            val image = InputImage.fromFilePath(context, uri)
            labelFoodImage(image)
        } catch (_: Exception) {
            FoodRecognitionResult(
                matchedItem = OfflineFoodCatalog.items.last(),
                detectedLabels = emptyList(),
                topConfidencePercent = 0,
                recognizedByAi = false
            )
        }
    }

    private suspend fun labelFoodImage(image: InputImage): FoodRecognitionResult =
        suspendCancellableCoroutine { cont ->
            imageLabeler.process(image)
                .addOnSuccessListener { labels ->
                    if (!cont.isActive) return@addOnSuccessListener
                    val sorted = labels.sortedByDescending { it.confidence }
                    val labelTexts = sorted.map { it.text }
                    val topConf = ((sorted.firstOrNull()?.confidence ?: 0f) * 100f).toInt()
                    val matched = OfflineFoodCatalog.matchLabels(labelTexts)
                    cont.resume(
                        FoodRecognitionResult(
                            matchedItem = matched,
                            detectedLabels = labelTexts.take(5),
                            topConfidencePercent = topConf,
                            recognizedByAi = labelTexts.isNotEmpty()
                        )
                    )
                }
                .addOnFailureListener {
                    if (!cont.isActive) return@addOnFailureListener
                    cont.resume(
                        FoodRecognitionResult(
                            matchedItem = OfflineFoodCatalog.items.last(),
                            detectedLabels = emptyList(),
                            topConfidencePercent = 0,
                            recognizedByAi = false
                        )
                    )
                }
        }
}
