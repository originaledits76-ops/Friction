package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.Challenge
import com.example.data.model.DailyUsageCache
import com.example.data.model.FrictionPenalty
import com.example.data.model.FrictionReward
import com.example.data.model.FrictionRule
import com.example.data.model.FrictionTask
import kotlinx.coroutines.flow.Flow

@Dao
interface FrictionDao {

    // Friction Rules
    @Query("SELECT * FROM friction_rules ORDER BY name ASC")
    fun getAllRules(): Flow<List<FrictionRule>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: FrictionRule)

    @Query("DELETE FROM friction_rules WHERE id = :id")
    suspend fun deleteRule(id: String)

    // Friction Tasks
    @Query("SELECT * FROM friction_tasks ORDER BY timestamp DESC")
    fun getAllTasks(): Flow<List<FrictionTask>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: FrictionTask)

    // Friction Rewards
    @Query("SELECT * FROM friction_rewards")
    fun getAllRewards(): Flow<List<FrictionReward>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRewards(rewards: List<FrictionReward>)

    // Friction Penalties
    @Query("SELECT * FROM friction_penalties ORDER BY timestamp DESC")
    fun getAllPenalties(): Flow<List<FrictionPenalty>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPenalty(penalty: FrictionPenalty)

    // Daily Usage Cache
    @Query("SELECT * FROM daily_usage_cache WHERE dateStr = :dateStr")
    suspend fun getDailyUsage(dateStr: String): DailyUsageCache?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyUsage(usage: DailyUsageCache)

    // Challenges
    @Query("SELECT * FROM challenges")
    fun getAllChallenges(): Flow<List<Challenge>>

    @Query("SELECT * FROM challenges")
    suspend fun getAllChallengesStatic(): List<Challenge>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChallenges(challenges: List<Challenge>)

    @Query("SELECT * FROM friction_rules")
    suspend fun getAllRulesStatic(): List<FrictionRule>

    @Query("SELECT * FROM friction_rewards")
    suspend fun getAllRewardsStatic(): List<FrictionReward>

    @Query("SELECT * FROM app_classifications")
    fun getAllAppClassifications(): Flow<List<com.example.data.model.AppClassification>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppClassification(classification: com.example.data.model.AppClassification)

    // Challenge History Cache
    @Query("SELECT * FROM challenge_history WHERE userUid = :userUid ORDER BY timestamp DESC")
    fun getChallengeHistory(userUid: String): Flow<List<com.example.data.model.ChallengeHistoryEntity>>

    @Query("SELECT * FROM challenge_history WHERE userUid = :userUid ORDER BY timestamp DESC")
    suspend fun getChallengeHistoryStatic(userUid: String): List<com.example.data.model.ChallengeHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChallengeHistory(entry: com.example.data.model.ChallengeHistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllChallengeHistory(entries: List<com.example.data.model.ChallengeHistoryEntity>)
}
