// FileName: MultipleFiles/CardViewModel.kt
package com.project.businesscardscannerapp.ViewModel

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.preference.PreferenceManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.project.businesscardscannerapp.AI.FollowUpSuggester
import com.project.businesscardscannerapp.AppUI.autoSaveToContacts
import com.project.businesscardscannerapp.RoomDB.DataBase.AppDatabase
import com.project.businesscardscannerapp.RoomDB.Entity.BusinessCard
import com.project.businesscardscannerapp.RoomDB.Entity.Folder
import com.project.businesscardscannerapp.RoomDB.Entity.FollowUp
import com.project.businesscardscannerapp.RoomDB.ProvideDB.BusinessCardRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.ListenerRegistration
import com.project.businesscardscannerapp.CompanyEnrichment.TagSuggester.suggestTags
import com.project.businesscardscannerapp.RoomDB.ProvideDB.CardSharingRepository
import com.project.businesscardscannerapp.AI.SmartFollowUpPrediction
import com.project.businesscardscannerapp.AI.SmartFollowUpPredictor
import com.project.businesscardscannerapp.Calendar.CalendarIntegrationService
import com.project.businesscardscannerapp.Notification.DailyDigestWorker
import com.project.businesscardscannerapp.Notification.SmartNotificationManager
import com.project.businesscardscannerapp.Notification.StaleLeadsWorker
import com.project.businesscardscannerapp.ReminderBroadcastReceiver
import com.project.businesscardscannerapp.RoomDB.Entity.BanditArm
import com.project.businesscardscannerapp.RoomDB.Entity.GoalCompletion
import com.project.businesscardscannerapp.RoomDB.Entity.PipelineStage
import com.project.businesscardscannerapp.ViewModel.NotificationConstants.REMINDER_CARD_ID
import com.project.businesscardscannerapp.ViewModel.NotificationConstants.REMINDER_MESSAGE
import com.project.businesscardscannerapp.ViewModel.NotificationConstants.REMINDER_NOTIFICATION_ID
import com.project.businesscardscannerapp.ViewModel.NotificationConstants.REMINDER_REQUEST_CODE_BASE
import com.project.businesscardscannerapp.Workers.FollowUpWorker
import com.project.businesscardscannerapp.Workers.SmartFollowUpWorker
import kotlinx.coroutines.withContext
import java.time.ZoneId
import java.util.Calendar
import java.util.concurrent.TimeUnit


object NotificationConstants {
    const val REMINDER_CARD_ID = "reminder_card_id"
    const val REMINDER_MESSAGE = "reminder_message"
    const val REMINDER_NOTIFICATION_ID = "reminder_notification_id"
    const val REMINDER_REQUEST_CODE_BASE = 1000
}

class BusinessCardViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: BusinessCardRepository
    val allCards: Flow<List<BusinessCard>>
    val allFolders: Flow<List<Folder>>
    val favoriteCards: Flow<List<BusinessCard>>

    private var _favoritesFolderId: Int? = null
    private val _favoritesFolderIdState = MutableStateFlow<Int?>(null)
    val favoritesFolderIdState: StateFlow<Int?> = _favoritesFolderIdState.asStateFlow()

    private var favoritesFolderId: Int? = null

    // Calendar and Notification Services
    private val calendarService by lazy { CalendarIntegrationService(application) }
    private val smartNotificationManager by lazy { SmartNotificationManager(application) }
    private val smartFollowUpPredictor by lazy { SmartFollowUpPredictor() }

    init {
        val database = AppDatabase.getDatabase(application)
        repository = BusinessCardRepository(database.businessCardDao(), database.insightsDao())

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

    @RequiresApi(Build.VERSION_CODES.O)
    fun insert(card: BusinessCard) = viewModelScope.launch {
        val context = getApplication<Application>().applicationContext
        autoSaveToContacts(context, card)
        // This will now automatically score the card before inserting
        repository.insert(card)
        // After inserting, suggest and schedule a follow-up
        val insertedCard = repository.getCardById(card.id) // Get the card with its generated ID
        insertedCard?.let {
            suggestAndScheduleFollowUp(it, "first-followup")
        }
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

    // AI Follow-up Reminder functions
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun suggestAndScheduleFollowUp(card: BusinessCard, reason: String) {
        val banditArms = repository.getAllBanditArms()
        val userTz = ZoneId.systemDefault() // Get user's current timezone
        val suggestedTime = FollowUpSuggester.suggestFollowup(card, banditArms, userTz)

        // Save the suggested follow-up to the database
        val followUp = FollowUp(
            cardId = card.id,
            scheduledAt = suggestedTime.toInstant().toEpochMilli(),
            reason = reason,
            suggestedByAi = true,
            status = "scheduled",
            completedAt = null
        )
        repository.insertFollowUp(followUp)

        // Schedule the WorkManager task
        scheduleFollowupWork(card.id, card.name, "Suggested follow-up", suggestedTime.toInstant().toEpochMilli())
    }

    private fun scheduleFollowupWork(cardId: Int, cardName: String?, message: String, whenMillis: Long) {
        val delay = maxOf(0, whenMillis - System.currentTimeMillis())
        val req = OneTimeWorkRequestBuilder<FollowUpWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(REMINDER_CARD_ID to cardId, "cardName" to cardName, REMINDER_MESSAGE to message))
            .addTag("followup_$cardId") // Unique tag for each card's follow-up
            .build()
        WorkManager.getInstance(getApplication()).enqueueUniqueWork("followup_$cardId", ExistingWorkPolicy.REPLACE, req)
    }

    fun markFollowUpDone(cardId: Int) = viewModelScope.launch {
        repository.getLastFollowUpForCard(cardId)?.let { followUp ->
            repository.updateFollowUp(followUp.copy(status = "done", completedAt = System.currentTimeMillis()))
            // Update bandit arm
            updateBanditArm(followUp.scheduledAt, System.currentTimeMillis())
        }
    }

    fun snoozeFollowUp(cardId: Int, days: Int) = viewModelScope.launch {
        repository.getLastFollowUpForCard(cardId)?.let { followUp ->
            val newScheduledAt = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(days.toLong())
            repository.updateFollowUp(followUp.copy(scheduledAt = newScheduledAt, status = "snoozed"))
            scheduleFollowupWork(cardId, repository.getCardById(cardId)?.name, "Snoozed follow-up", newScheduledAt)
        }
    }

    private suspend fun updateBanditArm(scheduledAt: Long, completedAt: Long) {
        val diffMillis = completedAt - scheduledAt
        val diffDays = TimeUnit.MILLISECONDS.toDays(diffMillis)

        val arms = listOf(1, 3, 7, 14) // Corresponding to "1d", "3d", etc.
        val closestArm = arms.minByOrNull { Math.abs(it - diffDays) }

        closestArm?.let { arm ->
            val armId = "${arm}d"
            val banditArm = repository.getBanditArm(armId) ?: BanditArm(armId, 0, 0)
            val newTries = banditArm.tries + 1
            val newSuccesses = if (Math.abs(diffMillis) <= TimeUnit.DAYS.toMillis(1)) { // Success if completed within ±1 day of scheduled time
                banditArm.successes + 1
            } else {
                banditArm.successes
            }
            repository.insertBanditArm(banditArm.copy(tries = newTries, successes = newSuccesses))
        }
    }

    // Add these methods for goal completion tracking
    suspend fun recordGoalCompletion(cardId: Int) {
        repository.recordGoalCompletion(cardId)
    }

    suspend fun getCurrentStreak(): Int {
        return repository.getCurrentStreak()
    }

    // Add this method to get the repository for external access
    fun getRepository(): BusinessCardRepository {
        return repository
    }

    // In BusinessCardViewModel.kt
    fun refreshTagsForAllCards() = viewModelScope.launch {
        val cards = repository.allCards.first()
        cards.forEach { card ->
            val newTags = suggestTags(card)
            if (newTags != card.tags) {
                repository.update(card.copy(tags = newTags))
            }
        }
    }

    fun refreshTagsForCard(cardId: Int) = viewModelScope.launch {
        val card = repository.getCardById(cardId)
        card?.let {
            val newTags = suggestTags(it)
            if (newTags != it.tags) {
                repository.update(it.copy(tags = newTags))
            }
        }
    }

    // Add to BusinessCardViewModel.kt
    private val _backupStatus = MutableStateFlow<BackupStatus>(BackupStatus.Idle)
    val backupStatus: StateFlow<BackupStatus> = _backupStatus.asStateFlow()

    sealed class BackupStatus {
        object Idle : BackupStatus()
        object InProgress : BackupStatus()
        data class Success(val timestamp: Long) : BackupStatus()
        data class Error(val message: String) : BackupStatus()
    }

    // Backup functions
    fun enableBackup(enable: Boolean) = viewModelScope.launch {
        repository.setBackupEnabled(enable)
        if (enable) {
            performBackup()
        }
    }

    // Fix performBackup method
    fun performBackup() = viewModelScope.launch {
        _backupStatus.value = BackupStatus.InProgress
        try {
            val user = FirebaseAuth.getInstance().currentUser
            if (user == null) {
                _backupStatus.value = BackupStatus.Error("User not authenticated")
                return@launch
            }

            // Get all data
            val cards = repository.allCards.first()
            val folders = repository.allFolders.first()
            val followUps = repository.getAllFollowUps().first()
            val banditArms = repository.getAllBanditArms()
            val goalCompletions = repository.getAllGoalCompletions()

            // Create backup data
            val backupData = mapOf(
                "cards" to cards,
                "folders" to folders,
                "followUps" to followUps,
                "banditArms" to banditArms,
                "goalCompletions" to goalCompletions,
                "backupTimestamp" to System.currentTimeMillis()
            )

            // Upload to Firebase - FIXED PATH
            val database = FirebaseDatabase.getInstance()
            val backupRef = database.reference
                .child("users")
                .child(user.uid)
                .child("backup")  // Changed from "backups/latest" to "backup"

            backupRef.setValue(backupData)
                .addOnSuccessListener {
                    viewModelScope.launch {
                        repository.updateLastBackupTimestamp(System.currentTimeMillis())
                        _backupStatus.value = BackupStatus.Success(System.currentTimeMillis())
                    }
                }
                .addOnFailureListener { e ->
                    _backupStatus.value = BackupStatus.Error(e.message ?: "Backup failed")
                }
        } catch (e: Exception) {
            _backupStatus.value = BackupStatus.Error(e.message ?: "Backup failed")
        }
    }

    // Fix restoreFromBackup method
    fun restoreFromBackup() = viewModelScope.launch {
        _backupStatus.value = BackupStatus.InProgress
        try {
            val user = FirebaseAuth.getInstance().currentUser
            if (user == null) {
                _backupStatus.value = BackupStatus.Error("User not authenticated")
                return@launch
            }

            val database = FirebaseDatabase.getInstance()
            val backupRef = database.reference
                .child("users")
                .child(user.uid)
                .child("backup")  // Changed to match backup path

            backupRef.get().addOnSuccessListener { snapshot ->
                viewModelScope.launch {
                    try {
                        val backupData = snapshot.value as? Map<String, Any?>
                        if (backupData == null) {
                            _backupStatus.value = BackupStatus.Error("No backup data found")
                            return@launch
                        }

                        // Clear existing data
                        // IMPORTANT: Clear in a specific order to avoid foreign key constraints issues
                        // Clear relationships first, then entities that depend on others.
                        repository.clearAllCardFolderRelationships()
                        repository.clearAllFollowUps()
                        repository.clearAllBanditArms()
                        repository.clearAllGoalCompletions()
                        repository.clearAllData() // This should clear BusinessCards and Folders

                        // Restore data by converting maps back to data class objects
                        (backupData["cards"] as? List<Map<String, Any?>>)?.let { cardMaps ->
                            cardMaps.mapNotNull { it.toBusinessCard() }.forEach { card ->
                                repository.insert(card)
                            }
                        }

                        (backupData["folders"] as? List<Map<String, Any?>>)?.let { folderMaps ->
                            folderMaps.mapNotNull { it.toFolder() }.forEach { folder ->
                                repository.insertFolder(folder)
                            }
                        }

                        // Restore card-folder cross-references (assuming they are not directly backed up as a separate list,
                        // but rather implicitly handled by re-inserting cards and folders.
                        // If you have a separate `card_folder_relations` node in Firebase, you'd need to restore it here.
                        // Based on your `performBackup`, you don't explicitly back up `CardFolderCrossRef` objects.
                        // If you need to restore these relationships, you'll need to add them to your backup data.

                        (backupData["followUps"] as? List<Map<String, Any?>>)?.let { followUpMaps ->
                            followUpMaps.mapNotNull { it.toFollowUp() }.forEach { followUp ->
                                repository.insertFollowUp(followUp)
                            }
                        }

                        (backupData["banditArms"] as? List<Map<String, Any?>>)?.let { banditArmMaps ->
                            banditArmMaps.mapNotNull { it.toBanditArm() }.forEach { banditArm ->
                                repository.insertBanditArm(banditArm)
                            }
                        }

                        (backupData["goalCompletions"] as? List<Map<String, Any?>>)?.let { goalCompletionMaps ->
                            goalCompletionMaps.mapNotNull { it.toGoalCompletion() }.forEach { goalCompletion ->
                                repository.businessCardDao.insertGoalCompletion(goalCompletion)
                            }
                        }

                        _backupStatus.value = BackupStatus.Success(System.currentTimeMillis())

                    } catch (e: Exception) {
                        _backupStatus.value = BackupStatus.Error("Restoration failed: ${e.message}")
                        Log.e("Restore", "Error during restoration", e)
                    }
                }
            }.addOnFailureListener { e ->
                _backupStatus.value = BackupStatus.Error(e.message ?: "Failed to fetch backup")
            }
        } catch (e: Exception) {
            _backupStatus.value = BackupStatus.Error(e.message ?: "Restoration failed")
        }
    }

    // Helper extension functions to convert Map<String, Any?> to data class objects
    private fun Map<String, Any?>.toBusinessCard(): BusinessCard? {
        return try {
            BusinessCard(
                id = (this["id"] as? Long)?.toInt() ?: 0,
                name = this["name"] as? String ?: "",
                company = this["company"] as? String ?: "",
                position = this["position"] as? String ?: "",
                phones = (this["phones"] as? List<String>) ?: emptyList(),
                email = this["email"] as? String ?: "",
                address = this["address"] as? String ?: "",
                website = this["website"] as? String ?: "",
                notes = this["notes"] as? String ?: "",
                imagePath = this["imagePath"] as? String,
                createdAt = this["createdAt"] as? Long ?: System.currentTimeMillis(),
                isFavorite = this["isFavorite"] as? Boolean ?: false,
                lastFollowUpDate = this["lastFollowUpDate"] as? Long,
                lastViewedAt = this["lastViewedAt"] as? Long ?: System.currentTimeMillis(),
                reminderTime = this["reminderTime"] as? Long,
                reminderMessage = this["reminderMessage"] as? String,
                tags = (this["tags"] as? List<String>) ?: emptyList(),
                leadScore = (this["leadScore"] as? Long)?.toInt() ?: 0,
                leadCategory = this["leadCategory"] as? String ?: "Unknown",
                industry = this["industry"] as? String ?: "Unknown",
                lastContactDate = this["lastContactDate"] as? Long,
                countryCode = this["countryCode"] as? String,
                firstContactMethod = this["firstContactMethod"] as? String,
                lastInteractionAt = this["lastInteractionAt"] as? Long,
                openedCount = (this["openedCount"] as? Long)?.toInt() ?: 0
            )
        } catch (e: Exception) {
            Log.e("BusinessCardViewModel", "Error converting map to BusinessCard: ${e.message}", e)
            null
        }
    }

    private fun Map<String, Any?>.toFolder(): Folder? {
        return try {
            Folder(
                id = (this["id"] as? Long)?.toInt() ?: 0,
                name = this["name"] as? String ?: "",
                createdAt = this["createdAt"] as? Long ?: System.currentTimeMillis()
            )
        } catch (e: Exception) {
            Log.e("BusinessCardViewModel", "Error converting map to Folder: ${e.message}", e)
            null
        }
    }

    private fun Map<String, Any?>.toFollowUp(): FollowUp? {
        return try {
            FollowUp(
                id = (this["id"] as? Long)?.toInt() ?: 0,
                cardId = (this["cardId"] as? Long)?.toInt() ?: 0,
                scheduledAt = this["scheduledAt"] as? Long ?: 0L,
                reason = this["reason"] as? String ?: "",
                suggestedByAi = this["suggestedByAi"] as? Boolean ?: false,
                status = this["status"] as? String ?: "",
                completedAt = this["completedAt"] as? Long
            )
        } catch (e: Exception) {
            Log.e("BusinessCardViewModel", "Error converting map to FollowUp: ${e.message}", e)
            null
        }
    }

    private fun Map<String, Any?>.toBanditArm(): BanditArm? {
        return try {
            BanditArm(
                armId = this["armId"] as? String ?: "",
                tries = (this["tries"] as? Long)?.toInt() ?: 0,
                successes = (this["successes"] as? Long)?.toInt() ?: 0
            )
        } catch (e: Exception) {
            Log.e("BusinessCardViewModel", "Error converting map to BanditArm: ${e.message}", e)
            null
        }
    }

    private fun Map<String, Any?>.toGoalCompletion(): GoalCompletion? {
        return try {
            GoalCompletion(
                id = (this["id"] as? Long)?.toInt() ?: 0,
                cardId = (this["cardId"] as? Long)?.toInt() ?: 0,
                completedAt = this["completedAt"] as? Long ?: 0L
            )
        } catch (e: Exception) {
            Log.e("BusinessCardViewModel", "Error converting map to GoalCompletion: ${e.message}", e)
            null
        }
    }

// Add synchronous versions of your DAO methods if needed

    // Add automatic backup on data changes
    fun scheduleAutomaticBackup() = viewModelScope.launch {
        if (repository.isBackupEnabled()) {
            val lastBackup = repository.getLastBackupTimestamp()
            val currentTime = System.currentTimeMillis()

            // Backup every 24 hours or when significant changes occur
            if (currentTime - lastBackup > 24 * 60 * 60 * 1000) {
                performBackup()
            }
        }
    }

    // Add to your BusinessCardViewModel class
    private val sharingRepository by lazy {
        CardSharingRepository(application.applicationContext)
    }

    fun listenForIncomingShares(onCardReceived: (CardSharingRepository.SharedCard) -> Unit): ListenerRegistration {
        return sharingRepository.listenForIncomingShares(onCardReceived)
    }

    fun searchUsersByUsername(username: String, callback: (List<CardSharingRepository.User>) -> Unit) {
        sharingRepository.searchUsersByUsername(username, callback)
    }

    // Update the ViewModel methods to use the new async approach

    // Remove the old synchronous method and replace with:
    fun generateQrForCard(
        card: BusinessCard,
        cardImage: Bitmap?,
        includeImage: Boolean = false,
        callback: (CardSharingRepository.QrResult) -> Unit
    ) {
        sharingRepository.generateQrForCard(card, cardImage, includeImage, callback)
    }

    private val cardSharingRepository = CardSharingRepository(application)

    // Keep the other methods the same
    // In BusinessCardViewModel.kt - update the processScannedQr method
    @RequiresApi(Build.VERSION_CODES.O)
    fun processScannedQr(qrData: String, onComplete: (BusinessCard?) -> Unit) {
        viewModelScope.launch {
            try {
                val processedCard = withContext(Dispatchers.IO) {
                    cardSharingRepository.processScannedQr(qrData)
                }

                processedCard?.let { card ->
                    // Insert the card into the database (will create duplicate with new ID)
                    insert(card)
                    onComplete(card) // Notify completion AFTER insertion
                } ?: run {
                    onComplete(null) // Notify failure
                }
            } catch (e: Exception) {
                Log.e("BusinessCardViewModel", "QR processing failed", e)
                onComplete(null)
            }
        }
    }

    // Helper function to find existing cards based on key fields
    private suspend fun findExistingCard(scannedCard: BusinessCard): BusinessCard? {
        val allCards = allCards.first()

        // Look for matches based on name, company, and email combination
        return allCards.find { existingCard ->
            (existingCard.name.equals(scannedCard.name, ignoreCase = true) &&
                    existingCard.company.equals(scannedCard.company, ignoreCase = true)) ||
                    (existingCard.email.isNotBlank() &&
                            existingCard.email.equals(scannedCard.email, ignoreCase = true))
        }
    }

    // Helper function to merge data from scanned card into existing card
    private fun mergeCardData(existingCard: BusinessCard, scannedCard: BusinessCard): BusinessCard {
        return existingCard.copy(
            // Keep existing ID and timestamps
            name = scannedCard.name.takeIf { it.isNotBlank() } ?: existingCard.name,
            company = scannedCard.company.takeIf { it.isNotBlank() } ?: existingCard.company,
            position = scannedCard.position.takeIf { it.isNotBlank() } ?: existingCard.position,
            // Merge phone lists - add new numbers that don't exist
            phones = (existingCard.phones + scannedCard.phones).distinct(),
            email = scannedCard.email.takeIf { it.isNotBlank() } ?: existingCard.email,
            address = scannedCard.address.takeIf { it.isNotBlank() } ?: existingCard.address,
            website = scannedCard.website.takeIf { it.isNotBlank() } ?: existingCard.website,
            notes = if (scannedCard.notes.isNotBlank()) {
                // Append new notes to existing ones
                if (existingCard.notes.isNotBlank()) {
                    "$existingCard.notes\n---\n$scannedCard.notes"
                } else {
                    scannedCard.notes
                }
            } else {
                existingCard.notes
            },
            // Use the newer image if available
            imagePath = scannedCard.imagePath ?: existingCard.imagePath,
            // Update tags with new suggestions
            tags = if (scannedCard.tags.isNotEmpty()) scannedCard.tags else existingCard.tags,
            // Update last viewed time
            lastViewedAt = System.currentTimeMillis()
        )
    }

    val newLeads = repository.getCardsByPipelineStage(PipelineStage.NEW)
    val contactedLeads = repository.getCardsByPipelineStage(PipelineStage.CONTACTED)
    val closedWonLeads = repository.getCardsByPipelineStage(PipelineStage.CLOSED_WON)
    val closedLostLeads = repository.getCardsByPipelineStage(PipelineStage.CLOSED_LOST)
    val meetingLeads = repository.getCardsByPipelineStage(PipelineStage.MEETING)
    val negotiationLeads = repository.getCardsByPipelineStage(PipelineStage.NEGOTIATION)

    val pipelineCards = repository.getCardsForPipelineView()

    fun updatePipelineStage(cardId: Int, newStage: PipelineStage) = viewModelScope.launch {
        repository.updatePipelineStage(cardId, newStage)

        if (newStage == PipelineStage.CONTACTED){
            val card = repository.getCardById(cardId)
            card?.let{
                repository.update(it.copy(lastContactDate = System.currentTimeMillis()))
            }
        }
    }

    suspend fun calculateAnalytics(): PipelineAnalytics {
        val totalContacts = repository.allCards.first().size
        val stageDistribution = repository.getPipelineDistribution()
        val overdueFollowUps = repository.getOverdueFollowUpsCount(System.currentTimeMillis())

        // Calculate conversion rate (Closed Won / Total)
        val closedWonCount = stageDistribution[PipelineStage.CLOSED_WON] ?: 0
        val conversionRate = if (totalContacts > 0) {
            (closedWonCount.toFloat() / totalContacts.toFloat()) * 100
        } else 0f

        return PipelineAnalytics(
            totalContacts = totalContacts,
            conversionRate = conversionRate,
            stageDistribution = stageDistribution,
            overdueFollowUps = overdueFollowUps
        )
    }

    // Calendar Integration Methods
    fun hasCalendarPermission(): Boolean = calendarService.hasCalendarPermission()

    fun requestCalendarPermission(activity: Activity) {
        calendarService.requestCalendarPermission(activity)
    }

    fun addFollowUpToCalendar(
        cardName: String,
        company: String?,
        followUpTime: Long,
        notes: String? = null
    ): Boolean {
        return calendarService.addFollowUpToCalendar(cardName, company, followUpTime, notes)
    }

    // Smart Notification Methods
    fun scheduleSmartNotifications() = viewModelScope.launch {
        // Schedule daily digest at 9 AM
        val dailyRequest = PeriodicWorkRequestBuilder<DailyDigestWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(calculateInitialDelay(9, 0), TimeUnit.MILLISECONDS)
            .build()

        // Schedule stale leads check every 12 hours
        val staleLeadsRequest = PeriodicWorkRequestBuilder<StaleLeadsWorker>(12, TimeUnit.HOURS)
            .build()

        WorkManager.getInstance(getApplication()).apply {
            enqueueUniquePeriodicWork(
                "daily_digest",
                ExistingPeriodicWorkPolicy.KEEP, // FIXED: Use ExistingPeriodicWorkPolicy
                dailyRequest
            )
            enqueueUniquePeriodicWork(
                "stale_leads_check",
                ExistingPeriodicWorkPolicy.KEEP, // FIXED: Use ExistingPeriodicWorkPolicy
                staleLeadsRequest
            )
        }
    }

    private fun calculateInitialDelay(hour: Int, minute: Int): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }

        var triggerTime = calendar.timeInMillis
        if (triggerTime < System.currentTimeMillis()) {
            triggerTime += 24 * 60 * 60 * 1000 // Add 24 hours if time has passed today
        }

        return triggerTime - System.currentTimeMillis()
    }

    private fun shouldAddToCalendar(): Boolean {
        // Use PreferenceManager correctly
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplication())
        return sharedPrefs.getBoolean("auto_add_to_calendar", true)
    }

    // Manual trigger for testing
    fun triggerDailyDigest() = viewModelScope.launch {
        smartNotificationManager.showDailyDigestNotification()
    }

    fun triggerStaleLeadsCheck() = viewModelScope.launch {
        smartNotificationManager.showStaleLeadsNotification()
    }

    // Update the suggestAndScheduleSmartFollowUp method in BusinessCardViewModel

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun suggestAndScheduleSmartFollowUp(card: BusinessCard): SmartFollowUpPrediction? {
        return try {
            val prediction = smartFollowUpPredictor.predictOptimalFollowUp(card)

            // Save to database
            val followUp = FollowUp(
                cardId = card.id,
                scheduledAt = prediction.optimalTime.toInstant().toEpochMilli(),
                reason = "smart-suggestion",
                suggestedByAi = true,
                status = "scheduled",
                completedAt = null
            )
            repository.insertFollowUp(followUp)

            // Schedule smart follow-up work instead of regular follow-up
            scheduleSmartFollowupWork(
                card.id,
                card.name,
                prediction.smartMessage,
                prediction.optimalTime.toInstant().toEpochMilli()
            )

            // Add to calendar if user prefers
            if (shouldAddToCalendar()) {
                addFollowUpToCalendar(
                    card.name,
                    card.company,
                    prediction.optimalTime.toInstant().toEpochMilli(),
                    "Smart follow-up: ${prediction.smartMessage}"
                )
            }

            prediction
        } catch (e: Exception) {
            Log.e("SmartFollowUp", "Error in smart follow-up prediction", e)
            null
        }
    }

    private fun scheduleSmartFollowupWork(cardId: Int, cardName: String?, message: String, whenMillis: Long) {
        val delay = maxOf(0, whenMillis - System.currentTimeMillis())
        val req = OneTimeWorkRequestBuilder<SmartFollowUpWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(
                REMINDER_CARD_ID to cardId,
                "cardName" to cardName,
                REMINDER_MESSAGE to message
            ))
            .addTag("smart_followup_$cardId")
            .build()
        WorkManager.getInstance(getApplication()).enqueueUniqueWork(
            "smart_followup_$cardId",
            ExistingWorkPolicy.REPLACE,
            req
        )
    }

    // Add to BusinessCardViewModel class
    fun snoozeReminder(cardId: Int, days: Int) = viewModelScope.launch {
        val card = repository.getCardById(cardId)
        card?.let {
            val newReminderTime = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(days.toLong())
            repository.update(it.copy(reminderTime = newReminderTime))

            // Reschedule the alarm
            scheduleReminder(getApplication(), cardId, newReminderTime, it.reminderMessage ?: "Follow up reminder", it.name)
        }
    }

    fun markReminderDone(cardId: Int) = viewModelScope.launch {
        val card = repository.getCardById(cardId)
        card?.let {
            repository.update(it.copy(reminderTime = null, reminderMessage = null))
            cancelReminder(getApplication(), cardId)
        }
    }

    private fun scheduleReminder(context: Context, cardId: Int, time: Long, message: String, cardName: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            putExtra(REMINDER_CARD_ID, cardId)
            putExtra(REMINDER_MESSAGE, message)
            putExtra(REMINDER_NOTIFICATION_ID, cardId + REMINDER_REQUEST_CODE_BASE)
            putExtra("cardName", cardName)
        }

        val pendingIntent = android.app.PendingIntent.getBroadcast(
            context,
            cardId + REMINDER_REQUEST_CODE_BASE,
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, time, pendingIntent)
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, time, pendingIntent)
        }
    }

    private fun cancelReminder(context: Context, cardId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        val intent = Intent(context, ReminderBroadcastReceiver::class.java)
        val pendingIntent = android.app.PendingIntent.getBroadcast(
            context,
            cardId + REMINDER_REQUEST_CODE_BASE,
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

}

// Add this data class for pipeline analytics
data class PipelineAnalytics(
    val totalContacts: Int = 0,
    val conversionRate: Float = 0f,
    val stageDistribution: Map<PipelineStage, Int> = emptyMap(),
    val overdueFollowUps: Int = 0
)