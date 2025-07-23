// FileName: MultipleFiles/ProvideDB.kt
package com.raghu.businesscardscanner2.RoomDB.ProvideDB

import androidx.compose.ui.platform.LocalContext
import androidx.room.Room
import com.raghu.businesscardscanner2.LeadScoreDB.LeadScorer
import com.raghu.businesscardscanner2.RoomDB.DAO.BusinessCardDao
import com.raghu.businesscardscanner2.RoomDB.DataBase.AppDatabase
import com.raghu.businesscardscanner2.RoomDB.Entity.BusinessCard
import com.raghu.businesscardscanner2.RoomDB.Entity.CardFolderCrossRef
import com.raghu.businesscardscanner2.RoomDB.Entity.Folder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map


class BusinessCardRepository(private val businessCardDao: BusinessCardDao) {
    val allCards: Flow<List<BusinessCard>> = businessCardDao.getAllCards()

    val allCardsByRecent: Flow<List<BusinessCard>> = businessCardDao.getAllCardsByRecent()
    val allCardsByLastViewed: Flow<List<BusinessCard>> = businessCardDao.getAllCardsByLastViewed()
    val allCardsByName: Flow<List<BusinessCard>> = businessCardDao.getAllCardsByName()
    val allCardsByCompany: Flow<List<BusinessCard>> = businessCardDao.getAllCardsByCompany()

    suspend fun updateLastViewed(cardId: Int) {
        businessCardDao.updateLastViewed(cardId)
    }

    suspend fun insert(card: BusinessCard) {
        businessCardDao.insert(card)
    }

    suspend fun update(card: BusinessCard) {
        businessCardDao.update(card)
    }

    suspend fun delete(card: BusinessCard) {
        businessCardDao.delete(card)
    }

    suspend fun getCardById(id: Int): BusinessCard? {
        return businessCardDao.getCardById(id)
    }

    // In your Repository
    fun getCardByIdFlow(cardId: Int): Flow<BusinessCard?> {
        return businessCardDao.getCardByIdFlow(cardId)
    }

    val allFolders: Flow<List<Folder>> = businessCardDao.getAllFolders()

    suspend fun insertFolder(folder: Folder): Long = businessCardDao.insertFolder(folder)

    suspend fun deleteFolder(folder: Folder) = businessCardDao.deleteFolder(folder)

    // Card-Folder relationship
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
        // First delete all card-folder relationships
        businessCardDao.deleteAllCardFolderRelationships(folder.id)
        // Then delete the folder itself
        businessCardDao.deleteFolder(folder)
    }


    suspend fun deleteCardById(cardId: Int) {
        getCardById(cardId)?.let { delete(it) }
    }

    // New methods for reminders
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
        val cards = businessCardDao.getAllCards().first() // Get first emission
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
}
