package com.appyhigh.adutils

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
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
            lifecycle,
            binding.typeOne,
            bannerAdUnit,
            AdSize.BANNER,
            bannerAdLoadCallback
        )

        AdSdk.loadBannerAd(
            lifecycle,
            binding.typeTwo,
            bannerAdUnit,
            AdSize.LARGE_BANNER,
            bannerAdLoadCallback
        )

        AdSdk.loadBannerAd(
            lifecycle,
            binding.typeThree,
            bannerAdUnit,
            AdSize.MEDIUM_RECTANGLE,
            bannerAdLoadCallback
        )
    }

    private val bannerAdLoadCallback = object : BannerAdLoadCallback {
        override fun onAdLoaded() {
            Log.d("BannerAdLoadCallback", "onAdLoaded")
        }

        override fun onAdFailedToLoad(adError: LoadAdError) {
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