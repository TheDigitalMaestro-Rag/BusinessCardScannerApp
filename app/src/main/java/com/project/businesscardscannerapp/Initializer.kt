package com.project.businesscardscannerapp

import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.gms.ads.MobileAds
import com.project.businesscardscannerapp.MLkitTextRec.TextRecognizer
import com.project.businesscardscannerapp.RoomDB.DataBase.AppDatabase
import com.project.businesscardscannerapp.Workers.CoachWorker
import com.project.businesscardscannerapp.Workers.InsightsWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class BusinessCardScannerApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val textRecognizer by lazy { TextRecognizer() }
    val adManager by lazy { AdManager(this) }

    val database by lazy {
        AppDatabase.getDatabase(this)
    }

    override fun onCreate() {
        super.onCreate()
        initializeAdSystems()
        initializeMLKit()
        scheduleInsightsWorker()
        scheduleCoachWorker()
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

    private fun scheduleInsightsWorker() {
        val insightsWorkRequest = PeriodicWorkRequestBuilder<InsightsWorker>(
            7, TimeUnit.DAYS, // Repeat every 7 days
            1, TimeUnit.HOURS // Flex interval (optional, but good practice)
        )
            .addTag("insights_summary_work")
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "insights_summary_work",
            ExistingPeriodicWorkPolicy.KEEP, // Keep existing work if already scheduled
            insightsWorkRequest
        )
    }

    private fun scheduleCoachWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val coachWorkRequest = PeriodicWorkRequestBuilder<CoachWorker>(
            1, TimeUnit.DAYS // Run once per day
        ).setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "coach_worker",
            ExistingPeriodicWorkPolicy.KEEP,
            coachWorkRequest
        )

        Log.d("CoachWorker", "Coach worker scheduled to run daily")
    }

    override fun onTerminate() {
        applicationScope.cancel()
        super.onTerminate()
    }
}