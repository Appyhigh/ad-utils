package com.appyhigh.adutils

import com.google.android.gms.ads.LoadAdError

abstract class NativeAdLoadCallback {
    open fun onAdLoaded() {}
    open fun onAdFailed(adError: LoadAdError) {}
    open fun onAdClicked() {}
}