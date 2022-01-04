package com.appyhigh.adutils

import android.app.Application
import com.google.firebase.FirebaseApp

class MyApp : Application() {

    override fun onCreate() {
        super.onCreate()

        RemoteConfigUtils.init()
    }

}