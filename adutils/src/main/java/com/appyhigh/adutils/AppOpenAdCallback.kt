package com.appyhigh.adutils

interface AppOpenAdCallback {
    fun onInitSuccess(manager: AppOpenManager)
    fun onAdLoaded()
    fun onAdClosed()
}