// FileName: MultipleFiles/CardViewModel.kt
package com.raghu.businesscardscanner2.ViewModel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.raghu.businesscardscanner2.AppUI.autoSaveToContacts
import com.raghu.businesscardscanner2.RoomDB.DataBase.AppDatabase
import com.raghu.businesscardscanner2.RoomDB.Entity.BusinessCard
import com.raghu.businesscardscanner2.RoomDB.Entity.Folder
import com.raghu.businesscardscanner2.RoomDB.ProvideDB.BusinessCardRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


class BusinessCardViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: BusinessCardRepository
    val allCards: Flow<List<BusinessCard>>
    val allFolders: Flow<List<Folder>>
    val favoriteCards: Flow<List<BusinessCard>>

    private var _favoritesFolderId: Int? = null
    private val _favoritesFolderIdState = MutableStateFlow<Int?>(null)
    val favoritesFolderIdState: StateFlow<Int?> = _favoritesFolderIdState.asStateFlow()

    private var favoritesFolderId: Int? = null

    init {
        val dao = AppDatabase.getDatabase(application).businessCardDao()
        repository = BusinessCardRepository(dao)

        allCards = repository.allCards
        allFolders = repository.allFolders
        favoriteCards = repository.favoriteCards

        viewModelScope.launch {
            val folders = repository.allFolders.first()
            var folder = folders.find { it.name == "Favorites" }

            if (folder == null) {
                val folderId = repository.insertFolder(Folder(name = "Favorites"))
                _favoritesFolderId = folderId.toInt()
                _favoritesFolderIdState.value = folderId.toInt()
            } else {
                _favoritesFolderId = folder.id
                _favoritesFolderIdState.value = folder.id
            }
        }
    }

    enum class SortOption {
        RECENTLY_ADDED, RECENTLY_VIEWED, NAME, COMPANY
    }

    // Sorting state
    // Sorting state
    private val _sortOption = MutableStateFlow(SortOption.RECENTLY_ADDED)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    // Sorted cards flow that reacts to sort option changes
    val sortedCards: Flow<List<BusinessCard>> = _sortOption.flatMapLatest { option ->
        when (option) {
            SortOption.RECENTLY_ADDED -> repository.allCardsByRecent
            SortOption.RECENTLY_VIEWED -> repository.allCardsByLastViewed
            SortOption.NAME -> repository.allCardsByName
            SortOption.COMPANY -> repository.allCardsByCompany
        }
    }

    // Function to change sort option
    fun setSortOption(option: SortOption) {
        _sortOption.value = option
    }

    // Function to update last viewed time
    suspend fun updateLastViewed(cardId: Int) {
        repository.updateLastViewed(cardId)
    }

    fun insert(card: BusinessCard) = viewModelScope.launch {
        val context = getApplication<Application>().applicationContext
        autoSaveToContacts(context, card)
        // This will now automatically score the card before inserting
        repository.insert(card)
    }

    // Modify update function to ensure scoring
    fun update(card: BusinessCard) = viewModelScope.launch {
        // This will now automatically score the card before updating
        repository.update(card)
    }

    // Add this function to manually trigger scoring if needed
    fun rescoreAllCards() = viewModelScope.launch {
        repository.scoreAndUpdateAllLeads()
    }

    fun delete(card: BusinessCard) = viewModelScope.launch {
        repository.delete(card)
    }

    fun toggleFavorite(cardId: Int, isFavorite: Boolean) = viewModelScope.launch {
        repository.toggleFavorite(cardId, isFavorite)

        favoritesFolderId?.let { folderId ->
            if (isFavorite) {
                repository.addCardToFolder(cardId, folderId)
            } else {
                repository.removeCardFromFolder(cardId, folderId)
            }
        }
    }

    fun getCardByIdFlow(cardId: Int): Flow<BusinessCard?> {
        return repository.getCardByIdFlow(cardId)
    }

    fun createFolder(name: String) = viewModelScope.launch {
        repository.insertFolder(Folder(name = name))
    }

    fun deleteFolder(folder: Folder) = viewModelScope.launch {
        repository.deleteFolder(folder)
    }

    fun addCardsToFolder(cardIds: List<Int>, folderId: Int) = viewModelScope.launch {
        cardIds.forEach { cardId ->
            repository.addCardToFolder(cardId, folderId)
        }
    }

    fun removeCardsFromFolder(cardIds: List<Int>, folderId: Int) = viewModelScope.launch {
        cardIds.forEach { cardId ->
            repository.removeCardFromFolder(cardId, folderId)
        }
    }

    fun getCardsInFolder(folderId: Int): Flow<List<BusinessCard>> {
        return repository.getCardsInFolder(folderId)
    }

    fun getFolderById(folderId: Int): Flow<Folder?> {
        return repository.getFolderById(folderId)
    }

    // Optional: for suspending getCardById()
    fun getCardById(id: Int): BusinessCard? {
        var card: BusinessCard? = null
        viewModelScope.launch {
            card = repository.getCardById(id)
        }
        return card
    }

    // Add these functions to your BusinessCardViewModel class

    fun toggleFavoriteStatus(cardId: Int, isFavorite: Boolean) {
        viewModelScope.launch {
            repository.updateFavoriteStatus(listOf(cardId), isFavorite)
        }
    }

    fun removeCardsFromFavorites(cardIds: List<Int>) {
        viewModelScope.launch {
            repository.updateFavoriteStatus(cardIds, false)
        }
    }

    private val _selectedFolders = MutableStateFlow(setOf<Int>())
    val selectedFolders: StateFlow<Set<Int>> = _selectedFolders.asStateFlow()

    fun toggleFolderSelection(folder: Folder) {
        if (folder.id != favoritesFolderId) {
            val current = _selectedFolders.value.toMutableSet()
            if (current.contains(folder.id)) {
                current.remove(folder.id)
            } else {
                current.add(folder.id)
            }
            _selectedFolders.value = current
        }
    }

    fun clearFolderSelection() {
        _selectedFolders.value = emptySet()
    }

    fun deleteSelectedFolders() {
        viewModelScope.launch {
            val folders = repository.allFolders.first() // suspend function
            val foldersToDelete = _selectedFolders.value.mapNotNull { folderId ->
                folders.find { it.id == folderId }
            }
            foldersToDelete.forEach { folder ->
                repository.deleteFolderWithRelationships(folder)
            }
            _selectedFolders.value = emptySet()
        }
    }

    private val _cardImages = MutableStateFlow<Map<Int, Bitmap>>(emptyMap())
    val cardImages: StateFlow<Map<Int, Bitmap>> = _cardImages

    fun loadCardImage(cardId: Int, imagePath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val bitmap = BitmapFactory.decodeFile(imagePath)
            _cardImages.update { it + (cardId to bitmap) }
        }
    }


    suspend fun getCardByIdSuspend(id: Int): BusinessCard? {
        return repository.getCardById(id)
    }


    fun deleteCardById(cardId: Int) {
        viewModelScope.launch {
            repository.deleteCardById(cardId)
        }
    }

    fun updateLastViewedSafe(cardId: Int) {
        viewModelScope.launch {
            repository.updateLastViewed(cardId)
        }
    }

    // New functions for reminder
    fun setReminder(cardId: Int, reminderTime: Long, reminderMessage: String) = viewModelScope.launch {
        val card = repository.getCardById(cardId)?.copy(
            reminderTime = reminderTime,
            reminderMessage = reminderMessage
        )
        card?.let { repository.update(it) }
    }

    fun clearReminder(cardId: Int) = viewModelScope.launch {
        repository.clearReminder(cardId)
    }

    fun getCardsWithReminders(currentTime: Long): Flow<List<BusinessCard>> {
        return repository.getCardsWithReminders(currentTime)
    }
}
