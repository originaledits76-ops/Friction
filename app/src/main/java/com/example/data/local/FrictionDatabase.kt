package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.data.model.Challenge
import com.example.data.model.DailyUsageCache
import com.example.data.model.FrictionPenalty
import com.example.data.model.FrictionReward
import com.example.data.model.FrictionRule
import com.example.data.model.FrictionTask
import com.example.data.model.RuleType

class FrictionConverters {
    @TypeConverter
    fun fromRuleType(value: RuleType?): String {
        return value?.name ?: RuleType.APP_LIMIT.name
    }

    @TypeConverter
    fun toRuleType(value: String?): RuleType {
        if (value.isNullOrBlank()) return RuleType.APP_LIMIT
        return try {
            RuleType.valueOf(value)
        } catch (e: Exception) {
            RuleType.APP_LIMIT
        }
    }
}

@Database(
    entities = [
        FrictionRule::class,
        FrictionTask::class,
        FrictionReward::class,
        FrictionPenalty::class,
        Challenge::class,
        DailyUsageCache::class,
        com.example.data.model.AppClassification::class,
        com.example.data.model.ChallengeHistoryEntity::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(FrictionConverters::class)
abstract class FrictionDatabase : RoomDatabase() {

    abstract fun frictionDao(): FrictionDao

    companion object {
        @Volatile
        private var INSTANCE: FrictionDatabase? = null

        fun getDatabase(context: Context): FrictionDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FrictionDatabase::class.java,
                    "friction_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
