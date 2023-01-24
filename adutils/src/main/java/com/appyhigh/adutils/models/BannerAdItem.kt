package com.appyhigh.adutils.models

import android.app.Activity
import android.content.Context
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import com.appyhigh.adutils.callbacks.BannerAdLoadCallback
import com.google.android.gms.ads.AdSize

data class BannerAdItem(
    val activity: Context,
    val id: Long,
    val lifecycle: Lifecycle,
    val viewGroup: ViewGroup,
    val adUnit: String,
    val adSize: AdSize,
    val adName: String,
    val showLoadingMessage: Boolean,
    val bannerAdLoadCallback: BannerAdLoadCallback?,
    var contentURL: String?,
    var neighbourContentURL: List<String>?,
    var isAdManager:Boolean
)
