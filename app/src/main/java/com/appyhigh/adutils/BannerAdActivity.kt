package com.appyhigh.adutils

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.appyhigh.adutils.callbacks.BannerAdLoadCallback
import com.appyhigh.adutils.databinding.ActivityBannerAdBinding
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.LoadAdError

class BannerAdActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBannerAdBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBannerAdBinding.inflate(layoutInflater)
        setContentView(binding.root)

        AdSdk.loadBannerAd(
            this,
            lifecycle,
            binding.typeOne,
            "ca-app-pub-3940256099942544/6300978111",
            "util_banner",
            AdSize.BANNER,
            bannerAdLoadCallback,
            background = Color.DKGRAY,
            textColor1 = Color.WHITE,
            loadingTextSize = 32
        )

        AdSdk.loadBannerAd(
            this,
            lifecycle,
            binding.typeTwo,
            "ca-app-pub-3940256099942544/6300978111",
            "util_banner1",
            AdSize.LARGE_BANNER,
            bannerAdLoadCallback,
            background = Color.DKGRAY,
            textColor1 = Color.WHITE,
            loadingTextSize = 32
        )

        AdSdk.loadBannerAd(
            this,
            lifecycle,
            binding.typeThree,
            "ca-app-pub-3940256099942544/6300978111",
            "util_banner2",
            AdSize.MEDIUM_RECTANGLE,
            bannerAdLoadCallback,
            background = Color.DKGRAY,
            textColor1 = Color.WHITE,
            loadingTextSize = 32
        )
    }

    private val bannerAdLoadCallback = object : BannerAdLoadCallback {
        override fun onAdLoaded() {
            Log.d("BannerAdLoadCallback", "onAdLoaded")
        }

        override fun onAdFailedToLoad(adError: LoadAdError?) {
            Log.d("BannerAdLoadCallback", "onAdFailedToLoad")
        }

        override fun onAdOpened() {
            Log.d("BannerAdLoadCallback", "onAdOpened")
        }

        override fun onAdClicked() {
            Log.d("BannerAdLoadCallback", "onAdClicked")
        }

        override fun onAdClosed() {
            Log.d("BannerAdLoadCallback", "onAdClosed")
        }

    }

    companion object {
        private val bannerAdUnit = "ca-app-pub-3940256099942544/6300978111"
    }
}