package com.appyhigh.adutils.interfaces

import com.appyhigh.adutils.DynamicsAds
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd

interface RewardInternalCallback {
    fun onSuccess(rewardAds: RewardedAd)
    fun onFailed(adError: LoadAdError?, ad: RewardedAd?)
}