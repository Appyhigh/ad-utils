package com.appyhigh.adutils.callbacks

import com.appyhigh.adutils.AppOpenManager
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.LoadAdError

abstract class AppOpenAdCallback {
    open fun onInitSuccess(manager: AppOpenManager) {}
    open fun onAdLoaded() {}
    open fun onAdFailedToLoad(loadAdError: LoadAdError) {}
    open fun onAdFailedToShow(adError: AdError) {}
    open fun onAdClosed() {}
}