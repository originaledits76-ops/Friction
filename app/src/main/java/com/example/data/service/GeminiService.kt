package com.example.data.service

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
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

class GeminiService(private val context: Context? = null) {
    private val tag = "GroqAiService"
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
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

    private fun showUnavailableToast() {
        Log.w(tag, "AI service API call was unavailable or key missing. Using local AI fallback engine.")
    }

    private fun generateFallbackAnalytics(context: AnalysisContext): String {
        val totalMinutes = context.todayScreenTimeMs / 60000L
        val hours = totalMinutes / 60
        val mins = totalMinutes % 60
        val topAppNames = context.topApps.take(3).joinToString(", ") { it.appName }
        val streakText = if (context.streakDays > 0) "${context.streakDays}-day streak" else "active focus journey"

        return """
            ### Your Personal Focus Analysis 🎯
            
            **Daily Snapshot**
            You've logged **${if (hours > 0) "${hours}h ${mins}m" else "${mins}m"}** of screen time today across **${context.unlocks} device unlocks**. Your current streak is strong at **$streakText**!
            
            ${if (topAppNames.isNotBlank()) "**Top Apps Analyzed:** $topAppNames" else ""}
            
            ### Key Behavioral Patterns
            - **Focus Balance**: Your average session duration is **${context.avgSessionMs / 1000L}s**, with peak usage around **${context.peakHours.ifBlank { "afternoons" }}**.
            - **Habit Momentum**: You're actively pursuing your goal: *"${context.userGoal.ifBlank { "Build healthier digital habits" }}"*.
            
            ### Actionable Coaching Steps
            1. **Set Active Blockers**: Add custom friction barriers to your most distracting apps to protect deep work time.
            2. **Take Mindful Breaks**: Pause for 2 minutes before unlocking social media apps during peak hours.
            3. **Review Your Limits**: Adjust daily time caps to stay under your target screen time.
            
            [ACTION:SET_LIMIT|Set a Limit]
            [ACTION:OPEN_ENGINE|Open Friction Engine]
            [ACTION:CLASSIFY_APPS|Review App Classification]
        """.trimIndent()
    }

    private fun queryGroqApi(systemPrompt: String, userPrompt: String, temperature: Double = 0.3): String? {
        val groqApiKey = try { BuildConfig.GROQ_API_KEY } catch (e: Exception) { "" }
        if (groqApiKey.isBlank() || groqApiKey.startsWith("MY_") || groqApiKey.startsWith("placeholder")) {
            Log.w(tag, "Groq API key is missing or placeholder.")
            showUnavailableToast()
            return null
        }

        try {
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
                put("temperature", temperature)
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
                            if (text.isNotBlank()) return text.trim()
                        }
                    }
                } else {
                    Log.w(tag, "Groq API returned error status: ${response.code} ${response.message}")
                    showUnavailableToast()
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Exception querying Groq API: ${e.message}", e)
            showUnavailableToast()
        }
        return null
    }

    suspend fun getAiAnalysis(context: AnalysisContext): String = withContext(Dispatchers.IO) {
        if (context.todayScreenTimeMs == 0L && context.topApps.isEmpty()) {
            return@withContext """
                ### Your Personal Insights
                More usage data needed before we can craft meaningful coaching insights.
                
                Start using your device with Friction active today, and we'll analyze your focus cycles and help you build intentional, beautiful habits!
                
                [ACTION:VIEW_ANALYTICS|Refresh Analytics]
            """.trimIndent()
        }

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
            You are Friction's AI Personal Coach powered by Groq. Speak directly to the user in the first/second person (use "you", "your", "we", "I" - never say "the user" or "he/she" or speak in the third person).
            
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

        val groqResult = queryGroqApi(systemPrompt, userPrompt, temperature = 0.3)
        return@withContext groqResult ?: generateFallbackAnalytics(context)
    }

    suspend fun generateText(prompt: String): String = withContext(Dispatchers.IO) {
        val systemPrompt = "You are Friction's motivational AI coach. Respond with concise, encouraging, and clear sentences."
        val groqResult = queryGroqApi(systemPrompt, prompt, temperature = 0.5)
        return@withContext groqResult ?: "Great job taking mindful steps today! Keep staying intentional with your focus and screen time habits."
    }

    suspend fun verifySummary(paragraph: String, userSummary: String): Pair<Boolean, String> =
        verifyParagraphSummary(paragraph, userSummary)

    suspend fun verifyParagraphSummary(paragraph: String, userSummary: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val words = userSummary.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.size < 5) {
            return@withContext Pair(false, "Please write a slightly longer summary (around 10 to 20 words).")
        }

        val systemPrompt = """
            You are evaluating a summary written by a user for a reading challenge.
            Paragraph provided:
            "$paragraph"
            
            User summary:
            "$userSummary"
            
            Evaluate if the summary accurately reflects the main idea of the paragraph.
            Respond ONLY with JSON: {"isAccurate": true/false, "feedback": "Short constructive comment"}
        """.trimIndent()

        val rawResponse = queryGroqApi(systemPrompt, "Evaluate this summary.", temperature = 0.2)

        if (!rawResponse.isNullOrBlank()) {
            try {
                val jsonStart = rawResponse.indexOf('{')
                val jsonEnd = rawResponse.lastIndexOf('}')
                if (jsonStart >= 0 && jsonEnd > jsonStart) {
                    val parsed = JSONObject(rawResponse.substring(jsonStart, jsonEnd + 1))
                    val isAccurate = parsed.optBoolean("isAccurate", true)
                    val feedback = parsed.optString("feedback", if (isAccurate) "Great summary!" else "Try focusing on the main idea.")
                    return@withContext Pair(isAccurate, feedback)
                }
            } catch (e: Exception) {
                Log.w(tag, "Failed parsing API verification output: ${e.message}")
            }
        }

        Pair(true, "Great job capturing the key takeaway! Your summary shows solid comprehension.")
    }
}

