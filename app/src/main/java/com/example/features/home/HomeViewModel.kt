package com.example.features.home

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.FrictionDatabase
import com.example.data.model.AppUsageInfo
import com.example.data.model.Challenge
import com.example.data.model.FriendInfo
import com.example.data.model.FrictionRule
import com.example.data.model.RuleType
import com.example.data.repository.FrictionRepository
import com.example.data.service.ScreenTimeService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class ChallengeHistoryEntry(
    val id: String,
    val title: String,
    val type: String,
    val status: String, // "COMPLETED" or "SKIPPED"
    val rewardText: String,
    val completionTimeSec: Int,
    val dateStr: String
) {
    fun serialize(): String {
        return "$id||$title||$type||$status||$rewardText||$completionTimeSec||$dateStr"
    }

    companion object {
        fun deserialize(str: String): ChallengeHistoryEntry? {
            val parts = str.split("||")
            if (parts.size < 7) return null
            return ChallengeHistoryEntry(
                id = parts[0],
                title = parts[1],
                type = parts[2],
                status = parts[3],
                rewardText = parts[4],
                completionTimeSec = parts[5].toIntOrNull() ?: 0,
                dateStr = parts[6]
            )
        }
    }
}

class HomeViewModel(
    private val repository: FrictionRepository,
    private val context: Context
) : ViewModel() {

    private val tag = "HomeViewModel"
    private val prefs = context.getSharedPreferences("friction_progress_prefs", Context.MODE_PRIVATE)

    // Permission state
    private val _isPermissionGranted = MutableStateFlow(repository.isUsageAccessGranted())
    val isPermissionGranted: StateFlow<Boolean> = _isPermissionGranted.asStateFlow()

    // Screen Time Metrics
    private val _todayScreenTimeMs = MutableStateFlow(0L)
    val todayScreenTimeMs: StateFlow<Long> = _todayScreenTimeMs.asStateFlow()

    private val _topApps = MutableStateFlow<List<AppUsageInfo>>(emptyList())
    val topApps: StateFlow<List<AppUsageInfo>> = _topApps.asStateFlow()

    private val _detailedAnalytics = MutableStateFlow<ScreenTimeService.DetailedAnalytics?>(null)
    val detailedAnalytics: StateFlow<ScreenTimeService.DetailedAnalytics?> = _detailedAnalytics.asStateFlow()

    // Database Flows
    val rules: StateFlow<List<FrictionRule>> = repository.allRules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val challenges: StateFlow<List<Challenge>> = repository.allChallenges
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val appClassifications: StateFlow<List<com.example.data.model.AppClassification>> = repository.allAppClassifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Daily/Weekly/Monthly History
    val dailyHistory: Map<String, Long> = repository.getDailyHistory()
    val weeklyHistory: Map<String, Long> = repository.getWeeklyHistory()
    val monthlyHistory: Map<String, Long> = repository.getMonthlyHistory()

    // Buddy & Friends Lists
    private val _friends = MutableStateFlow<List<FriendInfo>>(emptyList())
    val friends: StateFlow<List<FriendInfo>> = _friends.asStateFlow()

    // Leaderboards
    private val _leaderboardWeekly = MutableStateFlow<List<FriendInfo>>(emptyList())
    val leaderboardWeekly: StateFlow<List<FriendInfo>> = _leaderboardWeekly.asStateFlow()

    private val _leaderboardMonthly = MutableStateFlow<List<FriendInfo>>(emptyList())
    val leaderboardMonthly: StateFlow<List<FriendInfo>> = _leaderboardMonthly.asStateFlow()

    // User Progress State (Level, XP, Coins, Streak, Badges, Challenge History)
    private val _userLevel = MutableStateFlow(prefs.getInt("user_level", 1))
    val userLevel: StateFlow<Int> = _userLevel.asStateFlow()

    private val _userXp = MutableStateFlow(prefs.getInt("user_xp", 0))
    val userXp: StateFlow<Int> = _userXp.asStateFlow()

    private val _userCoins = MutableStateFlow(prefs.getInt("user_coins", 0))
    val userCoins: StateFlow<Int> = _userCoins.asStateFlow()

    private val _userStreak = MutableStateFlow(prefs.getInt("user_streak", 0))
    val userStreak: StateFlow<Int> = _userStreak.asStateFlow()

    private val _customObjects = MutableStateFlow<List<String>>(
        prefs.getString("custom_objects", "Water Bottle,Notebook,Backpack,Pen,Chair")
            ?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
            ?: listOf("Water Bottle", "Notebook", "Backpack", "Pen", "Chair")
    )
    val customObjects: StateFlow<List<String>> = _customObjects.asStateFlow()

    fun updateCustomObjects(objects: List<String>) {
        val clean = objects.map { it.trim() }.filter { it.isNotEmpty() }.take(5)
        if (clean.isNotEmpty()) {
            _customObjects.value = clean
            prefs.edit().apply {
                putString("custom_objects", clean.joinToString(","))
                if (activeUserUid.isNotBlank()) {
                    putString("${activeUserUid}_custom_objects", clean.joinToString(","))
                }
                apply()
            }
            Log.i(tag, "[HomeViewModel] Custom objects updated locally: $clean (Active User: '$activeUserUid')")
            
            if (activeUserUid.isNotBlank()) {
                viewModelScope.launch {
                    try {
                        repository.syncUserProfileProgress(
                            userUid = activeUserUid,
                            level = _userLevel.value,
                            xp = _userXp.value,
                            coins = _userCoins.value,
                            streak = _userStreak.value,
                            unlockedBadges = _unlockedBadges.value,
                            customObjects = clean
                        )
                        Log.i(tag, "[HomeViewModel] Custom object list successfully synced to Firestore for UID '$activeUserUid'")
                    } catch (e: Exception) {
                        Log.e(tag, "[HomeViewModel] Failed to sync custom objects list to Firestore for UID '$activeUserUid': ${e.message}", e)
                    }
                }
            }
        }
    }

    // AI Personal Insights Service
    private val geminiService = com.example.data.service.GeminiService()
    
    private val _aiCoachingState = MutableStateFlow("Tap 'Analyze My Habits' to generate personalized insights.")
    val aiCoachingState: StateFlow<String> = _aiCoachingState.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    fun generateAiCoaching() {
        viewModelScope.launch {
            _isAiLoading.value = true
            _aiCoachingState.value = "Analyzing your habits..."
            try {
                val todayTime = _todayScreenTimeMs.value
                val apps = _topApps.value
                val detailed = _detailedAnalytics.value
                val activeLimits = rules.value.count { it.active }
                
                val unlocks = detailed?.unlockCount ?: 0
                val avgSession = detailed?.averageSessionMs ?: 0L
                val longestSession = detailed?.longestSessionMs ?: 0L
                val peakHours = detailed?.peakUsageHours ?: "No Data"

                val ctx = com.example.data.service.GeminiService.AnalysisContext(
                    todayScreenTimeMs = todayTime,
                    topApps = apps,
                    unlocks = unlocks,
                    avgSessionMs = avgSession,
                    longestSessionMs = longestSession,
                    peakHours = peakHours,
                    streakDays = _userStreak.value,
                    xp = _userXp.value,
                    userGoal = prefs.getString("demo_goal", "") ?: "",
                    userMotivation = prefs.getString("demo_motivation", "") ?: "",
                    activeLimitsCount = activeLimits
                )

                val result = geminiService.getAiAnalysis(ctx)
                _aiCoachingState.value = result
            } catch (e: Exception) {
                _aiCoachingState.value = "AI service not available."
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    private val _unlockedBadges = MutableStateFlow<List<String>>(
        prefs.getString("unlocked_badges", "")?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()
    )
    val unlockedBadges: StateFlow<List<String>> = _unlockedBadges.asStateFlow()

    private val _challengeHistory = MutableStateFlow<List<ChallengeHistoryEntry>>(
        prefs.getStringSet("challenge_history", emptySet())?.mapNotNull { ChallengeHistoryEntry.deserialize(it) }?.sortedByDescending { it.dateStr } ?: emptyList()
    )
    val challengeHistory: StateFlow<List<ChallengeHistoryEntry>> = _challengeHistory.asStateFlow()

    // Level-up Celebration States
    private val _isLevelUpPending = MutableStateFlow(false)
    val isLevelUpPending: StateFlow<Boolean> = _isLevelUpPending.asStateFlow()

    private val _lastLevelUpFrom = MutableStateFlow(1)
    val lastLevelUpFrom: StateFlow<Int> = _lastLevelUpFrom.asStateFlow()

    private val _lastLevelUpTo = MutableStateFlow(2)
    val lastLevelUpTo: StateFlow<Int> = _lastLevelUpTo.asStateFlow()

    private var activeUserUid: String = ""

    init {
        refreshMetrics()
        loadBuddiesAndLeaderboard("")
    }

    // Load user specifically
    fun setUserContext(userUid: String) {
        if (userUid.isBlank()) return
        activeUserUid = userUid
        Log.i(tag, "[setUserContext] Setting user context for UID '$userUid'")
        
        // Restore user progress from local cache per UID
        val uidLevel = prefs.getInt("${userUid}_level", prefs.getInt("user_level", 1))
        val uidXp = prefs.getInt("${userUid}_xp", prefs.getInt("user_xp", 0))
        val uidCoins = prefs.getInt("${userUid}_coins", prefs.getInt("user_coins", 0))
        val uidStreak = prefs.getInt("${userUid}_streak", prefs.getInt("user_streak", 0))
        val uidBadges = prefs.getString("${userUid}_badges", null)?.split(",")?.filter { it.isNotEmpty() } 
            ?: prefs.getString("unlocked_badges", "")?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()
        val uidCustomObjs = prefs.getString("${userUid}_custom_objects", null)?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
            ?: prefs.getString("custom_objects", "Water Bottle,Notebook,Backpack,Pen,Chair")?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
            ?: listOf("Water Bottle", "Notebook", "Backpack", "Pen", "Chair")

        _userLevel.value = uidLevel
        _userXp.value = uidXp
        _userCoins.value = uidCoins
        _userStreak.value = uidStreak
        _unlockedBadges.value = uidBadges
        _customObjects.value = uidCustomObjs

        loadBuddiesAndLeaderboard(userUid)

        // Collect local Room DB challenge history flow
        viewModelScope.launch {
            repository.getLocalChallengeHistoryFlow(userUid).collect { entities ->
                if (entities.isNotEmpty()) {
                    val entries = entities.map { entity ->
                        ChallengeHistoryEntry(
                            id = entity.id,
                            title = entity.title,
                            type = entity.type,
                            status = entity.status,
                            rewardText = entity.rewardText,
                            completionTimeSec = entity.completionTimeSec,
                            dateStr = entity.dateStr
                        )
                    }
                    _challengeHistory.value = entries
                    Log.d(tag, "[HomeViewModel] Loaded ${entries.size} challenge history entries from local Room DB for UID '$userUid'")
                }
            }
        }

        // Sync history from Firestore cloud
        viewModelScope.launch {
            try {
                val cloudEntries = repository.syncChallengeHistoryFromCloud(userUid)
                Log.i(tag, "[HomeViewModel] Synced ${cloudEntries.size} challenge history entries from cloud for UID '$userUid'")
            } catch (e: Exception) {
                Log.e(tag, "[HomeViewModel] Cloud challenge history sync failed for UID '$userUid': ${e.message}", e)
            }
        }
    }

    fun checkPermission() {
        val granted = repository.isUsageAccessGranted()
        _isPermissionGranted.value = granted
        if (granted) {
            refreshMetrics()
        }
    }

    fun refreshMetrics() {
        viewModelScope.launch {
            val appStats = repository.getTodayUsageData()
            _topApps.value = appStats
            _todayScreenTimeMs.value = appStats.sumOf { it.totalTimeInForegroundMs }
            _detailedAnalytics.value = repository.getDetailedAnalytics()
        }
    }

    fun loadBuddiesAndLeaderboard(userUid: String) {
        viewModelScope.launch {
            repository.getFriendsFlow(userUid).collect {
                _friends.value = it
            }
        }
        viewModelScope.launch {
            repository.getLeaderboard("WEEKLY").collect {
                _leaderboardWeekly.value = it
            }
        }
        viewModelScope.launch {
            repository.getLeaderboard("MONTHLY").collect {
                _leaderboardMonthly.value = it
            }
        }
    }

    // Rules actions
    fun toggleRule(ruleId: String, active: Boolean) {
        viewModelScope.launch {
            val current = rules.value.find { it.id == ruleId }
            if (current != null) {
                repository.insertRule(current.copy(active = active))
                checkBadges()
            }
        }
    }

    fun addRule(rule: FrictionRule) {
        viewModelScope.launch {
            repository.insertRule(rule)
            checkBadges()
        }
    }

    fun duplicateRule(ruleId: String) {
        viewModelScope.launch {
            val rule = rules.value.find { it.id == ruleId }
            if (rule != null) {
                val duplicated = rule.copy(
                    id = "rule_${UUID.randomUUID()}",
                    name = "${rule.name} (Copy)",
                    createdAt = System.currentTimeMillis()
                )
                repository.insertRule(duplicated)
                checkBadges()
            }
        }
    }

    fun deleteRule(ruleId: String) {
        viewModelScope.launch {
            repository.deleteRule(ruleId)
        }
    }

    // Challenge & Task execution actions
    fun completeChallenge(title: String, type: String, xpGained: Int, coinsGained: Int, timeSec: Int) {
        viewModelScope.launch {
            val entry = ChallengeHistoryEntry(
                id = "history_${UUID.randomUUID()}",
                title = title,
                type = type,
                status = "COMPLETED",
                rewardText = "+$xpGained XP, +$coinsGained Coins",
                completionTimeSec = timeSec,
                dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            )
            
            // Add entry to history state
            val updatedList = _challengeHistory.value.toMutableList().apply { add(0, entry) }
            _challengeHistory.value = updatedList
            
            // Handle XP, Coins, and Level Ups
            var currentXp = _userXp.value + xpGained
            var currentLevel = _userLevel.value
            val oldLevel = currentLevel
            var xpNeeded = currentLevel * 100
            
            while (currentXp >= xpNeeded) {
                currentXp -= xpNeeded
                currentLevel++
                xpNeeded = currentLevel * 100
            }
            
            _userXp.value = currentXp
            _userLevel.value = currentLevel
            _userCoins.value += coinsGained
            
            if (currentLevel > oldLevel) {
                _lastLevelUpFrom.value = oldLevel
                _lastLevelUpTo.value = currentLevel
                _isLevelUpPending.value = true
                Log.i(tag, "🎉 LEVEL UP! User leveled up from $oldLevel to $currentLevel!")
            }
            
            // Increment streak dynamically
            _userStreak.value += 1
            
            saveProgress()
            checkBadges()

            Log.i(tag, "[completeChallenge] Challenge completed: '$title' ($type). +$xpGained XP (Total XP: ${_userXp.value}), +$coinsGained Coins (Total: ${_userCoins.value}), Level: ${_userLevel.value}, Streak: ${_userStreak.value}")
            
            // Sync with remote firestore and Room if active user is set
            if (activeUserUid.isNotEmpty()) {
                repository.syncUserProfileProgress(
                    userUid = activeUserUid,
                    level = _userLevel.value,
                    xp = _userXp.value,
                    coins = _userCoins.value,
                    streak = _userStreak.value,
                    unlockedBadges = _unlockedBadges.value,
                    customObjects = _customObjects.value
                )
                repository.saveChallengeHistory(activeUserUid, entry)
            }
        }
    }

    fun skipChallenge(title: String, type: String, penaltyXp: Int) {
        viewModelScope.launch {
            val entry = ChallengeHistoryEntry(
                id = "history_${UUID.randomUUID()}",
                title = title,
                type = type,
                status = "SKIPPED",
                rewardText = "-$penaltyXp XP",
                completionTimeSec = 0,
                dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            )
            
            // Add entry to history
            val updatedList = _challengeHistory.value.toMutableList().apply { add(0, entry) }
            _challengeHistory.value = updatedList
            
            // Penalize XP slightly (no negative values)
            val newXp = (_userXp.value - penaltyXp).coerceAtLeast(0)
            _userXp.value = newXp
            
            if (_userStreak.value > 0) {
                _userStreak.value = (_userStreak.value - 1).coerceAtLeast(1)
            }
            
            saveProgress()

            Log.i(tag, "[skipChallenge] Challenge skipped: '$title' ($type). Penalty -$penaltyXp XP (Remaining XP: ${_userXp.value})")
            
            if (activeUserUid.isNotEmpty()) {
                repository.syncUserProfileProgress(
                    userUid = activeUserUid,
                    level = _userLevel.value,
                    xp = _userXp.value,
                    coins = _userCoins.value,
                    streak = _userStreak.value,
                    unlockedBadges = _unlockedBadges.value,
                    customObjects = _customObjects.value
                )
                repository.saveChallengeHistory(activeUserUid, entry)
            }
        }
    }

    fun triggerLevelUpDone() {
        _isLevelUpPending.value = false
    }

    fun checkBadges() {
        val badges = _unlockedBadges.value.toMutableList()
        var updated = false

        // 1. First Rule badge
        if (rules.value.isNotEmpty() && !badges.contains("First Rule")) {
            badges.add("First Rule")
            updated = true
        }

        // 2. 7 Day Streak badge
        if (_userStreak.value >= 7 && !badges.contains("7 Day Streak")) {
            badges.add("7 Day Streak")
            updated = true
        }

        // 3. 10 Challenges Completed
        val completedCount = _challengeHistory.value.count { it.status == "COMPLETED" }
        if (completedCount >= 10 && !badges.contains("10 Challenges Completed")) {
            badges.add("10 Challenges Completed")
            updated = true
        }

        // 4. First Reading Session
        val hasReading = _challengeHistory.value.any { it.status == "COMPLETED" && (it.type == "READ" || it.title.contains("Read", ignoreCase = true)) }
        if (hasReading && !badges.contains("First Reading Session")) {
            badges.add("First Reading Session")
            updated = true
        }

        // 5. First Walk
        val hasWalk = _challengeHistory.value.any { it.status == "COMPLETED" && (it.type == "WALK" || it.type == "RUN" || it.title.contains("Walk", ignoreCase = true) || it.title.contains("Run", ignoreCase = true)) }
        if (hasWalk && !badges.contains("First Walk")) {
            badges.add("First Walk")
            updated = true
        }

        // 6. 100 Push-ups badge (if total completed push-ups is estimated from push-up historical challenges)
        val totalPushups = _challengeHistory.value.filter { it.status == "COMPLETED" && it.title.contains("Push-ups", ignoreCase = true) }.size * 15
        if (totalPushups >= 100 && !badges.contains("100 Push-ups")) {
            badges.add("100 Push-ups")
            updated = true
        }

        if (updated) {
            _unlockedBadges.value = badges
            saveProgress()
            Log.i(tag, "[checkBadges] Unlocked new badges! Badges now: $badges")
        }
    }

    private fun saveProgress() {
        prefs.edit().apply {
            putInt("user_level", _userLevel.value)
            putInt("user_xp", _userXp.value)
            putInt("user_coins", _userCoins.value)
            putInt("user_streak", _userStreak.value)
            putString("unlocked_badges", _unlockedBadges.value.joinToString(","))
            putStringSet("challenge_history", _challengeHistory.value.map { it.serialize() }.toSet())

            if (activeUserUid.isNotBlank()) {
                putInt("${activeUserUid}_level", _userLevel.value)
                putInt("${activeUserUid}_xp", _userXp.value)
                putInt("${activeUserUid}_coins", _userCoins.value)
                putInt("${activeUserUid}_streak", _userStreak.value)
                putString("${activeUserUid}_badges", _unlockedBadges.value.joinToString(","))
                putString("${activeUserUid}_custom_objects", _customObjects.value.joinToString(","))
            }
            apply()
        }
        Log.d(tag, "[saveProgress] User progress saved locally for UID '$activeUserUid' - Level: ${_userLevel.value}, XP: ${_userXp.value}, Coins: ${_userCoins.value}, Streak: ${_userStreak.value}")
    }

    // Buddy invitations
    fun sendFriendRequest(userUid: String, targetEmail: String) {
        viewModelScope.launch {
            repository.sendFriendRequest(userUid, targetEmail)
            loadBuddiesAndLeaderboard(userUid)
        }
    }

    fun acceptFriendRequest(userUid: String, friendUid: String) {
        viewModelScope.launch {
            repository.acceptFriendRequest(userUid, friendUid)
            loadBuddiesAndLeaderboard(userUid)
        }
    }

    fun rejectFriendRequest(userUid: String, friendUid: String) {
        viewModelScope.launch {
            repository.rejectFriendRequest(userUid, friendUid)
            loadBuddiesAndLeaderboard(userUid)
        }
    }

    fun saveAppClassification(pkg: String, name: String, classification: String) {
        viewModelScope.launch {
            repository.saveAppClassification(pkg, name, classification)
        }
    }

    fun updateUserProfile(user: com.example.data.model.User) {
        viewModelScope.launch {
            com.example.data.repository.AuthRepository(context).saveCachedUser(user)
            repository.updateUserProfile(user)
            Log.i(tag, "[HomeViewModel] User profile updated & cached: Name='${user.displayName}', Goal='${user.goal}'")
        }
    }
}
