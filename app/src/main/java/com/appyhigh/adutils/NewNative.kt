package com.appyhigh.adutils

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.appyhigh.adutils.callbacks.NativeAdLoadCallback
import com.appyhigh.adutils.databinding.ActivityNativeAdBinding
import com.appyhigh.adutils.databinding.ActivityNewNativeBinding
import com.google.android.gms.ads.LoadAdError

class NewNative : AppCompatActivity() {
    lateinit var binding: ActivityNewNativeBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewNativeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adUnit =
            "ca-app-pub-3940256099942544/2247696110"
        val adUnit2 =
            "ca-app-pub-3940256099942544/2247696110"
        val adUnit3 =
            "ca-app-pub-3940256099942544/1044960115"

        AdSdk.loadNativeAd(
            this@NewNative,
            lifecycle = lifecycle,
            adUnit = adUnit3,
            adName = "util_native_preload2",
            viewGroup = binding.llRoot,
            adType = AdSdk.ADType.DYNAMIC,
            callback = nativeAdLoadCallback("3"),
            background = null, textColor1 = null, textColor2 = null,
            loadTimeOut = 20000
        )

        AdSdk.loadNativeAd(
            this@NewNative,
            lifecycle = lifecycle,
            adUnit = adUnit3,
            adName = "util_native_preload2",
            viewGroup = binding.llRoot1,
            adType = AdSdk.ADType.DYNAMIC,
            callback = nativeAdLoadCallback("3"),
            background = null, textColor1 = null, textColor2 = null,
            loadTimeOut = 20000
        )

        AdSdk.loadNativeAd(
            this@NewNative,
            lifecycle = lifecycle,
            adUnit = adUnit3,
            adName = "util_native_preload2",
            viewGroup = binding.llRoot2,
            adType = AdSdk.ADType.DYNAMIC,
            callback = nativeAdLoadCallback("3"),
            background = null, textColor1 = null, textColor2 = null,
            loadTimeOut = 20000
        )

//        AdSdk.loadNativeAd(
//            this@NewNative,
//            lifecycle = lifecycle,
//            adUnit = adUnit3,
//            adName = "util_native_default",
//            viewGroup = binding.llRoot3,
//            adType = AdSdk.ADType.DYNAMIC,
//            callback = nativeAdLoadCallback("3"),
//            background = null, textColor1 = null, textColor2 = null,
//            loadTimeOut = 20000
//        )
//
//        AdSdk.loadNativeAd(
//            this@NewNative,
//            lifecycle = lifecycle,
//            adUnit = adUnit3,
//            adName = "util_native_default",
//            viewGroup = binding.llRoot4,
//            adType = AdSdk.ADType.DYNAMIC,
//            callback = nativeAdLoadCallback("3"),
//            background = null, textColor1 = null, textColor2 = null,
//            loadTimeOut = 20000
//        )

        AdSdk.loadNativeAdFromSrvs(
            context = applicationContext,
            lifecycle = lifecycle,
            layoutInflater = layoutInflater,
            adUnit = adUnit,
            adName = "util_native_preload2",
            viewGroup = binding.llRoot3,
            adType = AdSdk.ADType.DYNAMIC,
            background = null, textColor1 = null, textColor2 = null,
            nativeAdLoadCallback = null,
            preloadAds = true,
            autoRefresh = true,
            loadTimeOut = 4000
        )

        AdSdk.loadNativeAdFromSrvs(
            context = applicationContext,
            lifecycle = lifecycle,
            layoutInflater = layoutInflater,
            adUnit = adUnit,
            adName = "util_native_preload2",
            viewGroup = binding.llRoot4,
            adType = AdSdk.ADType.DYNAMIC,
            background = null, textColor1 = null, textColor2 = null,
            nativeAdLoadCallback = null,
            preloadAds = true,
            autoRefresh = true,
            loadTimeOut = 4000
        )
    }

    private fun nativeAdLoadCallback(s: String) = object : NativeAdLoadCallback() {
        override fun onAdLoaded() {
            Log.d("AdSdk", "onAdLoaded: $s ")
        }

        override fun onAdFailed(adError: LoadAdError?) {
            super.onAdFailed(adError)
//            Log.d("AdSdk", "onAdFailed: $s ${adError.message}")
        }

        override fun onAdClicked() {
            super.onAdClicked()
            Log.d("AdSdk", "onAdClicked: $s")
        }

        override fun onContextFailed() {
            super.onContextFailed()
            Log.d("AdSdk", "onContextFailed: $s")
        }
    }
}