package com.appyhigh.adutils.interfaces

import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.admanager.AdManagerInterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAd

interface InterstitialInternalCallback {
    fun onSuccess(interstitialAd: InterstitialAd? = null,adManagerInterstitialAd: AdManagerInterstitialAd? = null)
    fun onFailed(loadAdError: LoadAdError?)
}