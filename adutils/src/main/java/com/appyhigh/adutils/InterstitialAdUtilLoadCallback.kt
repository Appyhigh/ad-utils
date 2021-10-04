package com.appyhigh.adutils

import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd

interface InterstitialAdUtilLoadCallback {
    fun onAdFailedToLoad(adError: LoadAdError, ad: InterstitialAd?)
    fun onAdLoaded(ad: InterstitialAd?)
    fun onAdDismissedFullScreenContent()
    fun onAdFailedToShowFullScreenContent(adError: AdError?)
    fun onAdShowedFullScreenContent()
}