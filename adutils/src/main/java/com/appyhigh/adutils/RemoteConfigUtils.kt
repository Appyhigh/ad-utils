package com.appyhigh.adutils

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings

object RemoteConfigUtils {
    private const val TAG = "RemoteConfigUtils"

    private const val NATIVE_AD_TYPE_KEY = "native_ad_layout_type"

    private lateinit var firebaseRemoteConfig: FirebaseRemoteConfig

    private val DEFAULTS: HashMap<String, Any> = hashMapOf(NATIVE_AD_TYPE_KEY to "1")

    fun init() {
        firebaseRemoteConfig = getFirebaseRemoteConfig()
    }

    private fun getFirebaseRemoteConfig(): FirebaseRemoteConfig {
        firebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
        firebaseRemoteConfig.setConfigSettingsAsync(
            FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds((60 * 60 * 24).toLong())
                .build()
        )
        firebaseRemoteConfig.fetchAndActivate().addOnCompleteListener {

        }
        return firebaseRemoteConfig
    }

    fun getNativeAdTypeId(): String = firebaseRemoteConfig.getString(NATIVE_AD_TYPE_KEY)

}