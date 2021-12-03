package com.appyhigh.adutils

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.appyhigh.adutils.databinding.ActivityBannerAdBinding
import com.appyhigh.adutils.databinding.ActivityNativeAdBinding

class NativeAdActivity : AppCompatActivity() {
    lateinit var binding: ActivityNativeAdBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNativeAdBinding.inflate(layoutInflater)
        setContentView(binding.root)
        AdSdk().loadNativeAd(
            "ca-app-pub-3940256099942544/2247696110",
            binding.llRoot,
            nativeAdCallBack,
            R.layout.ad_item_big
        )
    }
    private val nativeAdCallBack = object :NativeAdLoadCallback{
        override fun onAdLoaded() {
            Log.d("NativeAdLoadCallback","onAdLoaded")
        }

        override fun onAdFailed() {
            Log.d("NativeAdLoadCallback","onAdFailed")
        }

        override fun onAdClicked() {
            Log.d("NativeAdLoadCallback","onAdClicked")
        }
    }
}