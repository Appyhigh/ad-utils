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
        AdSdk.loadNativeAd(
            lifecycle = lifecycle,
            adUnit = "ca-app-pub-3940256099942544/2247696110",
            viewGroup = binding.llRoot,
            adType = AdSdk.ADType.DEFAULT_AD,
            callback = object : NativeAdLoadCallback() {
                override fun onAdLoaded() {
                }

                override fun onAdFailed(adError: LoadAdError) {
                    super.onAdFailed(adError)
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                }
            },
            background = null, textColor1 = null, textColor2 = null,
        )
        AdSdk.loadNativeAd(
            lifecycle = lifecycle,
            adUnit = "ca-app-pub-3940256099942544/2247696110",
            viewGroup = binding.llRoot1,
            adType = AdSdk.ADType.SMALL,
            callback = object : NativeAdLoadCallback() {
                override fun onAdLoaded() {
                }

                override fun onAdFailed(adError: LoadAdError) {
                    super.onAdFailed(adError)
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                }
            },
            background = null, textColor1 = null, textColor2 = null,
        )
        AdSdk.loadNativeAd(
            lifecycle = lifecycle,
            adUnit = "ca-app-pub-3940256099942544/2247696110",
            viewGroup = binding.llRoot2,
            adType = AdSdk.ADType.MEDIUM,
            callback = object : NativeAdLoadCallback() {
                override fun onAdLoaded() {
                }

                override fun onAdFailed(adError: LoadAdError) {
                    super.onAdFailed(adError)
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                }
            },
            background = null, textColor1 = null, textColor2 = null,
        )
        AdSdk.loadNativeAd(
            lifecycle = lifecycle,
            adUnit = "ca-app-pub-3940256099942544/2247696110",
            viewGroup = binding.llRoot3,
            adType = AdSdk.ADType.BIGV3,
            callback = object : NativeAdLoadCallback() {
                override fun onAdLoaded() {
                }

                override fun onAdFailed(adError: LoadAdError) {
                    super.onAdFailed(adError)
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                }
            },
            background = null, textColor1 = null, textColor2 = null,
        )
        AdSdk.loadNativeAd(
            lifecycle = lifecycle,
            adUnit = "ca-app-pub-3940256099942544/2247696110",
            viewGroup = binding.llRoot4,
            adType = AdSdk.ADType.BIGV1,
            callback = object : NativeAdLoadCallback() {
                override fun onAdLoaded() {
                }

                override fun onAdFailed(adError: LoadAdError) {
                    super.onAdFailed(adError)
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                }
            },
            background = null, textColor1 = null, textColor2 = null,
        )

        AdSdk.loadNativeAd(
            lifecycle = lifecycle,
            adUnit = "ca-app-pub-3940256099942544/2247696110",
            viewGroup = binding.llRoot5,
            adType = AdSdk.ADType.BIGV2,
            callback = object : NativeAdLoadCallback() {
                override fun onAdLoaded() {
                }

                override fun onAdFailed(adError: LoadAdError) {
                    super.onAdFailed(adError)
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                }
            },
            background = null, textColor1 = null, textColor2 = null,
        )

/*
        AdSdk.loadNativeAdFromService(
            layoutInflater,
            applicationContext,
            "ca-app-pub-3940256099942544/2247696110",
            binding.llRoot1,
            nativeAdLoadCallback = null,
            AdSdk.ADType.DEFAULT_NATIVE_SMALL,
            background = null, textColor1 = null, textColor2 = null,
            loadingTextSize = 48,
            mediaMaxHeight = 150
        )
*/
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

}