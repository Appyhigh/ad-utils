package com.appyhigh.adutils.interfaces

import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd

interface BannerInternalCallback {
    fun onSuccess()
    fun onFailed(loadAdError: LoadAdError?)
}