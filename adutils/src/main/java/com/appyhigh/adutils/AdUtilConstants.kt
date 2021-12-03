package com.appyhigh.adutils

import android.widget.LinearLayout

object AdUtilConstants {

    enum class BannerAdSize {
        BANNER,
        LARGE_BANNER,
        MEDIUM_RECTANGLE
    }

    val bannerAdLifeCycleHashMap = HashMap<LinearLayout, BannerAdItem>()
    val nativeAdLifeCycleHashMap = HashMap<LinearLayout, NativeAdItem>()

}