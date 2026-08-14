package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.FrictionDao
import com.example.data.model.*
import com.example.data.service.ScreenTimeService
import com.example.features.home.ChallengeHistoryEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FrictionRepository(
    private val context: Context,
    private val dao: FrictionDao,
    private val screenTimeService: ScreenTimeService
) {
    private val tag = "FrictionRepository"

    // New Firebase Services & Repositories
    private val firestoreService = FirestoreService()
    private val userRepository = UserRepository(firestoreService)
    private val friendsRepository = FriendsRepository(firestoreService)
    private val leaderboardRepository = LeaderboardRepository(firestoreService)
    private val analyticsRepository = AnalyticsRepository(firestoreService)
    private val blockedAppsRepository = BlockedAppsRepository(firestoreService)
    private val challengeRepository = ChallengeRepository(firestoreService, dao)

    private val currentUserUid: String?
        get() = SafeFirebase.currentUser?.uid ?: context.getSharedPreferences("friction_prefs", Context.MODE_PRIVATE).getString("active_uid", null) ?: context.getSharedPreferences("friction_prefs", Context.MODE_PRIVATE).getString("demo_uid", null)

    fun getLocalChallengeHistoryFlow(userUid: String): Flow<List<com.example.data.model.ChallengeHistoryEntity>> {
        return dao.getChallengeHistory(userUid)
    }

    suspend fun syncChallengeHistoryFromCloud(userUid: String): List<ChallengeHistoryEntry> {
        return challengeRepository.fetchAndSyncChallengeHistoryFromCloud(userUid)
    }

    suspend fun syncUserProfileProgress(
        userUid: String,
        level: Int,
        xp: Int,
        coins: Int,
        streak: Int,
        unlockedBadges: List<String>,
        customObjects: List<String>
    ) {
        userRepository.syncProfileProgress(userUid, level, xp, coins, streak, unlockedBadges, customObjects)
    }

    init {
        // Prepopulate empty database with initial rules/challenges for offline support
        CoroutineScope(Dispatchers.IO).launch {
            prepopulateInitialData()
        }
        
        // Sync rules from cloud Firestore in background if online
        CoroutineScope(Dispatchers.IO).launch {
            syncRulesFromCloud()
        }
    }

    // Direct delegate checks for screen time permissions
    fun isUsageAccessGranted(): Boolean = screenTimeService.isUsageAccessGranted()

    fun getTodayUsageData(): List<AppUsageInfo> {
        val data = screenTimeService.getTodayUsageData()
        val uid = currentUserUid
        if (uid != null && data.isNotEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                val totalTime = data.sumOf { it.totalTimeInForegroundMs }
                // Save daily analytics
                analyticsRepository.saveDailyAnalytics(uid, totalTime, data)
                // Update stats in user document
                userRepository.updateStats(uid, totalTime, if (totalTime > 0) (totalTime / 600000L).toInt().coerceAtLeast(1) else 0)
            }
        }
        return data
    }

    fun getUsageDataForPeriod(startTime: Long, endTime: Long): List<AppUsageInfo> {
        return screenTimeService.getUsageDataForPeriod(startTime, endTime)
    }

    fun getDetailedAnalyticsForPeriod(startTime: Long, endTime: Long): ScreenTimeService.DetailedAnalytics {
        return screenTimeService.getDetailedAnalyticsForPeriod(startTime, endTime)
    }

    fun getDailyHistory(): Map<String, Long> = screenTimeService.getDailyHistory()

    fun getWeeklyHistory(): Map<String, Long> = screenTimeService.getWeeklyHistory()

    fun getMonthlyHistory(): Map<String, Long> = screenTimeService.getMonthlyHistory()

    fun getDetailedAnalytics(): ScreenTimeService.DetailedAnalytics = screenTimeService.getDetailedAnalytics()

    // Database Flows
    val allRules: Flow<List<FrictionRule>> = dao.getAllRules()
    val allTasks: Flow<List<FrictionTask>> = dao.getAllTasks()
    val allAppClassifications: Flow<List<com.example.data.model.AppClassification>> = dao.getAllAppClassifications()
    val allRewards: Flow<List<FrictionReward>> = dao.getAllRewards()
    val allPenalties: Flow<List<FrictionPenalty>> = dao.getAllPenalties()
    val allChallenges: Flow<List<Challenge>> = dao.getAllChallenges()

    suspend fun insertRule(rule: FrictionRule) {
        dao.insertRule(rule)
        val uid = currentUserUid
        val db = firestoreService.db
        if (uid != null && db != null) {
            try {
                val ruleMap = mapOf(
                    "userUid" to uid,
                    "ruleId" to rule.id,
                    "name" to rule.name,
                    "type" to rule.type.name,
                    "targetAppPackage" to rule.targetAppPackage,
                    "thresholdMinutes" to rule.thresholdMinutes,
                    "penaltyXp" to rule.penaltyXp,
                    "active" to rule.active,
                    "createdAt" to rule.createdAt
                )
                db.collection("rules").document("${uid}_${rule.id}")
                    .set(ruleMap, SetOptions.merge()).await()
                
                // Track blocked app package name
                if (rule.targetAppPackage != null) {
                    if (rule.active) {
                        blockedAppsRepository.saveBlockedApp(uid, rule.targetAppPackage, rule.id)
                    } else {
                        blockedAppsRepository.removeBlockedApp(uid, rule.targetAppPackage)
                    }
                }
            } catch (e: Exception) {
                Log.i(tag, "Rule insert remote sync note: ${e.message}")
            }
        }
    }

    suspend fun deleteRule(id: String) {
        dao.deleteRule(id)
        val uid = currentUserUid
        val db = firestoreService.db
        if (uid != null && db != null) {
            try {
                db.collection("rules").document("${uid}_$id").delete().await()
                Log.d(tag, "Deleted rule from Firestore: ${uid}_$id")
            } catch (e: Exception) {
                Log.i(tag, "Rule delete remote sync note: ${e.message}")
            }
        }
    }

    suspend fun insertTask(task: FrictionTask) = dao.insertTask(task)
    suspend fun insertPenalty(penalty: FrictionPenalty) = dao.insertPenalty(penalty)

    /**
     * Friends flow loaded strictly from Firestore
     */
    fun getFriendsFlow(userUid: String): Flow<List<FriendInfo>> {
        return friendsRepository.getFriendsFlow(userUid)
    }

    suspend fun searchUsers(currentUid: String, query: String): List<FriendInfo> {
        return friendsRepository.searchUsers(currentUid, query)
    }

    suspend fun sendFriendRequest(userUid: String, targetEmail: String): Boolean {
        return friendsRepository.sendFriendRequest(userUid, targetEmail)
    }

    suspend fun acceptFriendRequest(userUid: String, friendUid: String) {
        friendsRepository.acceptFriendRequest(userUid, friendUid)
    }

    suspend fun rejectFriendRequest(userUid: String, friendUid: String) {
        friendsRepository.rejectFriendRequest(userUid, friendUid)
    }

    suspend fun getAllAppUsersWithStatus(currentUid: String): List<FriendInfo> {
        return friendsRepository.getAllAppUsersWithStatus(currentUid)
    }

    suspend fun sendFriendRequestToUid(userUid: String, targetUid: String): Boolean {
        return friendsRepository.sendFriendRequestToUid(userUid, targetUid)
    }

    suspend fun getBuddyDetails(buddyUid: String): com.example.data.model.BuddyDetails? {
        return friendsRepository.getBuddyDetails(buddyUid)
    }

    /**
     * Leaderboards loaded strictly from Firestore
     */
    fun getLeaderboard(type: String): Flow<List<FriendInfo>> {
        return leaderboardRepository.getLeaderboard(type)
    }

    suspend fun syncUserProfile(
        userUid: String,
        level: Int,
        xp: Int,
        coins: Int = 0,
        streak: Int,
        unlockedBadges: List<String> = emptyList(),
        customObjects: List<String> = emptyList()
    ) {
        userRepository.syncProfileProgress(userUid, level, xp, coins, streak, unlockedBadges, customObjects)
    }

    suspend fun updateUserProfile(user: User): Boolean {
        return userRepository.createOrUpdateUser(user)
    }

    suspend fun saveChallengeHistory(userUid: String, entry: ChallengeHistoryEntry) {
        challengeRepository.saveChallengeHistory(userUid, entry)
    }

    private suspend fun syncRulesFromCloud() {
        val uid = currentUserUid ?: return
        if (uid.startsWith("guest_") || uid.startsWith("offline_")) return
        val db = firestoreService.db ?: return
        try {
            val rulesSnapshot = db.collection("rules")
                .whereEqualTo("userUid", uid)
                .get().await()

            for (doc in rulesSnapshot.documents) {
                val ruleId = doc.getString("ruleId") ?: continue
                val name = doc.getString("name") ?: ""
                val typeStr = doc.getString("type") ?: RuleType.APP_LIMIT.name
                val targetAppPackage = doc.getString("targetAppPackage")
                val thresholdMinutes = doc.getLong("thresholdMinutes")?.toInt() ?: 0
                val penaltyXp = doc.getLong("penaltyXp")?.toInt() ?: 10
                val active = doc.getBoolean("active") ?: true
                val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()

                val ruleType = try {
                    RuleType.valueOf(typeStr)
                } catch (e: Exception) {
                    RuleType.APP_LIMIT
                }

                val rule = FrictionRule(
                    id = ruleId,
                    name = name,
                    type = ruleType,
                    targetAppPackage = targetAppPackage,
                    thresholdMinutes = thresholdMinutes,
                    penaltyXp = penaltyXp,
                    active = active,
                    createdAt = createdAt
                )
                dao.insertRule(rule)
            }
            Log.d(tag, "Successfully synced rules from Firestore cloud.")
        } catch (e: Exception) {
            Log.w(tag, "Firestore rules sync skipped or restricted: ${e.message}")
        }
    }

    private suspend fun prepopulateInitialData() {
        // Remove default rules if they exist to keep the engine pristine
        val existingRules = dao.getAllRulesStatic()
        for (rule in existingRules) {
            if (rule.id in listOf("r1", "r2", "r3")) {
                dao.deleteRule(rule.id)
            }
        }
        
        // No default challenges or rules are pre-populated.
        // Every new user starts with an empty Friction Engine and creates their own challenges and rules.
        val existingRewards = dao.getAllRewardsStatic()
        if (existingRewards.isEmpty()) {
            val rewards = listOf(
                FrictionReward("rw1", "Ultimate Zen Mode Theme", 150, false, "Unlock the premium soothing lavender and gold deep relaxation skin."),
                FrictionReward("rw2", "Streak Shield Card", 300, false, "Protects your streak for 1 day if you miss a goal during digital detoxes."),
                FrictionReward("rw3", "Custom Vibration Anchor", 100, false, "Unlock physical sensory vibrating alert loops when over-scrolling.")
            )
            dao.insertRewards(rewards)
        }
    }

    suspend fun saveAppClassification(pkg: String, name: String, classification: String) {
        val entry = com.example.data.model.AppClassification(pkg, name, classification)
        dao.insertAppClassification(entry)
        val uid = currentUserUid
        val db = firestoreService.db
        if (uid != null && db != null) {
            db.collection("users").document(uid)
                .collection("appClassifications").document(pkg.replace("/", "_"))
                .set(mapOf(
                    "packageName" to pkg,
                    "appName" to name,
                    "classification" to classification,
                    "updatedAt" to System.currentTimeMillis()
                ), SetOptions.merge())
        }
    }
}
