package com.appyhigh.adutils

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.appyhigh.adutils.databinding.ActivityNativeAdBinding
import com.appyhigh.adutils.databinding.ActivityNativeAdPreloadBinding

class NativeAdPreloadActivity : AppCompatActivity() {
    lateinit var binding: ActivityNativeAdPreloadBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNativeAdPreloadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adUnit =
            "ca-app-pub-3940256099942544/2247696110"
        val adUnit2 =
            "ca-app-pub-3940256099942544/2247696110"
        val adUnit3 =
            "ca-app-pub-3940256099942544/2247696110"

        AdSdk.loadNativeAdFromSrvs(
            context = applicationContext,
            lifecycle = lifecycle,
            layoutInflater = layoutInflater,
            adUnit = adUnit,
            adName = "util_native_preload",
            viewGroup = binding.llRoot,
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
            adName = "util_native_preload2",
            viewGroup = binding.llRoot2,
            adType = AdSdk.ADType.MEDIUM,
            background = null, textColor1 = null, textColor2 = null,
            nativeAdLoadCallback = null,
            preloadAds = true,
            autoRefresh = true,
            loadTimeOut = 4000
        )


    }
}