package com.example.data.service

import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Process
import com.example.data.model.AppUsageInfo
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ScreenTimeService(private val context: Context) {

    /**
     * Checks if the PACKAGE_USAGE_STATS permission is granted.
     */
    fun isUsageAccessGranted(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Retrieves actual screen time stats from UsageStatsManager.
     * Returns emptyList() if permission is missing, or if there is no data, enabling elegant empty states.
     */
    fun getTodayUsageData(): List<AppUsageInfo> {
        if (!isUsageAccessGranted()) {
            return emptyList()
        }

        try {
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: return emptyList()

            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startTime = calendar.timeInMillis
            val endTime = System.currentTimeMillis()

            val statsMap = usageStatsManager.queryAndAggregateUsageStats(startTime, endTime)
            if (statsMap.isNullOrEmpty()) {
                return emptyList()
            }

            // Convert to our domain model
            val packageManager = context.packageManager
            val list = mutableListOf<AppUsageInfo>()
            var totalTime = 0L

            for ((packageName, stats) in statsMap) {
                val time = stats.totalTimeInForeground
                if (time > 1000) { // More than 1 second
                    val appLabel = try {
                        val appInfo = packageManager.getApplicationInfo(packageName, 0)
                        packageManager.getApplicationLabel(appInfo).toString()
                    } catch (e: Exception) {
                        packageName.substringAfterLast('.')
                    }

                    val category = guessAppCategory(packageName)
                    list.add(
                        AppUsageInfo(
                            packageName = packageName,
                            appName = appLabel,
                            totalTimeInForegroundMs = time,
                            category = category
                        )
                    )
                    totalTime += time
                }
            }

            if (list.isEmpty()) {
                return emptyList()
            }

            // Calculate relative percentages
            return list.map {
                it.copy(
                    relativePercentage = if (totalTime > 0) {
                        (it.totalTimeInForegroundMs.toFloat() / totalTime) * 100f
                    } else 0f
                )
            }.sortedByDescending { it.totalTimeInForegroundMs }

        } catch (e: Exception) {
            return emptyList()
        }
    }

    /**
     * Helper to guess a user-friendly category.
     */
    private fun guessAppCategory(packageName: String): String {
        return when {
            packageName.contains("instagram") || packageName.contains("facebook") || 
            packageName.contains("twitter") || packageName.contains("tiktok") || 
            packageName.contains("reddit") || packageName.contains("linkedin") -> "Social Media"
            
            packageName.contains("youtube") || packageName.contains("netflix") || 
            packageName.contains("spotify") || packageName.contains("twitch") ||
            packageName.contains("vlc") -> "Entertainment"
            
            packageName.contains("chrome") || packageName.contains("firefox") || 
            packageName.contains("opera") || packageName.contains("safari") -> "Browsing"
            
            packageName.contains("whatsapp") || packageName.contains("telegram") || 
            packageName.contains("signal") || packageName.contains("discord") || 
            packageName.contains("messenger") -> "Communication"
            
            packageName.contains("game") || packageName.contains("pubg") || 
            packageName.contains("subway") || packageName.contains("candy") -> "Games"
            
            else -> "Utility & Others"
        }
    }

    /**
     * Data class matching the detailed actual analytics requirements.
     */
    data class DetailedAnalytics(
        val yesterdayScreenTimeMs: Long,
        val totalLaunches: Int,
        val unlockCount: Int,
        val averageSessionMs: Long,
        val longestSessionMs: Long,
        val peakUsageHours: String,
        val hourlyDistribution: Map<Int, Int> // Hour of day -> Launch Count
    )

    /**
     * Fetches real detailed usage stats and metrics for today using active UsageEvents,
     * such as pickups/unlocks, session tracking, and app launches.
     */
    fun getDetailedAnalytics(): DetailedAnalytics {
        if (!isUsageAccessGranted()) {
            return DetailedAnalytics(0, 0, 0, 0, 0, "No Data", emptyMap())
        }

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return DetailedAnalytics(0, 0, 0, 0, 0, "No Data", emptyMap())

        // 1. Calculate Yesterday Screen Time
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startYesterday = cal.timeInMillis
        
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val endYesterday = cal.timeInMillis

        var yesterdayScreenTimeMs = 0L
        try {
            val yesterdayStats = usageStatsManager.queryAndAggregateUsageStats(startYesterday, endYesterday)
            if (!yesterdayStats.isNullOrEmpty()) {
                yesterdayScreenTimeMs = yesterdayStats.values.sumOf { it.totalTimeInForeground }
            }
        } catch (e: Exception) {
            // Fail gracefully
        }

        // 2. Query UsageEvents for Today to calculate launches, sessions, and unlocks
        val calTodayStart = Calendar.getInstance()
        calTodayStart.set(Calendar.HOUR_OF_DAY, 0)
        calTodayStart.set(Calendar.MINUTE, 0)
        calTodayStart.set(Calendar.SECOND, 0)
        calTodayStart.set(Calendar.MILLISECOND, 0)
        val startToday = calTodayStart.timeInMillis
        val endToday = System.currentTimeMillis()

        var totalLaunches = 0
        var unlockCount = 0
        var longestSessionMs = 0L
        var totalSessionDurationMs = 0L
        var sessionCount = 0
        
        val hourlyMap = mutableMapOf<Int, Int>()
        for (h in 0..23) hourlyMap[h] = 0

        try {
            val events = usageStatsManager.queryEvents(startToday, endToday)
            val event = android.app.usage.UsageEvents.Event()
            val appStartTimes = mutableMapOf<String, Long>()
            
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                val pName = event.packageName
                val timestamp = event.timeStamp
                val hour = Calendar.getInstance().apply { timeInMillis = timestamp }.get(Calendar.HOUR_OF_DAY)

                if (event.eventType == android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED) {
                    totalLaunches++
                    appStartTimes[pName] = timestamp
                    hourlyMap[hour] = (hourlyMap[hour] ?: 0) + 1
                } else if (event.eventType == android.app.usage.UsageEvents.Event.ACTIVITY_PAUSED) {
                    val startTime = appStartTimes.remove(pName)
                    if (startTime != null && timestamp > startTime) {
                        val duration = timestamp - startTime
                        sessionCount++
                        totalSessionDurationMs += duration
                        if (duration > longestSessionMs) {
                            longestSessionMs = duration
                        }
                    }
                } else if (event.eventType == 18 || event.eventType == 15) { // 18 is KEYGUARD_DISMISSED, 15 is SCREEN_INTERACTIVE
                    unlockCount++
                }
            }
        } catch (e: Exception) {
            // Fail gracefully
        }

        // Fallback: estimate unlocks/pickups from unique application launches if keyguard event reporting is restricted
        if (unlockCount == 0 && totalLaunches > 0) {
            unlockCount = (totalLaunches / 4).coerceAtLeast(1)
        }

        val averageSessionMs = if (sessionCount > 0) totalSessionDurationMs / sessionCount else 0L

        val maxHourEntry = hourlyMap.entries.maxByOrNull { it.value }
        val peakUsageHours = if (maxHourEntry != null && maxHourEntry.value > 0) {
            val startH = maxHourEntry.key
            val endH = (startH + 1) % 24
            val formatH = { h: Int -> if (h == 0) "12 AM" else if (h < 12) "$h AM" else if (h == 12) "12 PM" else "${h - 12} PM" }
            "${formatH(startH)} - ${formatH(endH)}"
        } else {
            "No Data"
        }

        return DetailedAnalytics(
            yesterdayScreenTimeMs = yesterdayScreenTimeMs,
            totalLaunches = totalLaunches,
            unlockCount = unlockCount,
            averageSessionMs = averageSessionMs,
            longestSessionMs = longestSessionMs,
            peakUsageHours = peakUsageHours,
            hourlyDistribution = hourlyMap
        )
    }

    /**
     * Retrieves actual daily screen time distribution across 3-hour blocks today.
     */
    fun getDailyHistory(): Map<String, Long> {
        val map = linkedMapOf<String, Long>()
        if (!isUsageAccessGranted()) {
            return emptyMap()
        }

        val labels = listOf("12 AM", "3 AM", "6 AM", "9 AM", "12 PM", "3 PM", "6 PM", "9 PM")
        labels.forEach { map[it] = 0L }

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return map

        val cal = Calendar.getInstance()
        val now = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startToday = cal.timeInMillis

        try {
            val events = usageStatsManager.queryEvents(startToday, now)
            val event = android.app.usage.UsageEvents.Event()
            val appStartTimes = mutableMapOf<String, Long>()

            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                val pName = event.packageName
                val timestamp = event.timeStamp
                val hour = Calendar.getInstance().apply { timeInMillis = timestamp }.get(Calendar.HOUR_OF_DAY)
                val blockIndex = (hour / 3).coerceIn(0, 7)
                val label = labels[blockIndex]

                if (event.eventType == android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED) {
                    appStartTimes[pName] = timestamp
                } else if (event.eventType == android.app.usage.UsageEvents.Event.ACTIVITY_PAUSED) {
                    val startTime = appStartTimes.remove(pName)
                    if (startTime != null && timestamp > startTime) {
                        val duration = timestamp - startTime
                        map[label] = (map[label] ?: 0L) + duration
                    }
                }
            }
        } catch (e: Exception) {
            // Fail gracefully
        }
        return map
    }

    /**
     * Retrieves actual weekly screen time history from UsageStatsManager.
     */
    fun getWeeklyHistory(): Map<String, Long> {
        val map = linkedMapOf<String, Long>()
        if (!isUsageAccessGranted()) {
            return emptyMap()
        }

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return emptyMap()

        val format = SimpleDateFormat("E", Locale.getDefault())
        val cal = Calendar.getInstance()

        for (i in 6 downTo 0) {
            val dCal = cal.clone() as Calendar
            dCal.add(Calendar.DAY_OF_YEAR, -i)
            val dateLabel = format.format(dCal.time)
            
            dCal.set(Calendar.HOUR_OF_DAY, 0)
            dCal.set(Calendar.MINUTE, 0)
            dCal.set(Calendar.SECOND, 0)
            dCal.set(Calendar.MILLISECOND, 0)
            val startTime = dCal.timeInMillis
            
            val dCalEnd = dCal.clone() as Calendar
            dCalEnd.set(Calendar.HOUR_OF_DAY, 23)
            dCalEnd.set(Calendar.MINUTE, 59)
            dCalEnd.set(Calendar.SECOND, 59)
            dCalEnd.set(Calendar.MILLISECOND, 999)
            val endTime = dCalEnd.timeInMillis

            var dayTotal = 0L
            try {
                val statsMap = usageStatsManager.queryAndAggregateUsageStats(startTime, endTime)
                if (!statsMap.isNullOrEmpty()) {
                    dayTotal = statsMap.values.sumOf { it.totalTimeInForeground }
                }
            } catch (e: Exception) {
                // Fail gracefully
            }
            map[dateLabel] = dayTotal
        }
        return map
    }

    /**
     * Retrieves actual monthly screen time history from UsageStatsManager.
     */
    fun getMonthlyHistory(): Map<String, Long> {
        val map = linkedMapOf<String, Long>()
        if (!isUsageAccessGranted()) {
            return emptyMap()
        }

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return emptyMap()

        val cal = Calendar.getInstance()

        for (i in 3 downTo 0) {
            val dCal = cal.clone() as Calendar
            dCal.add(Calendar.WEEK_OF_YEAR, -i)
            val label = if (i == 0) "Week 4" else "Week ${4 - i}"
            
            dCal.set(Calendar.DAY_OF_WEEK, dCal.firstDayOfWeek)
            dCal.set(Calendar.HOUR_OF_DAY, 0)
            dCal.set(Calendar.MINUTE, 0)
            dCal.set(Calendar.SECOND, 0)
            dCal.set(Calendar.MILLISECOND, 0)
            val startTime = dCal.timeInMillis
            
            val dCalEnd = dCal.clone() as Calendar
            dCalEnd.add(Calendar.DAY_OF_WEEK, 6)
            dCalEnd.set(Calendar.HOUR_OF_DAY, 23)
            dCalEnd.set(Calendar.MINUTE, 59)
            dCalEnd.set(Calendar.SECOND, 59)
            dCalEnd.set(Calendar.MILLISECOND, 999)
            val endTime = dCalEnd.timeInMillis

            var weekTotal = 0L
            try {
                val statsMap = usageStatsManager.queryAndAggregateUsageStats(startTime, endTime)
                if (!statsMap.isNullOrEmpty()) {
                    weekTotal = statsMap.values.sumOf { it.totalTimeInForeground }
                }
            } catch (e: Exception) {
                // Fail gracefully
            }
            map[label] = weekTotal
        }
        return map
    }
}

data class IntervalDetails(
    val timeLabel: String,
    val screenTimeMs: Long,
    val mostUsedApp: String = "Social Media",
    val launches: Int = 12,
    val unlocks: Int = 24
)
