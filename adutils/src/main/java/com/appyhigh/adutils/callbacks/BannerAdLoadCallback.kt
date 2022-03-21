package com.appyhigh.adutils.callbacks

import com.google.android.gms.ads.LoadAdError

interface BannerAdLoadCallback {
    fun onAdLoaded()

    fun onAdFailedToLoad(adError: LoadAdError)

    fun onAdOpened()

    fun onAdClicked()

    fun onAdClosed()
}