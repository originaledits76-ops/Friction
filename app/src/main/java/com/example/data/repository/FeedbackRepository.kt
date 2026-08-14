package com.example.data.repository

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast

data class FeedbackSubmission(
    val id: String = "",
    val userId: String = "",
    val authType: String = "Guest",
    val category: String = "feedback", // "feedback", "bug_report", "feature_request"
    val subjectOrTitle: String = "",
    val description: String = "",
    val starRating: Int = 0,
    val extraDetails: String = "",
    val appVersion: String = "1.0.0",
    val platform: String = "Android",
    val timestamp: Long = System.currentTimeMillis()
)

class FeedbackRepository {
    private val tag = "FeedbackRepository"
    private val recipientEmail = "alokchoubey892@gmail.com"

    fun sendEmailFeedback(context: Context, submission: FeedbackSubmission): Boolean {
        // Asynchronously save to Firestore feedback collection if available
        try {
            val firestore = SafeFirebase.firestore
            firestore?.collection("feedback")?.add(
                mapOf(
                    "userId" to submission.userId,
                    "authType" to submission.authType,
                    "category" to submission.category,
                    "subjectOrTitle" to submission.subjectOrTitle,
                    "description" to submission.description,
                    "starRating" to submission.starRating,
                    "extraDetails" to submission.extraDetails,
                    "appVersion" to submission.appVersion,
                    "platform" to submission.platform,
                    "timestamp" to submission.timestamp
                )
            )
        } catch (e: Exception) {
            Log.w(tag, "Failed to save feedback to Firestore: ${e.message}")
        }

        val subject = when (submission.category) {
            "bug_report" -> "[Friction Bug Report] ${submission.subjectOrTitle}"
            "feature_request" -> "[Friction Feature Request] ${submission.subjectOrTitle}"
            else -> "[Friction Review] ${if (submission.starRating > 0) "${submission.starRating} Stars" else submission.subjectOrTitle}"
        }

        val bodyBuilder = StringBuilder()
        when (submission.category) {
            "bug_report" -> {
                bodyBuilder.appendLine("Bug Subject: ${submission.subjectOrTitle}")
                bodyBuilder.appendLine()
                bodyBuilder.appendLine("Description:")
                bodyBuilder.appendLine(submission.description)
            }
            "feature_request" -> {
                bodyBuilder.appendLine("Feature Title: ${submission.subjectOrTitle}")
                bodyBuilder.appendLine()
                bodyBuilder.appendLine("Description:")
                bodyBuilder.appendLine(submission.description)
                if (submission.extraDetails.isNotBlank()) {
                    bodyBuilder.appendLine()
                    bodyBuilder.appendLine("Why Helpful:")
                    bodyBuilder.appendLine(submission.extraDetails)
                }
            }
            else -> {
                if (submission.starRating > 0) {
                    bodyBuilder.appendLine("Rating: ${submission.starRating} / 5 Stars")
                    bodyBuilder.appendLine()
                }
                bodyBuilder.appendLine("Thoughts:")
                bodyBuilder.appendLine(submission.description)
                if (submission.extraDetails.isNotBlank()) {
                    bodyBuilder.appendLine()
                    bodyBuilder.appendLine("Note / Suggestion:")
                    bodyBuilder.appendLine(submission.extraDetails)
                }
            }
        }

        bodyBuilder.appendLine()
        bodyBuilder.appendLine("---")
        bodyBuilder.appendLine("User ID: ${submission.userId} (${submission.authType})")
        bodyBuilder.appendLine("App Version: ${submission.appVersion}")
        bodyBuilder.appendLine("Platform: ${submission.platform}")

        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$recipientEmail")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(recipientEmail))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, bodyBuilder.toString())
        }

        return try {
            val chooserIntent = Intent.createChooser(emailIntent, "Send email using...")
            chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooserIntent)
            Log.i(tag, "Opened email client via ACTION_SENDTO for recipient $recipientEmail")
            true
        } catch (e: ActivityNotFoundException) {
            Log.e(tag, "No email client found on device for ACTION_SENDTO", e)
            Toast.makeText(context, "No email application found on your device", Toast.LENGTH_LONG).show()
            false
        } catch (e: Exception) {
            Log.e(tag, "Error starting email intent", e)
            Toast.makeText(context, "Could not open email app: ${e.message}", Toast.LENGTH_SHORT).show()
            false
        }
    }
}

