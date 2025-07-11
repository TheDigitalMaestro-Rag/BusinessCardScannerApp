package com.raghu.businesscardscanner2.RoomDB.ProvideDB

import androidx.compose.ui.platform.LocalContext
import androidx.room.Room
import com.raghu.businesscardscanner2.RoomDB.DAO.BusinessCardDao
import com.raghu.businesscardscanner2.RoomDB.DataBase.AppDatabase
import com.raghu.businesscardscanner2.RoomDB.Entity.BusinessCard
import com.raghu.businesscardscanner2.RoomDB.Entity.CardFolderCrossRef
import com.raghu.businesscardscanner2.RoomDB.Entity.Folder
import kotlinx.coroutines.flow.Flow


class BusinessCardRepository(private val businessCardDao: BusinessCardDao) {
    val allCards: Flow<List<BusinessCard>> = businessCardDao.getAllCards()

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

}