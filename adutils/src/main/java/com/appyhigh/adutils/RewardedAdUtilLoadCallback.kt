package com.appyhigh.adutils

import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd

interface RewardedAdUtilLoadCallback {
    fun onAdFailedToLoad(adError: LoadAdError, ad: RewardedAd?)
    fun onAdLoaded(ad: RewardedAd?)
    fun onAdDismissedFullScreenContent()
    fun onAdFailedToShowFullScreenContent(adError: AdError?)
    fun onAdShowedFullScreenContent()
}