package com.appyhigh.adutils.callbacks

import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.admanager.AdManagerInterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAd

interface InterstitialAdUtilLoadCallback {
    fun onAdFailedToLoad(adError: LoadAdError?, ad: InterstitialAd?)
    fun onAdLoaded(interstitialAd: InterstitialAd? = null,adManagerInterstitialAd: AdManagerInterstitialAd? = null)
    fun onAdImpression() {

    }
    fun onAdDismissedFullScreenContent()
    fun onAdFailedToShowFullScreenContent(adError: AdError?)
    fun onAdShowedFullScreenContent()
    fun onContextFailed()
}