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
    val customObjects: List<String> = emptyList()
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
            "customObjects" to customObjects
        )
    }
}
