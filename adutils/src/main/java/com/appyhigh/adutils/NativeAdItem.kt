package com.appyhigh.adutils

import androidx.lifecycle.Lifecycle
import com.google.android.gms.ads.AdSize

data class NativeAdItem(
    val lifecycle: Lifecycle,
    val adUnit: String,
    val nativeAdLoadCallback: NativeAdLoadCallback?,
    val layoutId:Int
)
