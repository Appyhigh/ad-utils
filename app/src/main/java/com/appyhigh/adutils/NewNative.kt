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
            "ca-app-pub-3940256099942544/2247696110"

        AdSdk.loadNativeAd(
            this@NewNative,
            lifecycle = lifecycle,
            adUnit = adUnit2,
            adName = "util_native_default",
            viewGroup = binding.llRoot,
            adType = AdSdk.ADType.DYNAMIC,
            callback = nativeAdLoadCallback("3"),
            background = null, textColor1 = null, textColor2 = null,
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