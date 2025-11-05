package com.project.businesscardscannerapp


import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

@Composable
fun BannerAdView(
    adUnitId: String = "///ca-app-pub-3940256099942544/6300978111" // Test ID ca-app-pub-8849596985483178/6170423532  ca-app-pub-3940256099942544/6300978111
) {
    val context = LocalContext.current
    val adView = remember { AdView(context) }

    AndroidView(
        factory = { context ->
            adView.adUnitId = adUnitId
            adView.setAdSize(AdSize.BANNER)
            adView.loadAd(AdRequest.Builder().build())
            adView
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
    )
}

