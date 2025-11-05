// FileName: MultipleFiles/DAO.kt
package com.project.businesscardscannerapp.RoomDB.DAO


import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.RoomDatabase
import androidx.room.Update
import com.project.businesscardscannerapp.RoomDB.Entity.BanditArm
import com.project.businesscardscannerapp.RoomDB.Entity.BusinessCard
import com.project.businesscardscannerapp.RoomDB.Entity.CardFolderCrossRef
import com.project.businesscardscannerapp.RoomDB.Entity.Folder
import com.project.businesscardscannerapp.RoomDB.Entity.FollowUp
import com.project.businesscardscannerapp.RoomDB.Entity.GoalCompletion
import com.project.businesscardscannerapp.RoomDB.Entity.PipelineStage
import kotlinx.coroutines.flow.Flow

@Dao
interface BusinessCardDao {
    @Insert
    suspend fun insert(card: BusinessCard)

    @Update
    suspend fun update(card: BusinessCard)

    @Delete
    suspend fun delete(card: BusinessCard)

    @Query("SELECT * FROM business_cards ORDER BY createdAt DESC")
    fun getAllCards(): Flow<List<BusinessCard>>

    @Query("SELECT * FROM business_cards WHERE id = :id")
    suspend fun getCardById(id: Int): BusinessCard?

    // In your DAO
    @Query("SELECT * FROM business_cards WHERE id = :cardId")
    fun getCardByIdFlow(cardId: Int): Flow<BusinessCard?>


    // Folder operations

    // BusinessCard-Folder relationship operations
    @Insert
    suspend fun insertFolder(folder: Folder): Long

    @Query("SELECT * FROM folders ORDER BY name ASC")
    fun getAllFolders(): Flow<List<Folder>>


    @Query("SELECT * FROM business_cards ORDER BY createdAt DESC")
    fun getAllCardsByRecent(): Flow<List<BusinessCard>>

    @Query("SELECT * FROM business_cards ORDER BY lastViewedAt DESC")
    fun getAllCardsByLastViewed(): Flow<List<BusinessCard>>

    @Query("SELECT * FROM business_cards ORDER BY name COLLATE NOCASE ASC")
    fun getAllCardsByName(): Flow<List<BusinessCard>>

    @Query("SELECT * FROM business_cards ORDER BY company COLLATE NOCASE ASC")
    fun getAllCardsByCompany(): Flow<List<BusinessCard>>

    @Query("UPDATE business_cards SET lastViewedAt = :timestamp WHERE id = :cardId")
    suspend fun updateLastViewed(cardId: Int, timestamp: Long = System.currentTimeMillis())

    // Card-Folder relationship operations
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCardFolderCrossRef(crossRef: CardFolderCrossRef)

    @Query("DELETE FROM cardfoldercrossref WHERE cardId = :cardId AND folderId = :folderId")
    suspend fun removeCardFromFolder(cardId: Int, folderId: Int)

    @RewriteQueriesToDropUnusedColumns
    @Query("""
        SELECT business_cards.*
        FROM business_cards
        INNER JOIN cardfoldercrossref
        ON business_cards.id = cardfoldercrossref.cardId
        WHERE cardfoldercrossref.folderId = :folderId
        ORDER BY business_cards.name ASC
    """)
    fun getCardsInFolder(folderId: Int): Flow<List<BusinessCard>>


    @Query("UPDATE business_cards SET isFavorite = :isFavorite WHERE id = :cardId")
    suspend fun updateFavoriteStatus(cardId: Int, isFavorite: Boolean)


    @Query("SELECT * FROM business_cards WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoriteCards(): Flow<List<BusinessCard>>


    @Query("SELECT * FROM folders WHERE id = :folderId")
    fun getFolderById(folderId: Int): Flow<Folder?>

    @Query("UPDATE business_cards SET isFavorite = :isFavorite WHERE id IN (:cardIds)")
    suspend fun updateFavoriteStatus(cardIds: List<Int>, isFavorite: Boolean)


    @Query("DELETE FROM cardfoldercrossref WHERE folderId = :folderId")
    suspend fun deleteAllCardFolderRelationships(folderId: Int)

    @Delete
    suspend fun deleteFolder(folder: Folder)

    // New query to get cards with active reminders
    @Query("SELECT * FROM business_cards WHERE reminderTime IS NOT NULL AND reminderTime > :currentTime ORDER BY reminderTime ASC")
    fun getCardsWithReminders(currentTime: Long): Flow<List<BusinessCard>>

    // New query to clear a reminder
    @Query("UPDATE business_cards SET reminderTime = NULL, reminderMessage = NULL WHERE id = :cardId")
    suspend fun clearReminder(cardId: Int)

    @Query("UPDATE business_cards SET leadScore = :score, leadCategory = :category WHERE id = :cardId")
    suspend fun updateLeadScore(cardId: Int, score: Int, category: String)

    @Query("SELECT * FROM business_cards ORDER BY leadScore DESC")
    fun getAllCardsByLeadScore(): Flow<List<BusinessCard>>

    @Query("SELECT * FROM business_cards WHERE leadCategory = :category ORDER BY leadScore DESC")
    fun getCardsByLeadCategory(category: String): Flow<List<BusinessCard>>

    // New DAO methods for FollowUp and BanditArm
    @Insert
    suspend fun insertFollowUp(followUp: FollowUp)

    @Update
    suspend fun updateFollowUp(followUp: FollowUp)

    @Query("SELECT * FROM follow_ups WHERE cardId = :cardId ORDER BY scheduledAt DESC LIMIT 1")
    suspend fun getLastFollowUpForCard(cardId: Int): FollowUp?

    @Query("SELECT * FROM follow_ups WHERE status = 'scheduled' AND scheduledAt < :now")
    fun getOverdueFollowUps(now: Long): Flow<List<FollowUp>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBanditArm(banditArm: BanditArm)

    @Update
    suspend fun updateBanditArm(banditArm: BanditArm)

    @Query("SELECT * FROM bandit_arms")
    suspend fun getAllBanditArms(): List<BanditArm>

    @Query("SELECT * FROM bandit_arms WHERE armId = :armId")
    suspend fun getBanditArm(armId: String): BanditArm?

    // In your BusinessCardDao interface, add:
    @Insert
    suspend fun insertGoalCompletion(goalCompletion: GoalCompletion)

    @Query("SELECT COUNT(DISTINCT date(completedAt/1000, 'unixepoch')) FROM goal_completions WHERE completedAt > :since")
    suspend fun streakSince(since: Long): Int


    // Add to BusinessCardDao.kt
    @Query("SELECT * FROM follow_ups ORDER BY scheduledAt DESC")
    fun getAllFollowUps(): Flow<List<FollowUp>>

    @Query("SELECT * FROM goal_completions")
    fun getAllGoalCompletions(): Flow<List<GoalCompletion>>

    @Query("DELETE FROM follow_ups")
    suspend fun deleteAllFollowUps()

    @Query("DELETE FROM bandit_arms")
    suspend fun deleteAllBanditArms()

    @Query("DELETE FROM goal_completions")
    suspend fun deleteAllGoalCompletions()

    @Query("DELETE FROM cardfoldercrossref")
    suspend fun deleteAllCardFolderRelationships()

    @Query("UPDATE business_cards SET pipelineStage = :stage WHERE id = :cardId")
    suspend fun updatePipelineStage(cardId: Int, stage: PipelineStage)

    @Query("SELECT * FROM business_cards WHERE pipelineStage = :stage ORDER BY createdAt DESC")
    fun getCardsByPipelineStage(stage: PipelineStage): Flow<List<BusinessCard>>

    @Query("SELECT pipelineStage, COUNT(*) as count FROM business_cards GROUP BY pipelineStage")
    suspend fun getPipelineDistribution(): List<PipelineStageCount>

    // Add this data class to your DAO.kt file:
    data class PipelineStageCount(
        val pipelineStage: PipelineStage,
        val count: Int
    )

    @Query("SELECT * FROM business_cards ORDER BY pipelineStage, leadScore DESC")
    fun getCardsForPipelineView(): Flow<List<BusinessCard>>

}
