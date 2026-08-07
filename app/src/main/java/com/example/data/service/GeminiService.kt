package com.example.data.service

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.AppUsageInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiService {
    private val tag = "AiService"
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    data class AnalysisContext(
        val todayScreenTimeMs: Long,
        val topApps: List<AppUsageInfo>,
        val unlocks: Int,
        val avgSessionMs: Long,
        val longestSessionMs: Long,
        val peakHours: String,
        val streakDays: Int = 0,
        val xp: Int = 0,
        val userGoal: String = "",
        val userMotivation: String = "",
        val activeLimitsCount: Int = 0
    )

    suspend fun getAiAnalysis(context: AnalysisContext): String = withContext(Dispatchers.IO) {
        val groqApiKey = try {
            BuildConfig.GROQ_API_KEY
        } catch (e: Exception) {
            ""
        }

        // Validate real usage data presence
        if (context.todayScreenTimeMs == 0L && context.topApps.isEmpty()) {
            return@withContext """
                ### Your Personal Insights
                More usage data needed before we can craft meaningful coaching insights.
                
                Start using your device with Friction active today, and we'll analyze your focus cycles and help you build intentional, beautiful habits!
                
                [ACTION:VIEW_ANALYTICS|Refresh Analytics]
            """.trimIndent()
        }

        // Try Groq API call if key is present
        if (groqApiKey.isNotBlank() && groqApiKey != "MY_GROQ_API_KEY" && !groqApiKey.startsWith("placeholder")) {
            try {
                val appDetailsJson = JSONArray()
                for (app in context.topApps) {
                    val appObj = JSONObject().apply {
                        put("appName", app.appName)
                        put("category", app.category)
                        put("timeUsedMinutes", app.totalTimeInForegroundMs / 60000L)
                    }
                    appDetailsJson.put(appObj)
                }

                val systemPrompt = """
                    You are Friction's AI Personal Coach. Speak directly to the user in the first/second person (use "you", "your", "we", "I" - never say "the user" or "he/she" or speak in the third person).
                    
                    Tone Rules:
                    - Highly supportive, motivating, professional, friendly, and constructive.
                    - Never judgmental, preachy, or clinical.
                    - Celebrate progress, encourage consistency, and offer actionable next steps.
                    
                    Linguistic Guidelines:
                    - Instead of saying "the user spends...", say "You spend..."
                    - Instead of saying "the user should...", say "You could..." or "We can..."
                    
                    Real-Time Context:
                    - Analyze the provided screen time and app usage metrics.
                    - Highlight positive trends (e.g. low screen time, streak count, many active limiters).
                    - If distracting apps are highly used, help the user set clear intentions to master their focus.
                    
                    CRITICAL REQUIREMENTS:
                    1. NEVER mention model names, backend tech, or API providers.
                    2. NEVER invent fake numbers, fake usage, or fabricated stats. Rely strictly on the numbers given.
                    3. Structure your response clearly using Markdown headings (###), bold text, bullet points, and an Actionable Next Steps section.
                    4. At the end of actionable recommendations, include interactive action triggers using this exact format:
                       [ACTION:SET_LIMIT|Set a Limit]
                       [ACTION:OPEN_ENGINE|Open Friction Engine]
                       [ACTION:CLASSIFY_APPS|Review App Classification]
                       [ACTION:REVIEW_GOAL|Review Goal]
                """.trimIndent()

                val userPrompt = """
                    User's Real Data:
                    - Total Screen Time Today: ${context.todayScreenTimeMs / 60000L} minutes
                    - Unlocks / Pickups: ${context.unlocks}
                    - Average Session: ${context.avgSessionMs / 1000L} seconds
                    - Longest Session: ${context.longestSessionMs / 60000L} minutes
                    - Peak Usage Period: ${context.peakHours}
                    - Top Apps Used: $appDetailsJson
                    - Current Streak: ${context.streakDays} days
                    - Total XP: ${context.xp}
                    - Active Limits Configured: ${context.activeLimitsCount}
                    - Goal: ${context.userGoal.ifBlank { "Build healthier digital habits" }}
                    - Motivation: ${context.userMotivation.ifBlank { "Live more mindfully" }}
                """.trimIndent()

                val messagesArray = JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", systemPrompt)
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", userPrompt)
                    })
                }

                val requestJson = JSONObject().apply {
                    put("model", "llama-3.3-70b-versatile")
                    put("messages", messagesArray)
                    put("temperature", 0.3)
                }

                val url = "https://api.groq.com/openai/v1/chat/completions"
                val mediaType = "application/json; charset=utf-8".toMediaType()
                val request = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer $groqApiKey")
                    .addHeader("Content-Type", "application/json")
                    .post(requestJson.toString().toRequestBody(mediaType))
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyString = response.body?.string()
                        if (bodyString != null) {
                            val root = JSONObject(bodyString)
                            val choices = root.optJSONArray("choices")
                            if (choices != null && choices.length() > 0) {
                                val messageObj = choices.getJSONObject(0).optJSONObject("message")
                                val text = messageObj?.optString("content", "") ?: ""
                                if (text.isNotBlank()) {
                                    return@withContext text.trim()
                                }
                            }
                        }
                    } else {
                        Log.w(tag, "Groq API returned error code ${response.code}")
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Exception querying Groq API: ${e.message}", e)
            }
        }

        // Dynamic, high-quality offline personal coach fallback
        return@withContext getOfflineCoachingResponse(context)
    }

    private fun getOfflineCoachingResponse(context: AnalysisContext): String {
        val todayMins = context.todayScreenTimeMs / 60000L
        val streak = context.streakDays
        val goal = context.userGoal.ifBlank { "Build healthier digital habits" }
        val motivation = context.userMotivation.ifBlank { "Live more mindfully" }
        
        val sb = StringBuilder()
        sb.append("### Your Mindful Coaching Circle\n")
        sb.append("Hello! I'm your Friction personal coach, and I'm honored to support you on your path to deep focus and intentional living. Let's align our efforts on your current focus goal:\n**$goal**.\n\n")
        
        if (streak > 0) {
            sb.append("🎉 **Celebrating Your Consistency:** You have maintained a **$streak-day streak**! That is absolutely outstanding. Every day you build intentional spaces between impulse and action, you are taking back ownership of your attention span.\n\n")
        } else {
            sb.append("🌱 **A Fresh Start Today:** Beginning today is a powerful choice. Let's start small, take one session at a time, and build consistent habits together.\n\n")
        }
        
        sb.append("### Attention Pattern Analysis\n")
        sb.append("Let's review what your focus footprint looks like today:\n")
        sb.append("- **Total Screen Presence:** You spent **$todayMins minutes** on your device. Every minute spent mindfully is a wonderful step forward.\n")
        sb.append("- **Friction Breaks:** You picked up your phone **${context.unlocks} times**. Developing awareness around these physical pickups is half the battle.\n")
        if (context.avgSessionMs > 0) {
            sb.append("- **Average Session Duration:** Your typical session lasts **${context.avgSessionMs / 1000L} seconds**. Keeping sessions short protects your mental energy.\n")
        }
        if (context.longestSessionMs > 0) {
            val longestMins = context.longestSessionMs / 60000L
            sb.append("- **Peak Strain Session:** Your longest continuous screen usage was **$longestMins minutes**. Consider introducing a quick physical stretch or water break after deep focus stretches!\n")
        }
        if (context.peakHours.isNotBlank() && context.peakHours != "No Data") {
            sb.append("- **Peak Activity Window:** Your screen time is most dense around **${context.peakHours}**. This is an ideal period to add mindful friction.\n")
        }
        
        if (context.topApps.isNotEmpty()) {
            sb.append("\n**Top Apps Review:**\n")
            val maxApp = context.topApps.first()
            val maxAppMins = maxApp.totalTimeInForegroundMs / 60000L
            sb.append("Your most active application today is **${maxApp.appName}** with **$maxAppMins minutes** of usage. ")
            
            val distractingCount = context.topApps.count { it.category == "Social Media" || it.category == "Games" || it.category == "Entertainment" }
            if (distractingCount > 0) {
                sb.append("You have a few high-stimulus apps on your dashboard today. By introducing dynamic friction barriers to these apps, you could redirect that mental bandwidth back to your main goal of *\"$goal\"*.\n")
            } else {
                sb.append("You are doing an exceptional job focusing your screen time on highly productive and intentional spaces! Keep protecting your mental clarity.\n")
            }
        }
        
        sb.append("\n### Actionable Next Steps\n")
        sb.append("To support your daily commitment to *$motivation*, here are a few gentle, empowering strategies you could practice:\n\n")
        
        if (context.activeLimitsCount == 0) {
            sb.append("1. 🛡️ **Establish Your First Barrier:** You don't have any active barriers right now. You could try adding a short micro-challenge to your most-frequented app to build a healthy pause.\n")
        } else {
            sb.append("1. 🛡️ **Optimize Your Barriers:** You currently have **${context.activeLimitsCount} active barriers** working for you. You are doing great! Continually check if these challenge models feel supportive or need adjustment.\n")
        }
        sb.append("2. 🔍 **Review Category Classifications:** Double-check your app categories. Moving more apps into the Productive or Distracting buckets helps customize your friction triggers.\n")
        sb.append("3. 🧠 **Take a Mindful Breath:** Next time you reach for your screen, pause for 5 seconds and check in with your motivation.\n\n")
        
        sb.append("[ACTION:SET_LIMIT|Set a Limit]\n")
        sb.append("[ACTION:OPEN_ENGINE|Open Friction Engine]\n")
        sb.append("[ACTION:CLASSIFY_APPS|Review App Classification]\n")
        sb.append("[ACTION:REVIEW_GOAL|Review Goal]\n")
        
        return sb.toString()
    }
}

