package com.appyhigh.adutils

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.app.AppCompatActivity
import com.appyhigh.adutils.callbacks.InterstitialAdUtilLoadCallback
import com.appyhigh.adutils.callbacks.RewardedAdUtilLoadCallback
import com.appyhigh.adutils.callbacks.RewardedCallback
import com.appyhigh.adutils.databinding.ActivityMainBinding
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.rewarded.RewardedAd

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null

    override fun onBackPressed() {
//      super.onBackPressed()
        if (binding.exitAd.visibility == VISIBLE) {
            binding.exitAd.visibility = GONE
        } else {
            binding.exitAd.visibility = VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        AdSdk.preloadAds(layoutInflater, applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        AdSdk.loadNPAForm(
            "https://www.google.com",
            this,
            "pub-3940256099942544",
            "182790353ADD7F5B71982136E0704453"
        )
        binding.btnBannerAd.setOnClickListener {
            startActivity(Intent(this, BannerAdActivity::class.java))
        }
        binding.btnInterstitialAd.setOnClickListener {
            interstitialAd?.show(this)
        }

        binding.btnRewardedAd.setOnClickListener {
            AdSdk.showRewardedAdsAfterWait(
                this,
                3500,
                "ca-app-pub-3940256099942544/5224354917",
                object : RewardedCallback {
                    override fun moveNext() {

                    }

                    override fun moveNext(error: LoadAdError) {
                    }

                    override fun moveNext(error: AdError) {
                    }

                    override fun adNotLoaded() {

                    }
                }
            ) { }

        }

        binding.btnNativeAd.setOnClickListener {
            startActivity(Intent(this, NativeAdActivity::class.java))
        }

        if (BuildConfig.DEBUG) {
//            binding.btnNativeAd.performClick()
        }
        loadInterstitialAd(this)
        loadRewardedAd(this)
        AdSdk.preLoadRewardedAd(this, "ca-app-pub-3940256099942544/5224354917")
    }

    private val mInterstitialAdUtilCallback = object : InterstitialAdUtilLoadCallback {
        override fun onAdFailedToLoad(adError: LoadAdError, ad: InterstitialAd?) {
            interstitialAd = ad
        }

        override fun onAdLoaded(ad: InterstitialAd?) {
            interstitialAd = ad
        }

        override fun onAdImpression() {
            super.onAdImpression()
            if (BuildConfig.DEBUG) {
            }
        }

        override fun onAdDismissedFullScreenContent() {
            /**
             * Comment this if you want the ad to load just once
             * Uncomment this to load ad again once shown
             */
            loadInterstitialAd(this@MainActivity)
        }

        override fun onAdFailedToShowFullScreenContent(adError: AdError?) {}

        override fun onAdShowedFullScreenContent() {
            interstitialAd = null
        }
    }

    private val mRewardedAdUtilCallback = object : RewardedAdUtilLoadCallback {
        override fun onAdFailedToLoad(adError: LoadAdError, ad: RewardedAd?) {
            rewardedAd = ad
        }

        override fun onAdLoaded(ad: RewardedAd?) {
            rewardedAd = ad
        }

        override fun onAdDismissedFullScreenContent() {
            /**
             * Comment this if you want the ad to load just once
             * Uncomment this to load ad again once shown
             */
            loadRewardedAd(this@MainActivity)
        }

        override fun onAdFailedToShowFullScreenContent(adError: AdError?) {}

        override fun onAdShowedFullScreenContent() {
            rewardedAd = null
        }

    }

    private fun loadInterstitialAd(activity: Activity) {
        AdSdk.loadInterstitialAd(
            "ca-app-pub-3940256099942544/1033173712",
            mInterstitialAdUtilCallback
        )
    }

    private fun loadRewardedAd(activity: Activity) {
        AdSdk.loadRewardedAd(
            activity,
            "ca-app-pub-3940256099942544/5224354917",
            mRewardedAdUtilCallback
        )
    }
}