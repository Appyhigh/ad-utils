package com.appyhigh.adutils.models

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import com.appyhigh.adutils.callbacks.NativeAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

data class NativeAdItemService(
    val layoutInflater: LayoutInflater,
    val context: Context,
    val id: Long,
    val adUnit: String,
    val viewGroup: ViewGroup,
    val nativeAdLoadCallback: NativeAdLoadCallback?,
    val populator: ((nativeAd: NativeAd, adView: NativeAdView) -> Unit)? = null,
    var viewId: String = "1",
    var background: Any? = null,
    var textColor1: Int? = null,
    var textColor2: Int? = null,
    var mediaMaxHeight: Int = 300,
    var textSize: Int = 48,
    var preloadAds: Boolean = false,
    var autoRefresh: Boolean = true,
    var contentURL: String?,
    var neighbourContentURL: List<String>?,
    var buttonColor: Int = Color.parseColor("#000000")
) {
}
