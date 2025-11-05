package com.project.businesscardscannerapp.RoomDB.DAO

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.project.businesscardscannerapp.RoomDB.Entity.GoalCompletion

@Dao
interface GoalDao {
    @Insert
    suspend fun insert(goal: GoalCompletion)

    @Query("""
        SELECT COUNT(DISTINCT date(completedAt/1000, 'unixepoch'))
        FROM goal_completions
        WHERE completedAt > :since
    """)
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