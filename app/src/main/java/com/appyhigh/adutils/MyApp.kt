package com.appyhigh.adutils

import android.app.Application

class MyApp : Application() {

    override fun onCreate() {
        super.onCreate()
        RemoteConfigUtils.init()
//        AdSdk.initialize(this, bannerRefreshTimer = 0, nativeRefreshTimer = 0)
    }

}