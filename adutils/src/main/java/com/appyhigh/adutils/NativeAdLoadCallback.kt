package com.appyhigh.adutils

interface NativeAdLoadCallback {
    fun onAdLoaded()
    fun onAdFailed()
    fun onAdClicked()
}