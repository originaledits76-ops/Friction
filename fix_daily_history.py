import re

with open('./app/src/main/java/com/example/data/service/ScreenTimeService.kt', 'r') as f:
    content = f.read()

replacement = """    fun getDailyHistory(): Map<String, Long> {
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
            val queryStart = Math.max(0L, startToday - 24 * 60 * 60 * 1000L)
            val events = usageStatsManager.queryEvents(queryStart, now)
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
                        val sessionStart = Math.max(start, startToday)
                        val sessionEnd = Math.min(timestamp, now)
                        if (sessionEnd > sessionStart) {
                            val hour = Calendar.getInstance().apply { timeInMillis = sessionStart }.get(Calendar.HOUR_OF_DAY)
                            val blockIndex = (hour / 3).coerceIn(0, 7)
                            val label = labels[blockIndex]
                            map[label] = (map[label] ?: 0L) + (sessionEnd - sessionStart)
                        }
                    }
                }
            }
            
            for ((pName, start) in appStartTimes) {
                val sessionStart = Math.max(start, startToday)
                val sessionEnd = now
                if (sessionEnd > sessionStart) {
                    val hour = Calendar.getInstance().apply { timeInMillis = sessionStart }.get(Calendar.HOUR_OF_DAY)
                    val blockIndex = (hour / 3).coerceIn(0, 7)
                    val label = labels[blockIndex]
                    map[label] = (map[label] ?: 0L) + (sessionEnd - sessionStart)
                }
            }
        } catch (e: Exception) {
            // Fail gracefully
        }
        return map
    }"""

content = re.sub(r'    fun getDailyHistory.*?return map\n    \}', replacement, content, flags=re.DOTALL)

with open('./app/src/main/java/com/example/data/service/ScreenTimeService.kt', 'w') as f:
    f.write(content)
