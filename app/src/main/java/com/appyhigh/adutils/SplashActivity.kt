package com.appyhigh.adutils

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.appyhigh.adutils.callbacks.AppOpenAdLoadCallback
import com.appyhigh.adutils.callbacks.SplashInterstitialCallback
import com.appyhigh.adutils.databinding.ActivitySplashBinding
import com.appyhigh.adutils.models.PreloadNativeAds
import com.appyhigh.adutils.models.apimodels.AppsData
import com.google.android.gms.ads.appopen.AppOpenAd

class SplashActivity : AppCompatActivity() {
    lateinit var binding: ActivitySplashBinding
    var appOpenManager: AppOpenManager? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val preloadingNativeAdList = hashMapOf<String, PreloadNativeAds>()
        preloadingNativeAdList.put(
            "util_native_preload",
            PreloadNativeAds(
                "ca-app-pub-3940256099942544/2247696110",
                "util_native_preload",
                AdSdk.ADType.MEDIUM
            )
        )
        preloadingNativeAdList.put(
            "util_native_preload1",
            PreloadNativeAds(
                "ca-app-pub-3940256099942544/2247696110",
                "util_native_preload1",
                AdSdk.ADType.MEDIUM
            )
        )
        AdSdk.initialize(
            applicationContext as MyApp,
            testDevice = "B3EEABB8EE11C2BE770B684D95219ECB",
            preloadingNativeAdList = null,
            fetchingCallback = object : AdSdk.FetchingCallback {
                override fun OnComplete(app: AppsData?) {
                    if (app != null){
                        runOnUiThread {
                            if (BuildConfig.DEBUG) {
                                AdSdk.attachAppOpenAdManager(
                                    DynamicsAds.getDynamicAdsId("ca-app-pub-3940256099942544/3419835294", "util_appopen"),
                                    "util_appopen",
                                    null,
                                    1000,
                                    false
                                )
                            } else {
                                AdSdk.attachAppOpenAdManager(DynamicsAds.getDynamicAdsId("ca-app-pub-3940256099942544/3419835294", "util_appopen"),
                                    "util_appopen",
                                    null)
                            }

                            AdSdk.loadSplashAd(
                                "ca-app-pub-3940256099942544/1033173712",
                                "util_interstitial",
                                this@SplashActivity,
                                object : SplashInterstitialCallback {
                                    override fun moveNext() {
                                        finish()
                                        startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                                    }
                                }, 5000
                            )
                            AdSdk.preloadAds(
                                applicationContext as MyApp,
                                preloadingNativeAdList
                            )
                        }
                    }
                    else {
                        Toast.makeText(applicationContext,"Failed",Toast.LENGTH_LONG).show()
                    }
                }
            }
        )

        /*Handler(Looper.getMainLooper()).postDelayed({
            if (appOpenManager == null) {
                startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                finish()
            }
        }, 4000)*/
    }

/*
    private val appOpenAdCallback = object : AppOpenAdCallback() {
        override fun onInitSuccess(manager: AppOpenManager) {
            appOpenManager = manager
        }

        override fun onAdLoaded() {
            if (appOpenManager != null) {
                appOpenManager?.showIfAdLoaded(this@SplashActivity)
            }
        }

        override fun onAdClosed() {
            startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            finish()
        }
    }
*/
}