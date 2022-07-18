package com.appyhigh.adutils

import com.appyhigh.adutils.models.BannerAdItem
import com.appyhigh.adutils.models.NativeAdItem
import com.appyhigh.adutils.models.NativeAdItemService
import com.appyhigh.adutils.models.PreloadNativeAds

object AdUtilConstants {

    enum class BannerAdSize {
        BANNER,
        LARGE_BANNER,
        MEDIUM_RECTANGLE
    }

    val bannerAdLifeCycleHashMap = HashMap<Long, BannerAdItem>()
    val nativeAdLifeCycleHashMap = HashMap<Long, NativeAdItem>()
    var preloadNativeAdList: HashMap<String, PreloadNativeAds>? = null
    val nativeAdLifeCycleServiceHashMap = HashMap<Long, NativeAdItemService>()

}