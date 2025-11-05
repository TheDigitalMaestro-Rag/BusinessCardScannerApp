package com.project.businesscardscannerapp.RoomDB.ProvideDB

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.project.businesscardscannerapp.CompanyEnrichment.TagSuggester.suggestTags
import com.project.businesscardscannerapp.LeadScoreDB.LeadScorer
import com.project.businesscardscannerapp.RoomDB.DAO.BestHour
import com.project.businesscardscannerapp.RoomDB.DAO.BucketCount
import com.project.businesscardscannerapp.RoomDB.DAO.BusinessCardDao
import com.project.businesscardscannerapp.RoomDB.DAO.InsightsDao
import com.project.businesscardscannerapp.RoomDB.DAO.NameCount
import com.project.businesscardscannerapp.RoomDB.Entity.BusinessCard
import com.project.businesscardscannerapp.RoomDB.Entity.CardFolderCrossRef
import com.project.businesscardscannerapp.RoomDB.Entity.Folder
import com.project.businesscardscannerapp.RoomDB.Entity.FollowUp
import com.project.businesscardscannerapp.RoomDB.Entity.BanditArm
import com.project.businesscardscannerapp.RoomDB.Entity.GoalCompletion
import com.project.businesscardscannerapp.RoomDB.Entity.PipelineStage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class BusinessCardRepository(
    val businessCardDao: BusinessCardDao,
    private val insightsDao: InsightsDao
) {

    // Add these properties for backup functionality
    private var backupEnabled: Boolean = false
    private var lastBackupTimestamp: Long = 0L

    // ... existing methods ...

    // Complete the missing backup-related methods
    suspend fun setBackupEnabled(enabled: Boolean) {
        backupEnabled = enabled
        // You might want to persist this setting to SharedPreferences or a database table
    }

    suspend fun updateLastBackupTimestamp(timestamp: Long) {
        lastBackupTimestamp = timestamp
        // You might want to persist this setting to SharedPreferences or a database table
    }

    suspend fun getLastBackupTimestamp(): Long {
        return lastBackupTimestamp
        // You might want to retrieve this from SharedPreferences or a database table
    }

    suspend fun isBackupEnabled(): Boolean {
        return backupEnabled
        // You might want to retrieve this from SharedPreferences or a database table
    }

    suspend fun clearAllData() {
        // Clear all data before restoration
        // Note: This is a destructive operation - use with caution!
        val folders = businessCardDao.getAllFolders().first()
        folders.forEach { folder ->
            businessCardDao.deleteAllCardFolderRelationships(folder.id)
            businessCardDao.deleteFolder(folder)
        }

        val cards = businessCardDao.getAllCards().first()
        cards.forEach { card ->
            businessCardDao.delete(card)
        }

        // Clear follow-ups, bandit arms, and goal completions if they exist
        // You might need to add DAO methods for these if they don't exist
    }

//    suspend fun getAllFollowUps(): Flow<List<FollowUp>> {
//        // Since there's no direct method in DAO, we'll need to create one
//        // For now, return an empty flow or implement a proper method in DAO
//        return businessCardDao.getAllCards().map { emptyList() }
//        // You should add this method to your BusinessCardDao:
//        // @Query("SELECT * FROM follow_ups ORDER BY scheduledAt DESC")
//        // fun getAllFollowUps(): Flow<List<FollowUp>>
//    }

    val allCardsByRecent: Flow<List<BusinessCard>> = businessCardDao.getAllCardsByRecent()
    val allCardsByLastViewed: Flow<List<BusinessCard>> = businessCardDao.getAllCardsByLastViewed()
    val allCardsByName: Flow<List<BusinessCard>> = businessCardDao.getAllCardsByName()
    val allCardsByCompany: Flow<List<BusinessCard>> = businessCardDao.getAllCardsByCompany()

    val allCards: Flow<List<BusinessCard>> = businessCardDao.getAllCards()
        .map { cards -> cards.map { LeadScorer.scoreLead(it) } }


    suspend fun update(card: BusinessCard) {
        val scoredCard = LeadScorer.scoreLead(card)
        val taggedCard = scoredCard.copy(tags = suggestTags(scoredCard))
        businessCardDao.update(taggedCard)
    }

    suspend fun updateLastViewed(cardId: Int) {
        businessCardDao.updateLastViewed(cardId)
    }

    suspend fun delete(card: BusinessCard) {
        businessCardDao.delete(card)
    }

    suspend fun getCardById(id: Int): BusinessCard? {
        return businessCardDao.getCardById(id)
    }

    fun getCardByIdFlow(cardId: Int): Flow<BusinessCard?> {
        return businessCardDao.getCardByIdFlow(cardId)
    }

    val allFolders: Flow<List<Folder>> = businessCardDao.getAllFolders()

    suspend fun insertFolder(folder: Folder): Long = businessCardDao.insertFolder(folder)

    suspend fun deleteFolder(folder: Folder) = businessCardDao.deleteFolder(folder)

    suspend fun addCardToFolder(cardId: Int, folderId: Int) {
        businessCardDao.insertCardFolderCrossRef(CardFolderCrossRef(cardId, folderId))
    }

    suspend fun removeCardFromFolder(cardId: Int, folderId: Int) {
        businessCardDao.removeCardFromFolder(cardId, folderId)
    }

    fun getCardsInFolder(folderId: Int): Flow<List<BusinessCard>> {
        return businessCardDao.getCardsInFolder(folderId)
    }

    fun getFolderById(folderId: Int): Flow<Folder?> {
        return businessCardDao.getFolderById(folderId)
    }

    suspend fun toggleFavorite(cardId: Int, isFavorite: Boolean) {
        businessCardDao.updateFavoriteStatus(cardId, isFavorite)
    }

    val favoriteCards: Flow<List<BusinessCard>> = businessCardDao.getFavoriteCards()

    suspend fun updateFavoriteStatus(cardIds: List<Int>, isFavorite: Boolean) {
        businessCardDao.updateFavoriteStatus(cardIds, isFavorite)
    }

    suspend fun deleteFolderWithRelationships(folder: Folder) {
        businessCardDao.deleteAllCardFolderRelationships(folder.id)
        businessCardDao.deleteFolder(folder)
    }

    suspend fun deleteCardById(cardId: Int) {
        getCardById(cardId)?.let { delete(it) }
    }

    fun getCardsWithReminders(currentTime: Long): Flow<List<BusinessCard>> {
        return businessCardDao.getCardsWithReminders(currentTime)
    }

    suspend fun clearReminder(cardId: Int) {
        businessCardDao.clearReminder(cardId)
    }

    suspend fun scoreAndUpdateLead(card: BusinessCard): BusinessCard {
        val scoredCard = LeadScorer.scoreLead(card)
        businessCardDao.update(scoredCard)
        return scoredCard
    }

    suspend fun scoreAndUpdateAllLeads() {
        val cards = businessCardDao.getAllCards().first()
        cards.forEach { card ->
            val scoredCard = LeadScorer.scoreLead(card)
            businessCardDao.update(scoredCard)
        }
    }

    fun getCardsByLeadCategory(category: String): Flow<List<BusinessCard>> {
        return businessCardDao.getAllCards().map { cards ->
            cards.filter { it.leadCategory == category }
        }
    }

    fun getCardsSortedByLeadScore(): Flow<List<BusinessCard>> {
        return businessCardDao.getAllCards().map { cards ->
            cards.sortedByDescending { it.leadScore }
        }
    }

    suspend fun insertFollowUp(followUp: FollowUp) = businessCardDao.insertFollowUp(followUp)
    suspend fun updateFollowUp(followUp: FollowUp) = businessCardDao.updateFollowUp(followUp)
    suspend fun getLastFollowUpForCard(cardId: Int): FollowUp? = businessCardDao.getLastFollowUpForCard(cardId)

    // Ensure this method is correctly defined
    fun getOverdueFollowUps(now: Long): Flow<List<FollowUp>> = businessCardDao.getOverdueFollowUps(now)

    suspend fun insertBanditArm(banditArm: BanditArm) = businessCardDao.insertBanditArm(banditArm)
    suspend fun updateBanditArm(banditArm: BanditArm) = businessCardDao.updateBanditArm(banditArm)
    suspend fun getAllBanditArms(): List<BanditArm> = businessCardDao.getAllBanditArms()
    suspend fun getBanditArm(armId: String): BanditArm? = businessCardDao.getBanditArm(armId)

    // New methods for Insights
    suspend fun countCards(start: Long, end: Long): Int = insightsDao.countCards(start, end)
    suspend fun getLeadBucketCounts(): List<BucketCount> = insightsDao.getLeadBucketCounts()
    suspend fun getTopIndustries(): List<NameCount> = insightsDao.getTopIndustries()
    suspend fun getTopCompanyDomains(): List<NameCount> = insightsDao.getTopCompanyDomains()
    suspend fun getBestHour(): BestHour? = insightsDao.getBestHour()
    suspend fun getOverdueFollowUpsCount(now: Long): Int = insightsDao.getOverdueFollowUps(now)
    fun getFollowUpsForCard(cardId: Int): Flow<List<FollowUp>> = insightsDao.getFollowUpsForCard(cardId)

    // Add to ProvideDB.kt in BusinessCardRepository class
    suspend fun recordGoalCompletion(cardId: Int) {
        val goalCompletion = GoalCompletion(cardId = cardId, completedAt = System.currentTimeMillis())
        businessCardDao.insertGoalCompletion(goalCompletion)
    }

    suspend fun getCurrentStreak(): Int {
        val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
        return businessCardDao.streakSince(sevenDaysAgo)
    }

    // Fix getAllFollowUps method
    fun getAllFollowUps(): Flow<List<FollowUp>> {
        return businessCardDao.getAllFollowUps()
    }

    // Add these methods to BusinessCardRepository
//    suspend fun getAllBanditArms(): List<BanditArm> {
//        return businessCardDao.getAllBanditArms().first()
//    }

    suspend fun getAllGoalCompletions(): List<GoalCompletion> {
        return businessCardDao.getAllGoalCompletions().first()
    }

    suspend fun clearAllFollowUps() {
        businessCardDao.deleteAllFollowUps()
    }

    suspend fun clearAllBanditArms() {
        businessCardDao.deleteAllBanditArms()
    }

    suspend fun clearAllGoalCompletions() {
        businessCardDao.deleteAllGoalCompletions()
    }

    suspend fun clearAllCardFolderRelationships() {
        businessCardDao.deleteAllCardFolderRelationships()
    }

    suspend fun updatePipelineStage(cardId: Int, stage: PipelineStage) {
        businessCardDao.updatePipelineStage(cardId, stage)
    }

    fun getCardsByPipelineStage(stage: PipelineStage): Flow<List<BusinessCard>>{
        return businessCardDao.getCardsByPipelineStage(stage)
    }

    suspend fun getPipelineDistribution(): Map<PipelineStage, Int> {
        val stageCounts = businessCardDao.getPipelineDistribution()
        return stageCounts.associate { it.pipelineStage to it.count }
    }

    fun getCardsForPipelineView(): Flow<List<BusinessCard>> {
        return businessCardDao.getCardsForPipelineView()
    }

    suspend fun insert(card: BusinessCard){
        val scoredCard = LeadScorer.scoreLead(card)
        val initialStage = when(scoredCard.leadScore){
            in 80..10 -> PipelineStage.NEW
            in 60..79 -> PipelineStage.CONTACTED
            else -> PipelineStage.NEW
        }

        val taggedCard = scoredCard.copy(tags = suggestTags(scoredCard), pipelineStage = initialStage)
        businessCardDao.insert(taggedCard)
    }
}
