package com.appyhigh.adutils.models

import com.appyhigh.adutils.AdSdk
import com.google.android.gms.ads.nativead.NativeAdView

data class PreloadNativeAds(
    val adId: String,
    val adSize: String = AdSdk.ADType.DEFAULT_AD,
    var ad: NativeAdView? = null,
    val mediaMaxHeight: Int,
    val loadingTextSize: Int
)
