package com.raghu.businesscardscanner2.RoomDB.DAO

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.RoomDatabase
import androidx.room.Update
import com.raghu.businesscardscanner2.RoomDB.Entity.BusinessCard
import com.raghu.businesscardscanner2.RoomDB.Entity.CardFolderCrossRef
import com.raghu.businesscardscanner2.RoomDB.Entity.Folder
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
}

