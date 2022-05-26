package com.appyhigh.adutils

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.appyhigh.adutils.callbacks.AppOpenAdLoadCallback
import com.appyhigh.adutils.callbacks.InterstitialAdUtilLoadCallback
import com.appyhigh.adutils.callbacks.SplashInterstitialCallback
import com.appyhigh.adutils.databinding.ActivitySplashBinding
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd

class SplashActivity : AppCompatActivity() {
    lateinit var binding: ActivitySplashBinding
    private var mInterstitialAd: InterstitialAd? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        loadInterstitial()
        AdSdk.loadAppOpenAd(this, "ca-app-pub-3940256099942544/3419835294", true,
            object : AppOpenAdLoadCallback() {

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    if(mInterstitialAd != null)
                        mInterstitialAd!!.show(this@SplashActivity)
                    else
                        continueToApp()
                }

                override fun onAdFailedToShow(adError: AdError) {
                    if(mInterstitialAd != null)
                        mInterstitialAd!!.show(this@SplashActivity)
                    else
                        continueToApp()
                }

                override fun onAdClosed() {
                    if(mInterstitialAd != null)
                        mInterstitialAd!!.show(this@SplashActivity)
                    else
                        continueToApp()
                }
            })
    }

    private fun loadInterstitial() {
        AdSdk.loadInterstitialAd("ca-app-pub-3940256099942544/1033173712",
            object : InterstitialAdUtilLoadCallback {
                override fun onAdFailedToLoad(adError: LoadAdError, ad: InterstitialAd?) {
                    mInterstitialAd = null
                }

                override fun onAdLoaded(ad: InterstitialAd?) {
                    mInterstitialAd = ad
                }

                override fun onAdDismissedFullScreenContent() {
                    continueToApp()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError?) {
                    continueToApp()
                }

                override fun onAdShowedFullScreenContent() {

                }
            })
    }

    private fun continueToApp() {
        finish()
        startActivity(Intent(this@SplashActivity, MainActivity::class.java))
    }
}