package com.project.businesscardscannerapp.RoomDB.DAO

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.project.businesscardscannerapp.RoomDB.Entity.FollowUp
import com.project.businesscardscannerapp.RoomDB.Entity.GoalCompletion
import kotlinx.coroutines.flow.Flow

// Data classes for query results
data class BucketCount(
    val leadBucket: String,
    val cnt: Int
)

data class NameCount(
    val name: String, // This will be 'industry' or 'company domain'
    val cnt: Int
)

data class BestHour(
    val hour: String, // Hour as string (e.g., "09")
    val cnt: Int
)

@Dao
interface InsightsDao {
    @Query("SELECT COUNT(*) FROM business_cards WHERE createdAt BETWEEN :start AND :end")
    suspend fun countCards(start: Long, end: Long): Int

    @Query("""
        SELECT leadCategory as leadBucket, COUNT(*) as cnt
        FROM business_cards GROUP BY leadCategory
    """)
    suspend fun getLeadBucketCounts(): List<BucketCount>

    @Query("""
        SELECT industry as name, COUNT(*) as cnt
        FROM business_cards WHERE industry IS NOT NULL AND industry != 'Unknown' GROUP BY industry
        ORDER BY cnt DESC LIMIT 5
    """)
    suspend fun getTopIndustries(): List<NameCount>

    // You might need to extract domain from email for company domain insights
    // This is a simplified example, a real implementation would parse email domains
    @Query("""
        SELECT SUBSTR(email, INSTR(email, '@') + 1) as name, COUNT(*) as cnt
        FROM business_cards WHERE email IS NOT NULL AND email LIKE '%@%'
        GROUP BY name ORDER BY cnt DESC LIMIT 5
    """)
    suspend fun getTopCompanyDomains(): List<NameCount>

    @Query("""
        SELECT STRFTIME('%H', completedAt / 1000, 'unixepoch') as hour, COUNT(*) as cnt
        FROM follow_ups
        WHERE status='done' AND completedAt IS NOT NULL
        GROUP BY hour ORDER BY cnt DESC LIMIT 1
    """)
    suspend fun getBestHour(): BestHour?

    @Query("SELECT COUNT(*) FROM follow_ups WHERE status='scheduled' AND scheduledAt < :now")
    suspend fun getOverdueFollowUps(now: Long): Int

    @Query("SELECT * FROM follow_ups WHERE cardId = :cardId ORDER BY scheduledAt DESC")
    fun getFollowUpsForCard(cardId: Int): Flow<List<FollowUp>>

    // Add to your BusinessCardDao interface
    @Insert
    suspend fun insertGoalCompletion(goalCompletion: GoalCompletion)

    @Query("SELECT COUNT(DISTINCT date(completedAt/1000, 'unixepoch')) FROM goal_completions WHERE completedAt > :since")
    suspend fun streakSince(since: Long): Int

    @Query("""
    SELECT strftime('%w', datetime(completedAt/1000, 'unixepoch')) as bestDay
    FROM goal_completions
    GROUP BY bestDay ORDER BY COUNT(*) DESC LIMIT 1
""")
    suspend fun bestDay(): String?

    @Query("""
    SELECT strftime('%H', datetime(completedAt/1000, 'unixepoch')) as bestHour
    FROM goal_completions
    GROUP BY bestHour ORDER BY COUNT(*) DESC LIMIT 1
""")
    suspend fun bestHour(): String?
}

