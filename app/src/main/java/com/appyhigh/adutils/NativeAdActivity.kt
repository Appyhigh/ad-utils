package com.appyhigh.adutils

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.appyhigh.adutils.callbacks.NativeAdLoadCallback
import com.appyhigh.adutils.databinding.ActivityNativeAdBinding
import com.google.android.gms.ads.LoadAdError

class NativeAdActivity : AppCompatActivity() {
    lateinit var binding: ActivityNativeAdBinding

    override fun onPause() {
        super.onPause()
        AdSdk.removeNativeAdFromService(binding.llRoot4)
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNativeAdBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d("RemoteCoonfig", RemoteConfigUtils.getNativeAdTypeId())
        /*AdSdk.loadNativeAd(
            lifecycle = lifecycle,
            adUnit = "ca-app-pub-3940256099942544/2247696110",
            viewGroup = binding.llRoot,
            adType = AdSdk.ADType.DEFAULT_AD,
            callback = nativeAdLoadCallback("1"),
            background = null, textColor1 = null, textColor2 = null,
        )*/
        val adUnit =
            "ca-app-pub-3940256099942544/2247696110"
        val adUnit2 =
            "ca-app-pub-3940256099942544/2247696110"
        val adUnit3 =
            "ca-app-pub-3940256099942544/2247696110"
        /*AdSdk.loadNativeAdFromService(
            context = applicationContext,
            layoutInflater = layoutInflater,
            adUnit = adUnit,
            viewGroup = binding.llRoot1,
            adType = AdSdk.ADType.BIGV3,
            background = null, textColor1 = null, textColor2 = null,
            nativeAdLoadCallback = null,
            preloadAds = true,
            autoRefresh = true
        )*/

        AdSdk.loadNativeAd(
            this@NativeAdActivity,
            lifecycle = lifecycle,
            adUnit = adUnit,
            adName = "util_native_grid",
            viewGroup = binding.llRoot8,
            adType = AdSdk.ADType.GRID_AD,
            callback = nativeAdLoadCallback("6"),
            background = null, textColor1 = null, textColor2 = null,
            mediaMaxHeight = 200,
            loadTimeOut = 4000
        )

        AdSdk.loadNativeAd(
            this@NativeAdActivity,
            lifecycle = lifecycle,
            adUnit = adUnit,
            adName = "util_native_grid",
            viewGroup = binding.llRoot9,
            adType = AdSdk.ADType.GRID_AD,
            callback = nativeAdLoadCallback("6"),
            background = null, textColor1 = null, textColor2 = null,
            mediaMaxHeight = 200,
            loadTimeOut = 4000
        )

        AdSdk.loadNativeAd(
            this@NativeAdActivity,
            lifecycle = lifecycle,
            adUnit = adUnit2,
            adName = "util_native_default",
            viewGroup = binding.llRoot2,
            adType = AdSdk.ADType.DEFAULT_AD,
            callback = nativeAdLoadCallback("3"),
            background = null, textColor1 = null, textColor2 = null,
            loadTimeOut = 4000
        )
        AdSdk.loadNativeAd(
            this@NativeAdActivity,
            lifecycle = lifecycle,
            adUnit = adUnit,
            adName = "util_native_small",
            viewGroup = binding.llRoot3,
            adType = AdSdk.ADType.SMALL,
            callback = nativeAdLoadCallback("4"),
            background = null, textColor1 = null, textColor2 = null,
            loadTimeOut = 4000
        )
        AdSdk.loadNativeAd(
            this@NativeAdActivity,
            lifecycle = lifecycle,
            adUnit = adUnit2,
            adName = "util_native_medium",
            viewGroup = binding.llRoot4,
            adType = AdSdk.ADType.MEDIUM,
            callback = nativeAdLoadCallback("5"),
            background = null, textColor1 = null, textColor2 = null,
            loadTimeOut = 4000
        )

        AdSdk.loadNativeAd(
            this@NativeAdActivity,
            lifecycle = lifecycle,
            adUnit = adUnit,
            adName = "util_native_bigv1",
            viewGroup = binding.llRoot5,
            adType = AdSdk.ADType.BIGV1,
            callback = nativeAdLoadCallback("6"),
            background = null, textColor1 = null, textColor2 = null,
            loadTimeOut = 4000,
            mediaMaxHeight = 500
        )

        AdSdk.loadNativeAd(
            this@NativeAdActivity,
            lifecycle = lifecycle,
            adUnit = adUnit,
            adName = "util_native_bigv2",
            viewGroup = binding.llRoot6,
            adType = AdSdk.ADType.BIGV2,
            callback = nativeAdLoadCallback("6"),
            background = null, textColor1 = null, textColor2 = null,
            loadTimeOut = 4000,
            mediaMaxHeight = 500,
        )

        AdSdk.loadNativeAd(
            this@NativeAdActivity,
            lifecycle = lifecycle,
            adUnit = adUnit,
            adName = "util_native_bigv3",
            viewGroup = binding.llRoot7,
            adType = AdSdk.ADType.BIGV3,
            callback = nativeAdLoadCallback("6"),
            background = null, textColor1 = null, textColor2 = null,
            loadTimeOut = 4000
        )



        binding.refresh.setOnClickListener {
            AdSdk.loadNativeAdFromSrvs(
                context = applicationContext,
                lifecycle = lifecycle,
                layoutInflater = layoutInflater,
                adUnit = adUnit,
                adName = "util_native_preload",
                viewGroup = binding.llRoot1,
                adType = AdSdk.ADType.MEDIUM,
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
                adName = "util_native_preload1",
                viewGroup = binding.llRoot,
                adType = AdSdk.ADType.MEDIUM,
                background = null, textColor1 = null, textColor2 = null,
                nativeAdLoadCallback = null,
                preloadAds = true,
                autoRefresh = true,
                loadTimeOut = 4000
            )
        }

        binding.removeAds.setOnClickListener {
            AdSdk.removeNativeAdFromService(binding.llRoot4)
        }
        binding.disableRefresh.setOnClickListener {
            AdSdk.disableNativeRefresh()
        }
        binding.enableRefresh.setOnClickListener {
            AdSdk.enableNativeRefresh()
        }
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