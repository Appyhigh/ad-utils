package com.appyhigh.adutils.callbacks

import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.LoadAdError

interface RewardedCallback {
    fun moveNext(rewarded: Boolean = false)
    fun moveNext(error: LoadAdError)
    fun moveNext(error: AdError)
    fun adNotLoaded()
}