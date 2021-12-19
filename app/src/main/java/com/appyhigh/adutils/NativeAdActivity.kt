package com.appyhigh.adutils

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.appyhigh.adutils.databinding.ActivityNativeAdBinding
import com.google.android.gms.ads.LoadAdError

class NativeAdActivity : AppCompatActivity() {
    lateinit var binding: ActivityNativeAdBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNativeAdBinding.inflate(layoutInflater)
        setContentView(binding.root)
        AdSdk.loadNativeAd(
            lifecycle,
            "ca-app-pub-3940256099942544/2247696110",
            binding.llRoot,
            nativeAdCallBack,
            R.layout.ad_item_big
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