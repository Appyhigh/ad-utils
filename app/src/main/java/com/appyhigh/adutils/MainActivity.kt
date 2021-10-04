package com.appyhigh.adutils

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.appyhigh.adutils.databinding.ActivityMainBinding
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var interstitialAd: InterstitialAd? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBannerAd.setOnClickListener {
            startActivity(Intent(this, BannerAdActivity::class.java))
        }
        binding.btnInterstitialAd.setOnClickListener {
            interstitialAd?.show(this)
        }
        binding.btnNativeAd.setOnClickListener {
            startActivity(Intent(this, NativeAdActivity::class.java))
        }
        loadInterstitialAd()
    }

    private val mInterstitialAdUtilCallback = object : InterstitialAdUtilLoadCallback {
        override fun onAdFailedToLoad(adError: LoadAdError, ad: InterstitialAd?) {
            interstitialAd = ad
        }

        override fun onAdLoaded(ad: InterstitialAd?) {
            interstitialAd = ad
        }

        override fun onAdDismissedFullScreenContent() {
            /**
             * Comment this if you want the ad to load just once
             * Uncomment this to load ad again once shown
             */
            loadInterstitialAd()
        }

        override fun onAdFailedToShowFullScreenContent(adError: AdError?) {}

        override fun onAdShowedFullScreenContent() {
            interstitialAd = null
        }
    }

    private fun loadInterstitialAd() {
        AdSdk().loadInterstitialAd(
            "ca-app-pub-3940256099942544/1033173712",
            mInterstitialAdUtilCallback
        )
    }
}