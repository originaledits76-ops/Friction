package com.example.features.settings

import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions

class PoseAnalyzer(private val onRepCounted: (Int) -> Unit) : ImageAnalysis.Analyzer {
    private val options = PoseDetectorOptions.Builder()
        .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
        .build()
    private val poseDetector = PoseDetection.getClient(options)

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            poseDetector.process(image)
                .addOnSuccessListener { pose ->
                    processPose(pose)
                }
                .addOnFailureListener { e ->
                    Log.e("PoseAnalyzer", "Pose detection failed", e)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    private var pushupState = "UP"
    private var reps = 0

    private fun processPose(pose: Pose) {
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val leftElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW)
        val rightElbow = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW)
        val leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)
        val rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)

        if (leftShoulder != null && rightShoulder != null && leftElbow != null && rightElbow != null && leftWrist != null && rightWrist != null) {
            val shoulderY = (leftShoulder.position.y + rightShoulder.position.y) / 2
            val elbowY = (leftElbow.position.y + rightElbow.position.y) / 2

            if (shoulderY > elbowY - 20) {
                if (pushupState == "UP") {
                    pushupState = "DOWN"
                }
            } else if (shoulderY < elbowY - 60) {
                if (pushupState == "DOWN") {
                    pushupState = "UP"
                    reps++
                    onRepCounted(reps)
                }
            }
        }
    }
}
