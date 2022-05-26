package com.appyhigh.adutils

import android.app.Application

class MyApp : Application() {

    override fun onCreate() {
        super.onCreate()
        RemoteConfigUtils.init()
        AdSdk.initialize(this, bannerRefreshTimer = 0, nativeRefreshTimer = 0)
        AdSdk.attachAppOpenAdManager("ca-app-pub-3940256099942544/3419835294", null, 5000)
    }

}