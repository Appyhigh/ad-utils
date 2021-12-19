package com.appyhigh.adutils

object AdUtilConstants {

    enum class BannerAdSize {
        BANNER,
        LARGE_BANNER,
        MEDIUM_RECTANGLE
    }

    val bannerAdLifeCycleHashMap = HashMap<Long, BannerAdItem>()
    val nativeAdLifeCycleHashMap = HashMap<Long, NativeAdItem>()

}