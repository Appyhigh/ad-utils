package com.appyhigh.adutils.interfaces

import com.google.android.gms.ads.nativead.NativeAd

interface NativeInternalCallback {
    fun onSuccess(nativeAd: NativeAd?)
    fun onFailure()
}