package com.example.data.model

import androidx.annotation.Keep

@Keep
data class User(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val guest: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLogin: Long = System.currentTimeMillis(),
    val level: Int = 1,
    val xp: Int = 0,
    val coins: Int = 0,
    val currentStreak: Int = 0,
    val screenTimeToday: Long = 0L,
    val unlocksToday: Int = 0,
    val premium: Boolean = false,
    val friendsCount: Int = 0,
    val age: Int = 0,
    val goal: String = "",
    val customGoal: String = "",
    val motivation: String = "",
    val unlockedBadges: List<String> = emptyList(),
    val customObjects: List<String> = emptyList(),
    val trialStartedAt: Long = 0L,
    val trialEndsAt: Long = 0L,
    val trialConsumed: Boolean = false,
    val isTrialActive: Boolean = false,
    val premiumPlan: String = "NONE",
    val subscriptionStatus: String = "FREE",
    val lastTrialValidation: Long = 0L,
    val hasSeenEarlyBirdOffer: Boolean = false
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "uid" to uid,
            "displayName" to displayName,
            "email" to email,
            "photoUrl" to photoUrl,
            "guest" to guest,
            "createdAt" to createdAt,
            "lastLogin" to lastLogin,
            "level" to level,
            "xp" to xp,
            "coins" to coins,
            "currentStreak" to currentStreak,
            "screenTimeToday" to screenTimeToday,
            "unlocksToday" to unlocksToday,
            "premium" to premium,
            "friendsCount" to friendsCount,
            "age" to age,
            "goal" to goal,
            "customGoal" to customGoal,
            "motivation" to motivation,
            "unlockedBadges" to unlockedBadges,
            "customObjects" to customObjects,
            "trialStartedAt" to trialStartedAt,
            "trialEndsAt" to trialEndsAt,
            "trialConsumed" to trialConsumed,
            "isTrialActive" to isTrialActive,
            "premiumPlan" to premiumPlan,
            "subscriptionStatus" to subscriptionStatus,
            "lastTrialValidation" to lastTrialValidation,
            "hasSeenEarlyBirdOffer" to hasSeenEarlyBirdOffer
        )
    }

    fun isPremiumEntitled(): Boolean {
        val now = System.currentTimeMillis()
        if (isTrialActive && trialEndsAt > now) return true
        return premium
    }

    fun getEntitlementStatusText(): String {
        val now = System.currentTimeMillis()
        if (isTrialActive && trialEndsAt > now) {
            val remainingDays = maxOf(1L, (trialEndsAt - now + 86399999L) / (86400000L))
            return "PRO TRIAL — $remainingDays DAY${if (remainingDays > 1) "S" else ""} LEFT"
        }
        if (premium) {
            return when (premiumPlan.uppercase()) {
                "LIFETIME" -> "LIFETIME PRO"
                "MONTHLY" -> "MONTHLY PRO"
                "ANNUAL", "YEARLY" -> "ANNUAL PRO"
                else -> when (subscriptionStatus.uppercase()) {
                    "LIFETIME_PRO" -> "LIFETIME PRO"
                    "MONTHLY_PRO" -> "MONTHLY PRO"
                    "ANNUAL_PRO" -> "ANNUAL PRO"
                    else -> "PRO MEMBER"
                }
            }
        }
        return "FREE PLAN"
    }

    fun hasTrialExpired(): Boolean {
        if (!isTrialActive) return false
        return System.currentTimeMillis() >= trialEndsAt
    }

    fun getGuestRemainingDays(): Long {
        val elapsedMs = System.currentTimeMillis() - createdAt
        val fourteenDaysMs = 14 * 24 * 3600 * 1000L
        val remainingMs = maxOf(0L, fourteenDaysMs - elapsedMs)
        return (remainingMs + (24 * 3600 * 1000L - 1)) / (24 * 3600 * 1000L)
    }

    fun isGuestExpired(): Boolean {
        if (!guest) return false
        val fourteenDaysMs = 14 * 24 * 3600 * 1000L
        return (System.currentTimeMillis() - createdAt) >= fourteenDaysMs
    }
}
