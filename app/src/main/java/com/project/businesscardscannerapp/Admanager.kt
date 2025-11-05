package com.project.businesscardscannerapp

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.compose.runtime.*
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

class AdManager(private val context: Context) {
    private var interstitialAd: InterstitialAd? by mutableStateOf(null)
    private val TAG = "AdManager"

    // Replace with your actual ad unit ID
    private val interstitialAdUnitId = "ca-app-pub-3940256099942544/1033173712" // Test ID

    init {
        loadInterstitialAd()
    }

    private fun loadInterstitialAd() {
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            context,
            interstitialAdUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    Log.d(TAG, "Interstitial ad loaded")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    interstitialAd = null
                    Log.d(TAG, "Interstitial ad failed to load: ${loadAdError.message}")
                }
            }
        )
    }

    fun showInterstitialAd(
        onAdDismissed: () -> Unit = {},
        onAdFailed: () -> Unit = {}
    ) {
        interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                loadInterstitialAd() // Load the next ad
                onAdDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                interstitialAd = null
                loadInterstitialAd()
                onAdFailed()
                Log.d(TAG, "Ad failed to show: ${adError.message}")
            }
        }

        interstitialAd?.show((context as androidx.activity.ComponentActivity))
            ?: run {
                Log.d(TAG, "Ad wasn't ready")
                onAdFailed()
            }
    }
}