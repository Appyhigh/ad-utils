package com.appyhigh.adutils.models

import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import com.appyhigh.adutils.callbacks.NativeAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

data class NativeAdItem(
    val id: Long,
    val lifecycle: Lifecycle,
    val adUnit: String,
    val viewGroup: ViewGroup,
    val nativeAdLoadCallback: NativeAdLoadCallback?,
    val layoutId:Int,
    val populator: ((nativeAd: NativeAd, adView: NativeAdView) -> Unit)? = null
)
