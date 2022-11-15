package com.appyhigh.adutils.callbacks

import com.google.android.gms.ads.LoadAdError

abstract class NativeAdLoadCallback {
    open fun onAdLoaded() {}
    open fun onAdFailed(adError: LoadAdError?) {}
    open fun onAdClicked() {}
}