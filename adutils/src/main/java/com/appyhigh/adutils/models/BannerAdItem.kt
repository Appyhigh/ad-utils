package com.appyhigh.adutils.models

import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import com.appyhigh.adutils.callbacks.BannerAdLoadCallback
import com.google.android.gms.ads.AdSize

data class BannerAdItem(
    val id: Long,
    val lifecycle: Lifecycle,
    val viewGroup: ViewGroup,
    val adUnit: String,
    val adSize: AdSize,
    val bannerAdLoadCallback: BannerAdLoadCallback?
)