package com.appyhigh.adutils

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.appyhigh.adutils.databinding.ActivityNativeAdBinding
import com.google.android.gms.ads.LoadAdError

class NativeAdActivity : AppCompatActivity() {
    lateinit var binding: ActivityNativeAdBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNativeAdBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d("RemoteCoonfig", RemoteConfigUtils.getNativeAdTypeId())
        AdSdk.loadNativeAd(
            lifecycle,
            "ca-app-pub-3940256099942544/2247696110",
            binding.llRoot,
            nativeAdCallBack,
            "1"
        )
        AdSdk.loadNativeAd(
            lifecycle,
            "ca-app-pub-3940256099942544/2247696110",
            binding.llRoot1,
            nativeAdCallBack,
            "2"
        )
        AdSdk.loadNativeAd(
            lifecycle,
            "ca-app-pub-3940256099942544/2247696110",
            binding.llRoot2,
            nativeAdCallBack,
            "3"
        )
        AdSdk.loadNativeAd(
            lifecycle,
            "ca-app-pub-3940256099942544/2247696110",
            binding.llRoot3,
            nativeAdCallBack,
            "4"
        )
        AdSdk.loadNativeAd(
            lifecycle,
            "ca-app-pub-3940256099942544/2247696110",
            binding.llRoot4,
            nativeAdCallBack,
            "5"
        )
    }

    private val nativeAdCallBack = object : NativeAdLoadCallback() {
        override fun onAdLoaded() {
            Log.d("NativeAdLoadCallback", "onAdLoaded")
        }

        override fun onAdFailed(adError: LoadAdError) {
            Log.d("NativeAdLoadCallback", "onAdFailed")
        }

        override fun onAdClicked() {
            Log.d("NativeAdLoadCallback", "onAdClicked")
        }
    }
}