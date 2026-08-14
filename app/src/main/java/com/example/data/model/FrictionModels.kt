package com.example.data.model

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey

@Keep
enum class RuleType {
    APP_LIMIT, SCREEN_TIME_LIMIT, FOCUS_INTERVAL, UNLOCK_COOLDOWN
}

@Keep
@Entity(tableName = "friction_rules")
data class FrictionRule(
    @PrimaryKey val id: String = "",
    val name: String = "",
    val type: RuleType = RuleType.APP_LIMIT,
    val targetAppPackage: String? = null,
    val thresholdMinutes: Int = 0,
    val penaltyXp: Int = 10,
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val targetAppName: String = "",
    val triggerType: String = "TIME",
    val challengeType: String = "MATH",
    val challengeValue: Int = 10

)

@Keep
@Entity(tableName = "friction_tasks")
data class FrictionTask(
    @PrimaryKey val id: String = "",
    val ruleId: String = "",
    val name: String = "",
    val completed: Boolean = false,
    val xpReward: Int = 15,
    val timestamp: Long = System.currentTimeMillis()
)

@Keep
@Entity(tableName = "friction_rewards")
data class FrictionReward(
    @PrimaryKey val id: String = "",
    val title: String = "",
    val costXp: Int = 50,
    val unlocked: Boolean = false,
    val description: String = ""
)

@Keep
@Entity(tableName = "friction_penalties")
data class FrictionPenalty(
    @PrimaryKey val id: String = "",
    val title: String = "",
    val type: String = "XP_LOSS",
    val amount: Int = 5,
    val timestamp: Long = System.currentTimeMillis()
)

@Keep
@Entity(tableName = "challenges")
data class Challenge(
    @PrimaryKey val id: String = "",
    val title: String = "",
    val description: String = "",
    val xpReward: Int = 20,
    val frictionSeconds: Int = 5,
    val type: String = "MATH", // MATH, BREATH, KEY_TAP, DRAW
    val solved: Boolean = false
)

@Keep
data class AppUsageInfo(
    val packageName: String,
    val appName: String,
    val totalTimeInForegroundMs: Long,
    val relativePercentage: Float = 0f,
    val category: String = "Entertainment"
)

@Keep
@Entity(tableName = "daily_usage_cache")
data class DailyUsageCache(
    @PrimaryKey val dateStr: String, // e.g. "2026-08-03"
    val totalScreenTimeMs: Long,
    val unlockCount: Int,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Keep
data class FriendInfo(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val currentStreak: Int = 0,
    val level: Int = 1,
    val xp: Int = 0,
    val status: String = "FRIEND" // "FRIEND", "SENT", "RECEIVED", "NONE"
)

@Keep
data class BuddyAppUsage(
    val packageName: String = "",
    val appName: String = "",
    val totalTimeMs: Long = 0L,
    val classification: String = "DISTRACTING" // "DISTRACTING" or "PRODUCTIVE"
)

@Keep
data class BuddyDetails(
    val uid: String = "",
    val displayName: String = "",
    val level: Int = 1,
    val xp: Int = 0,
    val currentStreak: Int = 0,
    val todayScreenTimeMs: Long = 0L,
    val topAppsToday: List<BuddyAppUsage> = emptyList()
)

@Keep
@Entity(tableName = "app_classifications")
data class AppClassification(
    @PrimaryKey val packageName: String = "",
    val appName: String = "",
    val classification: String = "DISTRACTING" // "DISTRACTING" or "PRODUCTIVE"
)

@Keep
@Entity(tableName = "challenge_history")
data class ChallengeHistoryEntity(
    @PrimaryKey val id: String = "",
    val userUid: String = "",
    val title: String = "",
    val type: String = "",
    val status: String = "COMPLETED", // "COMPLETED" or "SKIPPED"
    val rewardText: String = "",
    val completionTimeSec: Int = 0,
    val dateStr: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
