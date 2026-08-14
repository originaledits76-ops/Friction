package com.example.features.ads

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

    if (isAdLoaded) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                modifier = Modifier.fillMaxWidth(),
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
                            }
                        }
                        loadAd(AdRequest.Builder().build())
                    }
                }
            )
        }
    } else {
        // Render offscreen/hidden factory to trigger loadListener without taking UI space
        AndroidView(
            modifier = Modifier,
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
                        }
                    }
                    loadAd(AdRequest.Builder().build())
                }
            }
        )
    }
}
