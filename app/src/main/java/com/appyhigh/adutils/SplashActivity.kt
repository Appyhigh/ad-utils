package com.appyhigh.adutils

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.appyhigh.adutils.callbacks.SplashInterstitialCallback
import com.appyhigh.adutils.databinding.ActivitySplashBinding
import com.appyhigh.adutils.models.PreloadNativeAds

class SplashActivity : AppCompatActivity() {
    lateinit var binding: ActivitySplashBinding
    var appOpenManager: AppOpenManager? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val preloadingNativeAdList = hashMapOf<String, PreloadNativeAds>()
        preloadingNativeAdList.put(
            "ca-app-pub-3940256099942544/2247696110",
            PreloadNativeAds(
                "ca-app-pub-3940256099942544/2247696110",
                AdSdk.ADType.DEFAULT_NATIVE_SMALL,
                mediaMaxHeight = 150,
                loadingTextSize = 24
            )
        )
        AdSdk.initialize(
            applicationContext as MyApp,
            testDevice = "037CDCC60DB2EB75232FCC1738C3917C",
            bannerRefreshTimer = 5000L, nativeRefreshTimer = 5000L,
            preloadingNativeAdList = preloadingNativeAdList,
            layoutInflater = layoutInflater,
            currentAppVersion = BuildConfig.VERSION_CODE,
            packageName = "notification.status.saver.whatsapp.messenger"
        )
        if (BuildConfig.DEBUG) {
            AdSdk.attachAppOpenAdManager("ca-app-pub-3940256099942544/3419835294", null, 1000)
        } else {
            AdSdk.attachAppOpenAdManager("ca-app-pub-3940256099942544/3419835294", null)
        }
        AdSdk.loadSplashAd(
            "ca-app-pub-3940256099942544/1033173712",
            this,
            object : SplashInterstitialCallback {
                override fun moveNext() {
                    finish()
                    startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                }
            }, 1000
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