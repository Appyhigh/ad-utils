package com.appyhigh.adutils.interfaces

import com.google.android.gms.ads.interstitial.InterstitialAd

interface InterstitialInternalCallback {
    fun onSuccess(interstitialAd: InterstitialAd)
    fun onFailed(msg: String?=null)
}