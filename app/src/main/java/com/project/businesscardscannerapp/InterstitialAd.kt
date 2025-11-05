package com.project.businesscardscannerapp

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.activity.ComponentActivity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

object AdHelper {
    private var interstitialAd: InterstitialAd? = null
    private lateinit var appContext: Context
    private const val TAG = "AdHelper"

    // Test Ad Unit ID (replace with real one for production) ca-app-pub-3940256099942544/1033173712
    private const val interstitialAdUnitId = "///ca-app-pub-3940256099942544/1033173712"

    fun init(context: Context) {
        appContext = context.applicationContext
        MobileAds.initialize(appContext) {
            Log.d(TAG, "AdMob initialized")
        }
        loadInterstitialAd()
    }

    private fun loadInterstitialAd() {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            appContext,
            interstitialAdUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    Log.d(TAG, "Interstitial ad loaded")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    Log.w(TAG, "Failed to load interstitial ad: ${error.message}")
                }
            }
        )
    }

    fun showAd(activity: Activity) {
        if (interstitialAd != null) {
            interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    loadInterstitialAd()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    interstitialAd = null
                    Log.w(TAG, "Failed to show ad: ${adError.message}")
                    loadInterstitialAd()
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "Ad showed successfully")
                }
            }

            try {
                interstitialAd?.show(activity)
            } catch (e: Exception) {
                Log.e(TAG, "Error showing ad: ${e.message}")
                loadInterstitialAd()
            }
        } else {
            Log.d(TAG, "Ad not ready - loading new one")
            loadInterstitialAd()
        }
    }

    fun showAdIfAvailable(activity: Activity) {
        if (interstitialAd != null) {
            showAd(activity)
        } else {
            Log.d(TAG, "Ad not available")
        }
    }
}