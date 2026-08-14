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
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
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

    private val _weeklyScreenTimeMs = MutableStateFlow(0L)
    val weeklyScreenTimeMs: StateFlow<Long> = _weeklyScreenTimeMs.asStateFlow()

    private val _monthlyScreenTimeMs = MutableStateFlow(0L)
    val monthlyScreenTimeMs: StateFlow<Long> = _monthlyScreenTimeMs.asStateFlow()

    private val _topApps = MutableStateFlow<List<AppUsageInfo>>(emptyList())
    val topApps: StateFlow<List<AppUsageInfo>> = _topApps.asStateFlow()

    private val _weeklyTopApps = MutableStateFlow<List<AppUsageInfo>>(emptyList())
    val weeklyTopApps: StateFlow<List<AppUsageInfo>> = _weeklyTopApps.asStateFlow()

    private val _monthlyTopApps = MutableStateFlow<List<AppUsageInfo>>(emptyList())
    val monthlyTopApps: StateFlow<List<AppUsageInfo>> = _monthlyTopApps.asStateFlow()

    private val _detailedAnalytics = MutableStateFlow<ScreenTimeService.DetailedAnalytics?>(null)
    val detailedAnalytics: StateFlow<ScreenTimeService.DetailedAnalytics?> = _detailedAnalytics.asStateFlow()

    private val _weeklyDetailedAnalytics = MutableStateFlow<ScreenTimeService.DetailedAnalytics?>(null)
    val weeklyDetailedAnalytics: StateFlow<ScreenTimeService.DetailedAnalytics?> = _weeklyDetailedAnalytics.asStateFlow()

    private val _monthlyDetailedAnalytics = MutableStateFlow<ScreenTimeService.DetailedAnalytics?>(null)
    val monthlyDetailedAnalytics: StateFlow<ScreenTimeService.DetailedAnalytics?> = _monthlyDetailedAnalytics.asStateFlow()

    // Database Flows
    val rules: StateFlow<List<FrictionRule>> = repository.allRules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val challenges: StateFlow<List<Challenge>> = repository.allChallenges
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val appClassifications: StateFlow<List<com.example.data.model.AppClassification>> = repository.allAppClassifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Daily/Weekly/Monthly History StateFlows for real-time reactivity
    private val _dailyHistory = MutableStateFlow<Map<String, Long>>(emptyMap())
    val dailyHistory: StateFlow<Map<String, Long>> = _dailyHistory.asStateFlow()

    private val _weeklyHistory = MutableStateFlow<Map<String, Long>>(emptyMap())
    val weeklyHistory: StateFlow<Map<String, Long>> = _weeklyHistory.asStateFlow()

    private val _monthlyHistory = MutableStateFlow<Map<String, Long>>(emptyMap())
    val monthlyHistory: StateFlow<Map<String, Long>> = _monthlyHistory.asStateFlow()

    // Today's hourly screen time breakdown for line graph (Hour 0..23 -> duration in ms)
    private val _todayHourlyScreenTimeMs = MutableStateFlow<Map<Int, Long>>(emptyMap())
    val todayHourlyScreenTimeMs: StateFlow<Map<Int, Long>> = _todayHourlyScreenTimeMs.asStateFlow()

    // Configured Daily Screen Time Limit (0L = No Limit Set)
    private val _dailyScreenTimeLimitMs = MutableStateFlow(prefs.getLong("daily_limit_ms", 0L))
    val dailyScreenTimeLimitMs: StateFlow<Long> = _dailyScreenTimeLimitMs.asStateFlow()

    fun setDailyScreenTimeLimit(limitMs: Long) {
        _dailyScreenTimeLimitMs.value = limitMs
        prefs.edit().apply {
            putLong("daily_limit_ms", limitMs)
            if (activeUserUid.isNotBlank()) {
                putLong("${activeUserUid}_daily_limit_ms", limitMs)
            }
            apply()
        }
        Log.i(tag, "[HomeViewModel] Set daily screen time limit to $limitMs ms (Active User: '$activeUserUid')")
    }

    // Buddy & Friends Lists
    private val _friends = MutableStateFlow<List<FriendInfo>>(emptyList())
    val friends: StateFlow<List<FriendInfo>> = _friends.asStateFlow()

    private val _browseFriendsList = MutableStateFlow<List<FriendInfo>>(emptyList())
    val browseFriendsList: StateFlow<List<FriendInfo>> = _browseFriendsList.asStateFlow()

    private val _selectedBuddyDetails = MutableStateFlow<com.example.data.model.BuddyDetails?>(null)
    val selectedBuddyDetails: StateFlow<com.example.data.model.BuddyDetails?> = _selectedBuddyDetails.asStateFlow()

    private val _isBuddyDetailsLoading = MutableStateFlow(false)
    val isBuddyDetailsLoading: StateFlow<Boolean> = _isBuddyDetailsLoading.asStateFlow()

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

    private val currentMonthStr: String
        get() = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

    private val todayDateStr: String
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    private val _streakSaversRemaining = MutableStateFlow(
        if (prefs.getString("streak_savers_month", "") == SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())) {
            prefs.getInt("streak_savers_remaining", 4)
        } else {
            4
        }
    )
    val streakSaversRemaining: StateFlow<Int> = _streakSaversRemaining.asStateFlow()

    private val _loginStreakDays = MutableStateFlow(prefs.getInt("login_bonus_streak_days", 1))
    val loginStreakDays: StateFlow<Int> = _loginStreakDays.asStateFlow()

    private val _todayResistanceXp = MutableStateFlow(
        if (prefs.getString("resistance_xp_date", "") == SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) {
            prefs.getInt("resistance_xp_count", 0)
        } else {
            0
        }
    )
    val todayResistanceXp: StateFlow<Int> = _todayResistanceXp.asStateFlow()

    private val _todayDailyLimitRewarded = MutableStateFlow(
        prefs.getStringSet("daily_limit_rewarded_dates", emptySet())
            ?.contains(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) == true
    )
    val todayDailyLimitRewarded: StateFlow<Boolean> = _todayDailyLimitRewarded.asStateFlow()

    private var lastCloseAppTimestamp = 0L

    private val _customObjects = MutableStateFlow<List<String>>(
        prefs.getString("custom_objects", "Water Bottle,Notebook,Backpack,Pen,Chair")
            ?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
            ?: listOf("Water Bottle", "Notebook", "Backpack", "Pen", "Chair")
    )
    val customObjects: StateFlow<List<String>> = _customObjects.asStateFlow()

    // AI Personal Insights Service
    private val geminiService = com.example.data.service.GeminiService(context)
    
    private val _aiCoachingState = MutableStateFlow("Tap 'Analyze My Habits' to generate personalized insights.")
    val aiCoachingState: StateFlow<String> = _aiCoachingState.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

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
        checkMonthlySaverReset()
        checkDailyLoginBonus()
        refreshMetrics()
        loadBuddiesAndLeaderboard("")
        startRealTimeUsagePolling()
    }

    fun checkMonthlySaverReset() {
        val savedMonth = prefs.getString("streak_savers_month", "")
        val thisMonth = currentMonthStr
        if (savedMonth != thisMonth) {
            _streakSaversRemaining.value = 4
            prefs.edit().apply {
                putString("streak_savers_month", thisMonth)
                putInt("streak_savers_remaining", 4)
                apply()
            }
        }
    }

    fun checkDailyLoginBonus() {
        val today = todayDateStr
        val lastLogin = prefs.getString("last_login_date", "")
        if (lastLogin == today) {
            _loginStreakDays.value = prefs.getInt("login_bonus_streak_days", 1)
            return
        }

        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)

        val currentStreak = if (lastLogin == yesterday) {
            prefs.getInt("login_bonus_streak_days", 0) + 1
        } else {
            1
        }

        val bonusXp = currentStreak * 5

        prefs.edit().apply {
            putString("last_login_date", today)
            putInt("login_bonus_streak_days", currentStreak)
            apply()
        }

        _loginStreakDays.value = currentStreak
        addXp(bonusXp, "Daily Login Bonus (Day $currentStreak)")
    }

    fun onCloseBlockedApp() {
        val now = System.currentTimeMillis()
        if (now - lastCloseAppTimestamp > 2500L) {
            lastCloseAppTimestamp = now
            val today = todayDateStr
            val lastDate = prefs.getString("resistance_xp_date", "")
            var currentCount = prefs.getInt("resistance_xp_count", 0)

            if (lastDate != today) {
                currentCount = 0
                prefs.edit().putString("resistance_xp_date", today).apply()
            }

            if (currentCount < 10) {
                currentCount++
                prefs.edit().putInt("resistance_xp_count", currentCount).apply()
                _todayResistanceXp.value = currentCount
                addXp(1, "Closed Blocked App ($currentCount/10)")
            } else {
                _todayResistanceXp.value = 10
            }
        }
    }

    fun checkDailyLimitXpReward() {
        val limitMs = _dailyScreenTimeLimitMs.value
        if (limitMs <= 0L) return

        val today = todayDateStr
        val rewardedSet = prefs.getStringSet("daily_limit_rewarded_dates", emptySet())?.toMutableSet() ?: mutableSetOf()

        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)

        val lastEvaluated = prefs.getString("last_evaluated_limit_date", "")
        if (lastEvaluated != yesterday && lastEvaluated != today) {
            val yesterdayStart = cal.apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
            val yesterdayEnd = cal.apply { set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999) }.timeInMillis

            val usageList = repository.getUsageDataForPeriod(yesterdayStart, yesterdayEnd)
            val yesterdayUsage: Long = usageList.sumOf { it.totalTimeInForegroundMs }

            if (yesterdayUsage <= limitMs && !rewardedSet.contains(yesterday)) {
                rewardedSet.add(yesterday)
                prefs.edit().putStringSet("daily_limit_rewarded_dates", rewardedSet).apply()
                addXp(30, "Under Daily Limit Reward ($yesterday)")
                _userStreak.value += 1
                saveProgress()
            } else if (yesterdayUsage > limitMs) {
                checkMonthlySaverReset()
                val remaining = _streakSaversRemaining.value
                if (remaining > 0) {
                    _streakSaversRemaining.value = remaining - 1
                    prefs.edit().putInt("streak_savers_remaining", _streakSaversRemaining.value).apply()
                } else {
                    _userStreak.value = 0
                    saveProgress()
                }
            }
            prefs.edit().putString("last_evaluated_limit_date", yesterday).apply()
        }

        _todayDailyLimitRewarded.value = rewardedSet.contains(today)
    }

    fun addXp(amount: Int, reason: String) {
        if (amount <= 0) return
        var currentXp = _userXp.value + amount
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

        if (currentLevel > oldLevel) {
            _lastLevelUpFrom.value = oldLevel
            _lastLevelUpTo.value = currentLevel
            _isLevelUpPending.value = true
        }

        saveProgress()

        if (activeUserUid.isNotEmpty()) {
            viewModelScope.launch {
                repository.syncUserProfileProgress(
                    userUid = activeUserUid,
                    level = _userLevel.value,
                    xp = _userXp.value,
                    coins = _userCoins.value,
                    streak = _userStreak.value,
                    unlockedBadges = _unlockedBadges.value,
                    customObjects = _customObjects.value
                )
            }
        }
    }

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

    fun hasConsumedRewardedAiAnalysisToday(): Boolean {
        val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        val consumedDate = prefs.getString("rewarded_ai_analysis_consumed_date", "")
        return consumedDate == todayStr
    }

    fun markRewardedAiAnalysisConsumed() {
        val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        prefs.edit().putString("rewarded_ai_analysis_consumed_date", todayStr).apply()
    }

    fun markRewardedAiConsumed() {
        markRewardedAiAnalysisConsumed()
    }

    fun generateAiCoaching(
        user: com.example.data.model.User,
        onPaywallRequired: () -> Unit,
        onAdPromptRequired: () -> Unit = {},
        onShowRewardedAdDialog: () -> Unit = onAdPromptRequired
    ) {
        verifyPremiumEntitlement(user) { isEntitled ->
            if (isEntitled) {
                executeAiCoachingDirectly()
            } else {
                if (hasConsumedRewardedAiAnalysisToday()) {
                    onPaywallRequired()
                } else {
                    onAdPromptRequired()
                    onShowRewardedAdDialog()
                }
            }
        }
    }

    fun generateAiCoachingForced(user: com.example.data.model.User? = null) {
        executeAiCoachingDirectly()
    }

    fun executeAiCoachingDirectly() {
        viewModelScope.launch {
            _isAiLoading.value = true
            _aiCoachingState.value = "Analyzing your habits..."
            try {
                refreshMetricsInternal()
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

    private fun startRealTimeUsagePolling() {
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(5000L) // Real-time poll every 5 seconds
                if (_isPermissionGranted.value) {
                    refreshMetricsInternal()
                }
            }
        }
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

        val uidDailyLimit = prefs.getLong("${userUid}_daily_limit_ms", prefs.getLong("daily_limit_ms", 0L))
        _dailyScreenTimeLimitMs.value = uidDailyLimit

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
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            refreshMetricsInternal()
        }
    }

    private suspend fun refreshMetricsInternal() {
        val granted = repository.isUsageAccessGranted()
        _isPermissionGranted.value = granted
        if (granted) {
            val now = System.currentTimeMillis()

            // 1. Daily Bounds (00:00:00 today to now)
            val calToday = java.util.Calendar.getInstance()
            calToday.set(java.util.Calendar.HOUR_OF_DAY, 0)
            calToday.set(java.util.Calendar.MINUTE, 0)
            calToday.set(java.util.Calendar.SECOND, 0)
            calToday.set(java.util.Calendar.MILLISECOND, 0)
            val startToday = calToday.timeInMillis

            // 2. Weekly Bounds (start of current calendar week 00:00:00 to now)
            val calWeek = java.util.Calendar.getInstance()
            calWeek.set(java.util.Calendar.DAY_OF_WEEK, calWeek.firstDayOfWeek)
            calWeek.set(java.util.Calendar.HOUR_OF_DAY, 0)
            calWeek.set(java.util.Calendar.MINUTE, 0)
            calWeek.set(java.util.Calendar.SECOND, 0)
            calWeek.set(java.util.Calendar.MILLISECOND, 0)
            val startWeek = calWeek.timeInMillis

            // 3. Monthly Bounds (1st day of current calendar month 00:00:00 to now)
            val calMonth = java.util.Calendar.getInstance()
            calMonth.set(java.util.Calendar.DAY_OF_MONTH, 1)
            calMonth.set(java.util.Calendar.HOUR_OF_DAY, 0)
            calMonth.set(java.util.Calendar.MINUTE, 0)
            calMonth.set(java.util.Calendar.SECOND, 0)
            calMonth.set(java.util.Calendar.MILLISECOND, 0)
            val startMonth = calMonth.timeInMillis

            // Fetch app usage for each period
            val dailyApps = repository.getTodayUsageData()
            val weeklyApps = repository.getUsageDataForPeriod(startWeek, now)
            val monthlyApps = repository.getUsageDataForPeriod(startMonth, now)

            _topApps.value = dailyApps
            _weeklyTopApps.value = weeklyApps
            _monthlyTopApps.value = monthlyApps

            _todayScreenTimeMs.value = dailyApps.sumOf { it.totalTimeInForegroundMs }
            _weeklyScreenTimeMs.value = weeklyApps.sumOf { it.totalTimeInForegroundMs }
            _monthlyScreenTimeMs.value = monthlyApps.sumOf { it.totalTimeInForegroundMs }

            _detailedAnalytics.value = repository.getDetailedAnalytics()
            _weeklyDetailedAnalytics.value = repository.getDetailedAnalyticsForPeriod(startWeek, now)
            _monthlyDetailedAnalytics.value = repository.getDetailedAnalyticsForPeriod(startMonth, now)

            _dailyHistory.value = repository.getDailyHistory()
            _weeklyHistory.value = repository.getWeeklyHistory()
            _monthlyHistory.value = repository.getMonthlyHistory()

            val hourlyData = ScreenTimeService(context).getTodayHourlyScreenTimeMs()
            _todayHourlyScreenTimeMs.value = hourlyData

            // Trigger notification check when approaching configured daily limit
            com.example.data.service.NotificationHelper.checkAndSendDailyLimitNotification(
                context = context,
                todayScreenTimeMs = _todayScreenTimeMs.value,
                dailyLimitMs = _dailyScreenTimeLimitMs.value
            )
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
            val actualXp = 0 // Requirement 5D: DO NOT award XP for challenge completion
            val entry = ChallengeHistoryEntry(
                id = "history_${UUID.randomUUID()}",
                title = title,
                type = type,
                status = "COMPLETED",
                rewardText = "+0 XP, +$coinsGained Coins",
                completionTimeSec = timeSec,
                dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            )
            
            // Add entry to history state
            val updatedList = _challengeHistory.value.toMutableList().apply { add(0, entry) }
            _challengeHistory.value = updatedList
            
            _userCoins.value += coinsGained
            
            saveProgress()
            checkBadges()

            Log.i(tag, "[completeChallenge] Challenge completed: '$title' ($type). +0 XP, +$coinsGained Coins (Total: ${_userCoins.value})")
            
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
                rewardText = "+0 XP",
                completionTimeSec = 0,
                dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            )
            
            // Add entry to history
            val updatedList = _challengeHistory.value.toMutableList().apply { add(0, entry) }
            _challengeHistory.value = updatedList
            
            saveProgress()

            Log.i(tag, "[skipChallenge] Challenge skipped: '$title' ($type). +0 XP change.")
            
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

    // Buddy invitations & Browse Friends
    fun loadBrowseFriends(currentUid: String) {
        viewModelScope.launch {
            val list = repository.getAllAppUsersWithStatus(currentUid)
            _browseFriendsList.value = list
        }
    }

    fun loadBuddyDetails(buddyUid: String) {
        viewModelScope.launch {
            _isBuddyDetailsLoading.value = true
            val details = repository.getBuddyDetails(buddyUid)
            _selectedBuddyDetails.value = details
            _isBuddyDetailsLoading.value = false
        }
    }

    fun clearBuddyDetails() {
        _selectedBuddyDetails.value = null
        _isBuddyDetailsLoading.value = false
    }

    fun sendFriendRequest(userUid: String, targetEmail: String) {
        viewModelScope.launch {
            repository.sendFriendRequest(userUid, targetEmail)
            loadBuddiesAndLeaderboard(userUid)
            loadBrowseFriends(userUid)
        }
    }

    fun sendFriendRequestToUid(userUid: String, targetUid: String) {
        viewModelScope.launch {
            repository.sendFriendRequestToUid(userUid, targetUid)
            loadBuddiesAndLeaderboard(userUid)
            loadBrowseFriends(userUid)
        }
    }

    fun acceptFriendRequest(
        userUid: String,
        friendUid: String,
        activeFriendsCount: Int,
        isPremium: Boolean,
        onLimitReached: () -> Unit
    ) {
        if (!isPremium && activeFriendsCount >= 2) {
            onLimitReached()
            return
        }
        viewModelScope.launch {
            repository.acceptFriendRequest(userUid, friendUid)
            loadBuddiesAndLeaderboard(userUid)
            loadBrowseFriends(userUid)
        }
    }

    fun rejectFriendRequest(userUid: String, friendUid: String) {
        viewModelScope.launch {
            repository.rejectFriendRequest(userUid, friendUid)
            loadBuddiesAndLeaderboard(userUid)
            loadBrowseFriends(userUid)
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

    fun verifyPremiumEntitlement(user: com.example.data.model.User, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val userRepository = com.example.data.repository.UserRepository(com.example.data.repository.FirestoreService())
                val fetchedUser = userRepository.getUser(user.uid)
                if (fetchedUser != null) {
                    val isEntitled = fetchedUser.premium || (fetchedUser.isTrialActive && !fetchedUser.hasTrialExpired())
                    onResult(isEntitled)
                } else {
                    val isLocalEntitled = user.premium || (user.isTrialActive && !user.hasTrialExpired())
                    onResult(isLocalEntitled)
                }
            } catch (e: Exception) {
                val isLocalEntitled = user.premium || (user.isTrialActive && !user.hasTrialExpired())
                onResult(isLocalEntitled)
            }
        }
    }
    fun markEarlyBirdOfferSeen(user: com.example.data.model.User) {
        if (!user.hasSeenEarlyBirdOffer) {
            val updatedUser = user.copy(hasSeenEarlyBirdOffer = true)
            updateUserProfile(updatedUser)
            Log.i(tag, "[HomeViewModel] Marked Early Bird offer as seen for UID '${user.uid}'")
        }
    }

    fun startFreeTrial(user: com.example.data.model.User, onTrialStarted: () -> Unit = {}) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val trialEnd = now + (3 * 24 * 3600 * 1000L) // 3-day trial
            val updatedUser = user.copy(
                premium = true,
                trialStartedAt = now,
                trialEndsAt = trialEnd,
                isTrialActive = true,
                trialConsumed = true,
                premiumPlan = "TRIAL",
                subscriptionStatus = "TRIAL",
                lastTrialValidation = now
            )
            updateUserProfile(updatedUser)
            Log.i(tag, "[HomeViewModel] Activated 3-day free trial for UID '${user.uid}'. Trial ends at: $trialEnd")
            onTrialStarted()
        }
    }

    fun purchasePlan(user: com.example.data.model.User, planName: String) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val updatedUser = user.copy(
                premium = true,
                isTrialActive = false,
                premiumPlan = planName,
                subscriptionStatus = "ACTIVE",
                lastTrialValidation = now
            )
            updateUserProfile(updatedUser)
            Log.i(tag, "[HomeViewModel] Purchased premium plan '$planName' for UID '${user.uid}'")
        }
    }
}
