package com.appyhigh.adutils.interfaces

import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd

interface RewardInterstitialInternalCallback {
    fun onSuccess(ad: RewardedInterstitialAd?)
    fun onFailed()
}