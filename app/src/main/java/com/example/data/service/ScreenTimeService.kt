package com.example.data.service

import android.app.AppOpsManager
import android.app.usage.UsageEvents
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
     * Core accurate calculation engine for screen time between startTime and endTime.
     * Enforces local calendar day boundaries, splits sessions crossing boundaries,
     * and handles screen-off / keyguard events to prevent double counting or phantom usage.
     */
    private fun getAccurateScreenTimeForPeriod(startTime: Long, endTime: Long): Long {
        if (!isUsageAccessGranted()) return 0L
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return 0L

        var totalTime = 0L
        try {
            // Buffer query 12h prior to catch sessions starting before startTime
            val queryStart = Math.max(0L, startTime - 12 * 60 * 60 * 1000L)
            val events = usageStatsManager.queryEvents(queryStart, endTime)
            val event = UsageEvents.Event()
            val appStartTimes = mutableMapOf<String, Long>()

            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                val pName = event.packageName
                val timestamp = event.timeStamp

                when (event.eventType) {
                    UsageEvents.Event.ACTIVITY_RESUMED -> {
                        appStartTimes[pName] = timestamp
                    }
                    UsageEvents.Event.ACTIVITY_PAUSED, UsageEvents.Event.ACTIVITY_STOPPED -> {
                        val start = appStartTimes.remove(pName)
                        if (start != null) {
                            val sessionStart = Math.max(start, startTime)
                            val sessionEnd = Math.min(timestamp, endTime)
                            if (sessionEnd > sessionStart) {
                                totalTime += (sessionEnd - sessionStart)
                            }
                        }
                    }
                    16, 17, 26 -> { // SCREEN_NON_INTERACTIVE (16), KEYGUARD_SHOWN (17), DEVICE_SHUTDOWN (26)
                        for ((_, start) in appStartTimes) {
                            val sessionStart = Math.max(start, startTime)
                            val sessionEnd = Math.min(timestamp, endTime)
                            if (sessionEnd > sessionStart) {
                                totalTime += (sessionEnd - sessionStart)
                            }
                        }
                        appStartTimes.clear()
                    }
                }
            }

            // Cap ongoing sessions at endTime
            for ((_, start) in appStartTimes) {
                val sessionStart = Math.max(start, startTime)
                val sessionEnd = Math.min(endTime, sessionStart + 3600_000L) // cap continuous unclosed ongoing at 1h
                if (sessionEnd > sessionStart) {
                    totalTime += (sessionEnd - sessionStart)
                }
            }
        } catch (e: Exception) {
            // fail gracefully
        }

        // Hard cap total screen time to actual elapsed time in period
        val elapsedPeriod = Math.max(0L, endTime - startTime)
        return Math.min(totalTime, elapsedPeriod)
    }

    /**
     * Retrieves actual screen time stats for any given period [startTime, endTime].
     */
    fun getUsageDataForPeriod(startTime: Long, endTime: Long): List<AppUsageInfo> {
        if (!isUsageAccessGranted()) {
            return emptyList()
        }

        try {
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: return emptyList()

            val queryStart = Math.max(0L, startTime - 12 * 60 * 60 * 1000L)
            val events = usageStatsManager.queryEvents(queryStart, endTime)
            val event = UsageEvents.Event()
            val appUsageMap = mutableMapOf<String, Long>()
            val appStartTimes = mutableMapOf<String, Long>()

            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                val pName = event.packageName
                val timestamp = event.timeStamp

                when (event.eventType) {
                    UsageEvents.Event.ACTIVITY_RESUMED -> {
                        appStartTimes[pName] = timestamp
                    }
                    UsageEvents.Event.ACTIVITY_PAUSED, UsageEvents.Event.ACTIVITY_STOPPED -> {
                        val start = appStartTimes.remove(pName)
                        if (start != null) {
                            val sessionStart = Math.max(start, startTime)
                            val sessionEnd = Math.min(timestamp, endTime)
                            if (sessionEnd > sessionStart) {
                                appUsageMap[pName] = (appUsageMap[pName] ?: 0L) + (sessionEnd - sessionStart)
                            }
                        }
                    }
                    16, 17, 26 -> { // SCREEN_NON_INTERACTIVE, KEYGUARD_SHOWN, DEVICE_SHUTDOWN
                        for ((pkg, start) in appStartTimes) {
                            val sessionStart = Math.max(start, startTime)
                            val sessionEnd = Math.min(timestamp, endTime)
                            if (sessionEnd > sessionStart) {
                                appUsageMap[pkg] = (appUsageMap[pkg] ?: 0L) + (sessionEnd - sessionStart)
                            }
                        }
                        appStartTimes.clear()
                    }
                }
            }

            for ((pkg, start) in appStartTimes) {
                val sessionStart = Math.max(start, startTime)
                val sessionEnd = Math.min(endTime, sessionStart + 3600_000L)
                if (sessionEnd > sessionStart) {
                    appUsageMap[pkg] = (appUsageMap[pkg] ?: 0L) + (sessionEnd - sessionStart)
                }
            }

            // Convert to domain model
            val packageManager = context.packageManager
            val list = mutableListOf<AppUsageInfo>()
            var totalTime = 0L

            for ((packageName, time) in appUsageMap) {
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
     * Retrieves actual screen time stats for TODAY (from 00:00:00 local time to now).
     */
    fun getTodayUsageData(): List<AppUsageInfo> {
        val calendar = Calendar.getInstance()
        val now = System.currentTimeMillis()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return getUsageDataForPeriod(calendar.timeInMillis, now)
    }

    /**
     * Calculates actual hourly screen time duration (in ms) for TODAY from 00:00 to now.
     */
    fun getTodayHourlyScreenTimeMs(): Map<Int, Long> {
        val result = mutableMapOf<Int, Long>()
        for (h in 0..23) result[h] = 0L
        if (!isUsageAccessGranted()) return result

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return result

        val calendar = Calendar.getInstance()
        val now = System.currentTimeMillis()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis

        try {
            val queryStart = Math.max(0L, startOfDay - 12 * 60 * 60 * 1000L)
            val events = usageStatsManager.queryEvents(queryStart, now)
            val event = UsageEvents.Event()
            val appStartTimes = mutableMapOf<String, Long>()

            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                val pName = event.packageName
                val timestamp = event.timeStamp

                when (event.eventType) {
                    UsageEvents.Event.ACTIVITY_RESUMED -> {
                        appStartTimes[pName] = timestamp
                    }
                    UsageEvents.Event.ACTIVITY_PAUSED, UsageEvents.Event.ACTIVITY_STOPPED -> {
                        val start = appStartTimes.remove(pName)
                        if (start != null) {
                            val sessionStart = Math.max(start, startOfDay)
                            val sessionEnd = Math.min(timestamp, now)
                            if (sessionEnd > sessionStart) {
                                addDurationToHourlyBuckets(result, sessionStart, sessionEnd)
                            }
                        }
                    }
                    16, 17, 26 -> { // SCREEN_NON_INTERACTIVE, KEYGUARD_SHOWN, DEVICE_SHUTDOWN
                        for ((_, start) in appStartTimes) {
                            val sessionStart = Math.max(start, startOfDay)
                            val sessionEnd = Math.min(timestamp, now)
                            if (sessionEnd > sessionStart) {
                                addDurationToHourlyBuckets(result, sessionStart, sessionEnd)
                            }
                        }
                        appStartTimes.clear()
                    }
                }
            }
        } catch (e: Exception) {
            // Fail gracefully
        }
        return result
    }

    private fun addDurationToHourlyBuckets(map: MutableMap<Int, Long>, startMs: Long, endMs: Long) {
        val cal = Calendar.getInstance()
        var current = startMs
        while (current < endMs) {
            cal.timeInMillis = current
            val hour = cal.get(Calendar.HOUR_OF_DAY)

            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            val endOfHour = cal.timeInMillis + 1

            val chunkEnd = Math.min(endMs, endOfHour)
            val duration = chunkEnd - current
            map[hour] = (map[hour] ?: 0L) + duration
            current = chunkEnd
        }
    }

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
     * Fetches real detailed usage stats and metrics for any given period [startTime, endTime].
     */
    fun getDetailedAnalyticsForPeriod(startTime: Long, endTime: Long): DetailedAnalytics {
        if (!isUsageAccessGranted()) {
            return DetailedAnalytics(0, 0, 0, 0, 0, "No Data", emptyMap())
        }

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return DetailedAnalytics(0, 0, 0, 0, 0, "No Data", emptyMap())

        // Calculate comparison period (previous equivalent window)
        val periodDuration = Math.max(1L, endTime - startTime)
        val startPrevious = Math.max(0L, startTime - periodDuration)
        val endPrevious = startTime - 1L
        val yesterdayScreenTimeMs = getAccurateScreenTimeForPeriod(startPrevious, endPrevious)

        var totalLaunches = 0
        var unlockCount = 0
        var longestSessionMs = 0L
        var totalSessionDurationMs = 0L
        var sessionCount = 0

        val hourlyMap = mutableMapOf<Int, Int>()
        for (h in 0..23) hourlyMap[h] = 0

        var lastUnlockTime = 0L
        val lastLaunchTimeMap = mutableMapOf<String, Long>()

        try {
            val events = usageStatsManager.queryEvents(startTime, endTime)
            val event = UsageEvents.Event()
            val appStartTimes = mutableMapOf<String, Long>()

            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                val pName = event.packageName
                val timestamp = event.timeStamp
                val hour = Calendar.getInstance().apply { timeInMillis = timestamp }.get(Calendar.HOUR_OF_DAY)

                when (event.eventType) {
                    UsageEvents.Event.ACTIVITY_RESUMED -> {
                        val lastLaunch = lastLaunchTimeMap[pName] ?: 0L
                        if (timestamp - lastLaunch > 3000L) { // Deduplicate rapid resumes within 3s
                            totalLaunches++
                            lastLaunchTimeMap[pName] = timestamp
                            hourlyMap[hour] = (hourlyMap[hour] ?: 0) + 1
                        }
                        appStartTimes[pName] = timestamp
                    }
                    UsageEvents.Event.ACTIVITY_PAUSED, UsageEvents.Event.ACTIVITY_STOPPED -> {
                        val start = appStartTimes.remove(pName)
                        if (start != null && timestamp > start) {
                            val duration = timestamp - start
                            sessionCount++
                            totalSessionDurationMs += duration
                            if (duration > longestSessionMs) {
                                longestSessionMs = duration
                            }
                        }
                    }
                    18, 15 -> { // KEYGUARD_DISMISSED, SCREEN_INTERACTIVE
                        if (timestamp - lastUnlockTime > 2000L) { // Deduplicate unlocks within 2s
                            unlockCount++
                            lastUnlockTime = timestamp
                        }
                    }
                }
            }

            // Flush remaining active sessions
            for ((pkg, start) in appStartTimes) {
                if (endTime > start) {
                    val duration = Math.min(endTime - start, 3600_000L)
                    if (duration > 0) {
                        sessionCount++
                        totalSessionDurationMs += duration
                        if (duration > longestSessionMs) {
                            longestSessionMs = duration
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Fail gracefully
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
     * Fetches real detailed usage stats and metrics for today.
     */
    fun getDetailedAnalytics(): DetailedAnalytics {
        val calTodayStart = Calendar.getInstance()
        calTodayStart.set(Calendar.HOUR_OF_DAY, 0)
        calTodayStart.set(Calendar.MINUTE, 0)
        calTodayStart.set(Calendar.SECOND, 0)
        calTodayStart.set(Calendar.MILLISECOND, 0)
        return getDetailedAnalyticsForPeriod(calTodayStart.timeInMillis, System.currentTimeMillis())
    }

    /**
     * Retrieves actual daily screen time distribution across 3-hour blocks today.
     * Uses the exact same single-source calculation for consistency.
     */
    fun getDailyHistory(): Map<String, Long> {
        val map = linkedMapOf<String, Long>()
        if (!isUsageAccessGranted()) {
            return emptyMap()
        }

        val labels = listOf("12 AM", "3 AM", "6 AM", "9 AM", "12 PM", "3 PM", "6 PM", "9 PM")
        labels.forEach { map[it] = 0L }

        val cal = Calendar.getInstance()
        val now = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startToday = cal.timeInMillis

        // Distribute total usage across 3-hour intervals
        for (i in 0..7) {
            val blockStart = startToday + i * 3 * 3600_000L
            val blockEnd = Math.min(now, startToday + (i + 1) * 3 * 3600_000L)
            if (blockEnd > blockStart) {
                map[labels[i]] = getAccurateScreenTimeForPeriod(blockStart, blockEnd)
            } else {
                map[labels[i]] = 0L
            }
        }

        return map
    }

    /**
     * Retrieves actual weekly screen time history for the CURRENT calendar week.
     */
    fun getWeeklyHistory(): Map<String, Long> {
        val map = linkedMapOf<String, Long>()
        if (!isUsageAccessGranted()) {
            return emptyMap()
        }

        val format = SimpleDateFormat("E", Locale.getDefault())
        val cal = Calendar.getInstance()

        // Go to start of current calendar week
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        val now = System.currentTimeMillis()

        for (i in 0 until 7) {
            val dCal = cal.clone() as Calendar
            dCal.add(Calendar.DAY_OF_YEAR, i)
            val dateLabel = format.format(dCal.time)

            val startTime = dCal.timeInMillis
            val dCalEnd = dCal.clone() as Calendar
            dCalEnd.set(Calendar.HOUR_OF_DAY, 23)
            dCalEnd.set(Calendar.MINUTE, 59)
            dCalEnd.set(Calendar.SECOND, 59)
            dCalEnd.set(Calendar.MILLISECOND, 999)
            val endTime = Math.min(now, dCalEnd.timeInMillis)

            if (endTime > startTime && startTime <= now) {
                val dayTotal = getAccurateScreenTimeForPeriod(startTime, endTime)
                map[dateLabel] = dayTotal
            } else {
                map[dateLabel] = 0L
            }
        }
        return map
    }

    /**
     * Retrieves actual monthly screen time history for the CURRENT calendar month.
     */
    fun getMonthlyHistory(): Map<String, Long> {
        val map = linkedMapOf<String, Long>()
        if (!isUsageAccessGranted()) {
            return emptyMap()
        }

        val cal = Calendar.getInstance()
        val now = System.currentTimeMillis()

        // Set to 1st day of current calendar month
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        val maxDays = Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH)

        val week1End = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 7) }
        val week2End = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 14) }
        val week3End = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 21) }

        val weeks = listOf(
            "W1 (1-7)" to Pair(cal.timeInMillis, Math.min(now, week1End.timeInMillis - 1)),
            "W2 (8-14)" to Pair(week1End.timeInMillis, Math.min(now, week2End.timeInMillis - 1)),
            "W3 (15-21)" to Pair(week2End.timeInMillis, Math.min(now, week3End.timeInMillis - 1)),
            "W4 (22+)" to Pair(week3End.timeInMillis, now)
        )

        for ((label, range) in weeks) {
            val (start, end) = range
            if (end > start && start <= now) {
                map[label] = getAccurateScreenTimeForPeriod(start, end)
            } else {
                map[label] = 0L
            }
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
