package com.appyhigh.adutils

import androidx.lifecycle.Lifecycle
import com.google.android.gms.ads.AdSize

data class BannerAdItem(
    val lifecycle: Lifecycle,
    val adUnit: String,
    val adSize: AdSize,
    val bannerAdLoadCallback: BannerAdLoadCallback?
)
