package com.raghu.businesscardscanner2

import android.app.Activity
import android.app.Application
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.raghu.businesscardscanner2.AdHelper.showAd // Assuming AdHelper exists and has showAd
import com.raghu.businesscardscanner2.MLkitTextRec.TextRecognizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import androidx.room.Room
import com.raghu.businesscardscanner2.FollowUpRemaiders.FollowUpRepository
import com.raghu.businesscardscanner2.FollowUpRemaiders.FollowUpViewModelFactory
import com.raghu.businesscardscanner2.FollowUpRemaiders.NotificationHelper
import com.raghu.businesscardscanner2.RoomDB.DataBase.AppDatabase
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class BusinessCardScannerApp : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val textRecognizer by lazy { TextRecognizer() }
    val adManager by lazy { AdManager(this) }

    val database by lazy {
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "business_card_database"
        ).build()
    }

    val followUpRepository by lazy { FollowUpRepository(database.followUpReminderDao()) }
    val notificationHelper by lazy { NotificationHelper(this) }

    val followUpViewModelFactory by lazy {
        FollowUpViewModelFactory(followUpRepository, notificationHelper)
    }

    override fun onCreate() {
        super.onCreate()
        initializeAdSystems()
        initializeMLKit()
        initializeFollowUpSystem()
        Log.d("BusinessCardScannerApp", "Application initialized")
    }

    private fun initializeAdSystems() {
        AdHelper.init(this)
        MobileAds.initialize(this) { status ->
            Log.d("AdMob", "Initialization status: ${status.adapterStatusMap}")
        }
    }

    private fun initializeMLKit() {
        applicationScope.launch {
            try {
                textRecognizer.initializeRecognizers()
            } catch (e: Exception) {
                Log.e("MLKit", "TextRecognizer init failed", e)
            }
        }
    }

    private fun initializeFollowUpSystem() {
        // 1. Fix private access to createNotificationChannel
        notificationHelper.createNotificationChannel()

        applicationScope.launch {
            try {
                // Reschedule all pending reminders
                followUpRepository.getPendingReminders().collect { pendingReminders ->
                    pendingReminders.forEach { reminder ->
                        notificationHelper.scheduleFollowUpNotification(
                            cardId = reminder.id,
                            message = reminder.message,
                            triggerTime = reminder.dueDate,
                            repeatType = reminder.repeatType,
                            contactName = reminder.contactName,
                            companyName = reminder.companyName,
                            snoozeMinutes = 10 // Default value
                        )
                    }
                    Log.d("Reminders", "Rescheduled ${pendingReminders.size} pending reminders")
                }
            } catch (e: Exception) {
                Log.e("Reminders", "Failed to reschedule reminders", e)
            }
        }
    }

    override fun onTerminate() {
        applicationScope.cancel()
        super.onTerminate()
    }
}