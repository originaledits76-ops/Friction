import re

with open('./app/src/main/java/com/example/data/service/ScreenTimeService.kt', 'r') as f:
    content = f.read()

replacement1 = """    private fun getAccurateScreenTimeForPeriod(startTime: Long, endTime: Long): Long {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return 0L
        var totalTime = 0L
        try {
            // Query a bit earlier to catch sessions that started before startTime
            val queryStart = Math.max(0L, startTime - 24 * 60 * 60 * 1000L)
            val events = usageStatsManager.queryEvents(queryStart, endTime)
            val event = android.app.usage.UsageEvents.Event()
            val appStartTimes = mutableMapOf<String, Long>()
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                val pName = event.packageName
                val timestamp = event.timeStamp
                if (event.eventType == android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED) {
                    appStartTimes[pName] = timestamp
                } else if (event.eventType == android.app.usage.UsageEvents.Event.ACTIVITY_PAUSED || event.eventType == android.app.usage.UsageEvents.Event.ACTIVITY_STOPPED) {
                    val start = appStartTimes.remove(pName)
                    if (start != null) {
                        val sessionStart = Math.max(start, startTime)
                        val sessionEnd = Math.min(timestamp, endTime)
                        if (sessionEnd > sessionStart) {
                            totalTime += (sessionEnd - sessionStart)
                        }
                    }
                }
            }
            // Add currently running apps
            for ((pName, start) in appStartTimes) {
                val sessionStart = Math.max(start, startTime)
                val sessionEnd = endTime
                if (sessionEnd > sessionStart) {
                    totalTime += (sessionEnd - sessionStart)
                }
            }
        } catch (e: Exception) {
            // fail gracefully
        }
        return totalTime
    }"""

content = re.sub(r'    private fun getAccurateScreenTimeForPeriod.*?return totalTime\n    \}', replacement1, content, flags=re.DOTALL)


replacement2 = """    fun getTodayUsageData(): List<AppUsageInfo> {
        if (!isUsageAccessGranted()) {
            return emptyList()
        }

        try {
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: return emptyList()

            val calendar = Calendar.getInstance()
            val now = System.currentTimeMillis()
            calendar.timeInMillis = now
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startTime = calendar.timeInMillis

            val queryStart = Math.max(0L, startTime - 24 * 60 * 60 * 1000L)
            val events = usageStatsManager.queryEvents(queryStart, now)
            val event = android.app.usage.UsageEvents.Event()
            val appUsageMap = mutableMapOf<String, Long>()
            val appStartTimes = mutableMapOf<String, Long>()

            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                val pName = event.packageName
                val timestamp = event.timeStamp
                
                if (event.eventType == android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED) {
                    appStartTimes[pName] = timestamp
                } else if (event.eventType == android.app.usage.UsageEvents.Event.ACTIVITY_PAUSED || event.eventType == android.app.usage.UsageEvents.Event.ACTIVITY_STOPPED) {
                    val start = appStartTimes.remove(pName)
                    if (start != null) {
                        val sessionStart = Math.max(start, startTime)
                        val sessionEnd = Math.min(timestamp, now)
                        if (sessionEnd > sessionStart) {
                            appUsageMap[pName] = (appUsageMap[pName] ?: 0L) + (sessionEnd - sessionStart)
                        }
                    }
                }
            }
            
            for ((pName, start) in appStartTimes) {
                val sessionStart = Math.max(start, startTime)
                val sessionEnd = now
                if (sessionEnd > sessionStart) {
                    appUsageMap[pName] = (appUsageMap[pName] ?: 0L) + (sessionEnd - sessionStart)
                }
            }

            // Convert to our domain model"""

content = re.sub(r'    fun getTodayUsageData.*?// Convert to our domain model', replacement2, content, flags=re.DOTALL)

with open('./app/src/main/java/com/example/data/service/ScreenTimeService.kt', 'w') as f:
    f.write(content)
