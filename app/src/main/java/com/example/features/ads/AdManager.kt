package com.example.features.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AdManager {
    private const val TAG = "AdManager"

    // Official Google Test Ad Unit IDs
    private const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    private const val REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
    const val BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"

    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null

    private var isInterstitialLoading = false
    private var isRewardedLoading = false

    private val _isAdShowing = MutableStateFlow(false)
    val isAdShowing: StateFlow<Boolean> = _isAdShowing.asStateFlow()

    private var isPremiumUser = false
    private var lastInterstitialTime: Long = 0L
    private const val INTERSTITIAL_COOLDOWN_MS = 3 * 60 * 1000L // Hard 3-minute safety cap (180,000 ms)

    fun initialize(context: Context) {
        try {
            val prefs = context.getSharedPreferences("ad_prefs", Context.MODE_PRIVATE)
            lastInterstitialTime = prefs.getLong("last_interstitial_timestamp", 0L)

            MobileAds.initialize(context) { initializationStatus ->
                Log.d(TAG, "AdMob Initialized: $initializationStatus")
                loadInterstitialAd(context)
                loadRewardedAd(context)
            }
        } catch (e: Exception) {
            Log.e(TAG, "AdMob initialization error: ${e.message}")
        }
    }

    fun updateUserPremiumStatus(isPremium: Boolean, context: Context) {
        val wasPremium = this.isPremiumUser
        this.isPremiumUser = isPremium

        if (isPremium) {
            interstitialAd = null
            rewardedAd = null
        } else if (wasPremium && !isPremium) {
            loadInterstitialAd(context)
            loadRewardedAd(context)
        }
    }

    private fun loadInterstitialAd(context: Context) {
        if (isPremiumUser || interstitialAd != null || isInterstitialLoading) return

        isInterstitialLoading = true
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            context,
            INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isInterstitialLoading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    isInterstitialLoading = false
                }
            }
        )
    }

    private fun loadRewardedAd(context: Context) {
        if (isPremiumUser || rewardedAd != null || isRewardedLoading) return

        isRewardedLoading = true
        val adRequest = AdRequest.Builder().build()

        RewardedAd.load(
            context,
            REWARDED_AD_UNIT_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isRewardedLoading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    isRewardedLoading = false
                }
            }
        )
    }

    fun handleAppOpen(context: Context, activity: Activity?, isSafeToDisplay: () -> Boolean) {
        if (isPremiumUser || activity == null || _isAdShowing.value) return

        val now = System.currentTimeMillis()
        if (now - lastInterstitialTime < INTERSTITIAL_COOLDOWN_MS) {
            Log.d(TAG, "Interstitial suppressed: hard 3-minute cooldown active (${(INTERSTITIAL_COOLDOWN_MS - (now - lastInterstitialTime)) / 1000}s remaining)")
            return
        }

        if (!isSafeToDisplay()) {
            Log.d(TAG, "Interstitial suppressed: current flow is not safe for ad display.")
            return
        }

        val prefs = context.getSharedPreferences("ad_prefs", Context.MODE_PRIVATE)
        var openCount = prefs.getInt("app_open_count", 0)
        openCount++

        Log.d(TAG, "App Open Count: $openCount")

        if (openCount >= 5) {
            if (interstitialAd != null) {
                showInterstitialAd(activity, prefs) {
                    prefs.edit().putInt("app_open_count", 0).apply()
                    loadInterstitialAd(context)
                }
                return
            }
        }

        prefs.edit().putInt("app_open_count", openCount).apply()
        loadInterstitialAd(context)
    }

    private fun showInterstitialAd(activity: Activity, prefs: android.content.SharedPreferences, onComplete: () -> Unit) {
        val ad = interstitialAd
        val now = System.currentTimeMillis()

        if (isPremiumUser || ad == null || _isAdShowing.value || (now - lastInterstitialTime < INTERSTITIAL_COOLDOWN_MS)) {
            onComplete()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                _isAdShowing.value = false
                interstitialAd = null
                onComplete()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                _isAdShowing.value = false
                interstitialAd = null
                onComplete()
            }

            override fun onAdShowedFullScreenContent() {
                _isAdShowing.value = true
                lastInterstitialTime = System.currentTimeMillis()
                prefs.edit().putLong("last_interstitial_timestamp", lastInterstitialTime).apply()
                interstitialAd = null
            }
        }

        ad.show(activity)
    }

    fun showRewardedAd(activity: Activity, onRewardEarned: () -> Unit, onDismissed: (Boolean) -> Unit) {
        val ad = rewardedAd
        if (ad == null) {
            onDismissed(false)
            loadRewardedAd(activity.applicationContext)
            return
        }

        var rewardGranted = false

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                _isAdShowing.value = false
                rewardedAd = null
                onDismissed(rewardGranted)
                loadRewardedAd(activity.applicationContext)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                _isAdShowing.value = false
                rewardedAd = null
                onDismissed(false)
                loadRewardedAd(activity.applicationContext)
            }

            override fun onAdShowedFullScreenContent() {
                _isAdShowing.value = true
                rewardedAd = null
            }
        }

        ad.show(activity) { _ ->
            rewardGranted = true
            onRewardEarned()
        }
    }
}
