package com.appyhigh.adutils.callbacks

import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.LoadAdError

interface InterstitialCallback {
    fun moveNext()
    fun moveNext(error: LoadAdError)
    fun moveNext(error: AdError)
}