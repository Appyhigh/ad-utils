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
            this,
            lifecycle,
            "ca-app-pub-3940256099942544/2247696110",
            binding.llRoot,
            nativeAdCallBack,
            AdSdk.ADType.SMALLEST, null, null, null,
            Color.BLACK
        )

        AdSdk.loadNativeAd(
            this,
            lifecycle,
            "ca-app-pub-3940256099942544/2247696110",
            binding.llRoot1,
            nativeAdCallBack,
            AdSdk.ADType.SMALLER, null, null, null
        )
        AdSdk.loadNativeAd(
            this,
            lifecycle,
            "ca-app-pub-3940256099942544/2247696110",
            binding.llRoot2,
            nativeAdCallBack,
            AdSdk.ADType.SEMIMEDIUM, null, null, null
        )
        AdSdk.loadNativeAd(
            this,
            lifecycle,
            "ca-app-pub-3940256099942544/2247696110",
            binding.llRoot3,
            nativeAdCallBack,
            AdSdk.ADType.MEDIUM, null, null, null
        )
        AdSdk.loadNativeAd(
            this,
            lifecycle,
            "ca-app-pub-3940256099942544/2247696110",
            binding.llRoot4,
            nativeAdCallBack,
            AdSdk.ADType.DEFAULT_NATIVE_SMALL, null, null, null, maxHeight = 150
        )*/

/*
        AdSdk.loadNativeAd(
            activity = this,
            lifecycle = lifecycle,
            adUnit = "ca-app-pub-3940256099942544/2247696110",
            viewGroup = binding.llRoot4,
            mediaMaxHeight = 150,
            loadingTextSize = 24,
            adType = AdSdk.ADType.DEFAULT_NATIVE_SMALL,
            background = null,
            callback = null,
            textColor1 = null,
            textColor2 = null
        )
*/

        /*AdSdk.loadNativeAd(
            lifecycle,
            "ca-app-pub-3940256099942544/2247696110",
            binding.llRoot,
            nativeAdCallBack,
            AdSdk.ADType.MEDIUM, null, null, null, mediaMaxHeight = 100,
        )
        AdSdk.loadNativeAd(
            lifecycle,
            "ca-app-pub-3940256099942544/2247696110",
            binding.llRoot1,
            nativeAdCallBack,
            AdSdk.ADType.BIG, null, null, null, mediaMaxHeight = 100
        )*/
        AdSdk.loadNativeAd(
            lifecycle,
            "ca-app-pub-3940256099942544/2247696110",
            binding.llRoot2,
            nativeAdCallBack,
            AdSdk.ADType.DEFAULT_NATIVE_SMALL, null, null, null,
        )
/*
        AdSdk.loadNativeAd(
            lifecycle,
            "ca-app-pub-3940256099942544/2247696110",
            binding.llRoot3,
            nativeAdCallBack,
            AdSdk.ADType.SMALLER, null, null, null, mediaMaxHeight = 100
        )
*/
        /*AdSdk.loadNativeAd(
            lifecycle,
            DynamicsAds.getDynamicAdsId("ca-app-pub-3940256099942544/2247696110", "ADNAME"),
            binding.llRoot4,
            nativeAdCallBack,
            AdSdk.ADType.SMALLEST, null, null, null, mediaMaxHeight = 100,
        )*/

/*
        AdSdk.loadNativeAdFromService(
            layoutInflater,
            applicationContext,
            adUnit = "ca-app-pub-3940256099942544/2247696110",
            viewGroup = binding.llRoot4,
            mediaMaxHeight = 150,
            loadingTextSize = 24,
            adType = AdSdk.ADType.SEMIMEDIUM,
            background = null,
            nativeAdLoadCallback = null,
            textColor1 = null,
            textColor2 = null,
            autoRefresh = true
        )
*/

        binding.removeAds.setOnClickListener {
//            AdSdk.disableRefresh()
            AdSdk.removeNativeAdFromService(binding.llRoot4)
        }
        binding.disableRefresh.setOnClickListener {
            AdSdk.disableNativeRefresh()
        }
        binding.enableRefresh.setOnClickListener {
            AdSdk.enableNativeRefresh()
        }

        /*AdSdk.loadNativeAd(
            this,
            lifecycle,
            "ca-app-pub-3940256099942544/2247696110",
            binding.llRoot4,
            nativeAdCallBack,
            AdSdk.ADType.BIG,
            null,
            null,
            null,
            mediaMaxHeight = 600,
            loadingTextSize = 24
        )*/


/*
        AdSdk.loadNativeAd(
            this,
            lifecycle,
            "ca-app-pub-3940256099942544/2247696110",
            binding.adView,
            nativeAdCallBack,
            AdSdk.ADType.DEFAULT_NATIVE_SMALL, null, null, null, maxHeight = 200
        )
*/
    }

    private val nativeAdCallBack = object : NativeAdLoadCallback() {
        override fun onAdLoaded() {

        }

        override fun onAdFailed(adError: LoadAdError) {

        }

        override fun onAdClicked() {

        }
    }
}