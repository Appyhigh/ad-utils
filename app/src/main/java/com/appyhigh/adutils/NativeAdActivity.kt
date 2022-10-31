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
            DynamicsAds.getDynamicAdsId("ca-app-pub-3940256099942544/2247696110", "admob_banner")
        val adUnit2 =
            DynamicsAds.getDynamicAdsId("ca-app-pub-3940256099942544/2247696110", "admob_native")
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
            lifecycle = lifecycle,
            adUnit = adUnit2,
            viewGroup = binding.llRoot2,
            adType = AdSdk.ADType.MEDIUM,
            callback = nativeAdLoadCallback("3"),
            background = null, textColor1 = null, textColor2 = null,
        )
        AdSdk.loadNativeAd(
            lifecycle = lifecycle,
            adUnit = adUnit,
            viewGroup = binding.llRoot3,
            adType = AdSdk.ADType.BIGV1,
            callback = nativeAdLoadCallback("4"),
            background = null, textColor1 = null, textColor2 = null,
        )
        AdSdk.loadNativeAd(
            lifecycle = lifecycle,
            adUnit = adUnit2,
            viewGroup = binding.llRoot4,
            adType = AdSdk.ADType.BIGV2,
            callback = nativeAdLoadCallback("5"),
            background = null, textColor1 = null, textColor2 = null,
        )

        AdSdk.loadNativeAd(
            lifecycle = lifecycle,
            adUnit = adUnit,
            viewGroup = binding.llRoot5,
            adType = AdSdk.ADType.BIGV3,
            callback = nativeAdLoadCallback("6"),
            background = null, textColor1 = null, textColor2 = null,
        )

        binding.refresh.setOnClickListener {
            AdSdk.loadNativeAdFromService(
                context = applicationContext,
                layoutInflater = layoutInflater,
                adUnit = adUnit,
                viewGroup = binding.llRoot1,
                adType = AdSdk.ADType.BIGV3,
                background = null, textColor1 = null, textColor2 = null,
                nativeAdLoadCallback = null,
                preloadAds = true,
                autoRefresh = true
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

        override fun onAdFailed(adError: LoadAdError) {
            super.onAdFailed(adError)
            Log.d("AdSdk", "onAdFailed: $s ${adError.message}")
        }

        override fun onAdClicked() {
            super.onAdClicked()
            Log.d("AdSdk", "onAdClicked: $s")
        }
    }

}