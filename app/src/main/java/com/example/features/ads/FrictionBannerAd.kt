package com.example.features.ads

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.example.data.model.User

@Composable
fun FrictionBannerAd(user: User?, modifier: Modifier = Modifier) {
    val isPremium = user?.let { it.premium || (it.isTrialActive && !it.hasTrialExpired()) } ?: false
    if (isPremium) return

    var isAdLoaded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(if (isAdLoaded) Modifier.padding(vertical = 4.dp) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = if (isAdLoaded) Modifier.fillMaxWidth() else Modifier,
            factory = { context ->
                AdView(context).apply {
                    setAdSize(AdSize.BANNER)
                    adUnitId = AdManager.BANNER_AD_UNIT_ID
                    adListener = object : AdListener() {
                        override fun onAdLoaded() {
                            isAdLoaded = true
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            isAdLoaded = false
                            Log.w("FrictionBannerAd", "Banner ad failed to load: ${error.message}")
                        }
                    }
                    try {
                        loadAd(AdRequest.Builder().build())
                    } catch (e: Throwable) {
                        Log.e("FrictionBannerAd", "Exception while loading banner ad: ${e.message}")
                    }
                }
            }
        )
    }
}

