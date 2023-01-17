package com.appyhigh.adutils

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.app.AppCompatActivity
import com.appyhigh.adutils.callbacks.*
import com.appyhigh.adutils.databinding.ActivityMainBinding
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.admanager.AdManagerInterstitialAd
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd

class MainActivity : AppCompatActivity(), VersionCallback {
    private lateinit var binding: ActivityMainBinding
    private var interstitialAd: InterstitialAd? = null
    private var adManagerAd: AdManagerInterstitialAd? = null
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
        AdSdk.initVersionController(
            this@MainActivity,
            90,
            binding.root,
            this
        )
        if (BuildConfig.DEBUG) {
            AdSdk.attachAppOpenAdManager(
                "ca-app-pub-3940256099942544/3419835294",
                "util_appopen",
                object : AppOpenAdCallback() {
                    override fun onAdFailedToLoad(loadAdError: LoadAdError?) {
                        super.onAdFailedToLoad(loadAdError)
                        Log.d("Appopen", "onAdFailedToLoad: "+loadAdError?.message)
                    }
                                         },
                1000,
                false,
                loadTimeOut = 4000,
            )
        } else {
            AdSdk.attachAppOpenAdManager("ca-app-pub-3940256099942544/3419835294",
                "util_appopen",
                object : AppOpenAdCallback() {
                    override fun onAdFailedToLoad(loadAdError: LoadAdError?) {
                        super.onAdFailedToLoad(loadAdError)
                        Log.d("Appopen", "onAdFailedToLoad: "+loadAdError?.message)
                    }
                },
            loadTimeOut = 4000
            )
        }

        binding.btnBannerAd.setOnClickListener {
            startActivity(Intent(this, BannerAdActivity::class.java))
        }
        binding.btnInterstitialAd.setOnClickListener {
            AdSdk.showAvailableInterstitialAd(this,interstitialAd,adManagerAd)
        }

        binding.btnRewardedInterstitialAd.setOnClickListener {
            AdSdk.showRewardedIntersAd(this, "ca-app-pub-3940256099942544/5354046379","util_reward_interstitial", object : InterstitialCallback {
                override fun moveNext() {
                    Log.d("AdSDK", "moveNext: ")
                }

                override fun moveNext(error: LoadAdError) {
                    Log.d("AdSDK", "moveNext: 1 " + error.message)
                }

                override fun moveNext(error: AdError) {
                    Log.d("AdSDK", "moveNext: 2 " + error.message)
                }
            },
            loadTimeOut = 4000)
        }

        binding.btnRewardedAd.setOnClickListener {
            rewardedAd?.show(this,object :OnUserEarnedRewardListener{
                override fun onUserEarnedReward(p0: RewardItem) {
                    TODO("Not yet implemented")
                }
            })
        }

        binding.btnNativeAd.setOnClickListener {
            startActivity(Intent(this, NativeAdActivity::class.java))
        }

        if (BuildConfig.DEBUG) {
//            binding.btnNativeAd.performClick()
        }
//        DynamicsAds.listAllAds(this, "AdSdk")
        loadInterstitialAd(this)
        loadRewardedAd(this)
//        AdSdk.preLoadRewardedAd(this, "ca-app-pub-3940256099942544/5224354917")
    }

    private val mInterstitialAdUtilCallback = object : InterstitialAdUtilLoadCallback {
        override fun onAdFailedToLoad(adError: LoadAdError?, ad: InterstitialAd?) {
            interstitialAd = ad
        }

        override fun onAdLoaded(
            ad: InterstitialAd?,
            adManagerInterstitialAd: AdManagerInterstitialAd?
        ) {
            interstitialAd = ad
            adManagerAd = adManagerInterstitialAd
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
        override fun onAdFailedToLoad(adError: LoadAdError?, ad: RewardedAd?) {
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
            "util_interstitial",
            "ca-app-pub-3940256099942544/1033173712",
            mInterstitialAdUtilCallback,
            loadTimeOut = 4000
        )
    }

    private fun loadRewardedAd(activity: Activity) {
        AdSdk.loadRewardedAd(
            activity,
            "ca-app-pub-3940256099942544/5224354917",
            "util_rewarded",
            mRewardedAdUtilCallback,
            false,
            loadTimeOut = 4000
        )
    }

    override fun OnSoftUpdate() {
        TODO("Not yet implemented")
    }

    override fun OnHardUpdate() {
        TODO("Not yet implemented")
    }
}