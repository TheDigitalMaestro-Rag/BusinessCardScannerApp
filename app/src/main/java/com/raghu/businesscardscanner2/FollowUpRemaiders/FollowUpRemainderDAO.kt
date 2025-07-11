package com.raghu.businesscardscanner2.FollowUpRemaiders

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// --- DAO: FollowUpReminderDao.kt ---
@Dao
interface FollowUpReminderDao {
    @Query("SELECT * FROM follow_up_reminders WHERE isCompleted = 0 ORDER BY dueDate ASC")
    fun getPendingReminders(): Flow<List<FollowUpReminderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: FollowUpReminderEntity)

    @Update
    suspend fun updateReminder(reminder: FollowUpReminderEntity)

    @Query("SELECT * FROM follow_up_reminders WHERE id = :reminderId")
    suspend fun getReminderById(reminderId: Int): FollowUpReminderEntity

    @Query("UPDATE follow_up_reminders SET isCompleted = 1 WHERE id = :reminderId")
    suspend fun markReminderCompleted(reminderId: Int)

    @Query("SELECT * FROM follow_up_reminders WHERE id = :reminderId")
    fun getReminderByIdFlow(reminderId: Int): Flow<FollowUpReminderEntity>
}