package com.appyhigh.adutils.callbacks

import com.appyhigh.adutils.AppOpenManager
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd

abstract class AppOpenAdCallback : AppOpenAdLoadCallback() {
    open fun onInitSuccess(manager: AppOpenManager) {}
}

abstract class AppOpenAdLoadCallback {
    open fun onAdLoaded(ad: AppOpenAd) {}
    open fun onAdFailedToLoad(loadAdError: LoadAdError) {}
    open fun onAdFailedToShow(adError: AdError) {}
    open fun onAdClosed() {}
}