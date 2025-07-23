package com.raghu.businesscardscanner2

import android.app.Activity
import android.app.Application
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.raghu.businesscardscanner2.AdHelper.showAd
import com.raghu.businesscardscanner2.MLkitTextRec.TextRecognizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import androidx.room.Room
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


    override fun onCreate() {
        super.onCreate()
        initializeAdSystems()
        initializeMLKit()
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



    override fun onTerminate() {
        applicationScope.cancel()
        super.onTerminate()
    }
}
