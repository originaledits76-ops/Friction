package com.example.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object NotificationHelper {
    private const val TAG = "NotificationHelper"
    private const val CHANNEL_ID = "friction_daily_limit_channel"
    private const val CHANNEL_NAME = "Daily Limit Alerts"
    private const val NOTIF_ID = 2001

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Alerts when you are close to your daily screen time limit"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun checkAndSendDailyLimitNotification(
        context: Context,
        todayScreenTimeMs: Long,
        dailyLimitMs: Long
    ) {
        if (dailyLimitMs <= 0L) return
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return

        val remainingMs = dailyLimitMs - todayScreenTimeMs
        if (remainingMs <= 0L) return // Exceeded, do not send negative remaining time alerts

        val remainingMins = (remainingMs / 60000L).toInt()

        // Only trigger when remaining time is 60 minutes or less
        if (remainingMins > 60) return

        val prefs = context.getSharedPreferences("friction_limit_notif_prefs", Context.MODE_PRIVATE)
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val lastNotifiedDate = prefs.getString("last_notified_date", "")
        val lastNotifiedThreshold = prefs.getInt("last_notified_threshold", 999)

        // Thresholds: 60 mins, 30 mins, 15 mins
        val currentThreshold = when {
            remainingMins <= 15 -> 15
            remainingMins <= 30 -> 30
            else -> 60
        }

        // If already notified today at this or a tighter threshold, skip to avoid spam
        if (todayStr == lastNotifiedDate && currentThreshold >= lastNotifiedThreshold) {
            return
        }

        createNotificationChannel(context)

        val messageText = when {
            remainingMins in 55..60 -> "You have only 1 hour of screen time left today. Save it for something important."
            remainingMins in 30..54 -> "You have only $remainingMins minutes of screen time left today. Save it for something important."
            else -> "You have only $remainingMins minutes of screen time left today. Make them count."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Daily Screen Time Alert")
            .setContentText(messageText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(messageText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIF_ID, notification)
            prefs.edit()
                .putString("last_notified_date", todayStr)
                .putInt("last_notified_threshold", currentThreshold)
                .apply()
            Log.i(TAG, "Successfully sent daily limit notification ($remainingMins mins remaining)")
        } catch (e: SecurityException) {
            Log.e(TAG, "Notification permission missing: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send notification: ${e.message}")
        }
    }
}
