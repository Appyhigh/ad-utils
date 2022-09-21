package com.appyhigh.adutils

import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.LoadAdError

interface InterstitialCallback {
    fun moveNext()
    fun moveNext(error: LoadAdError)
    fun moveNext(error: AdError)
}