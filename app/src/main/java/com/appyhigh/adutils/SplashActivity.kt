package com.appyhigh.adutils

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.appyhigh.adutils.callbacks.SplashInterstitialCallback
import com.appyhigh.adutils.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity() {
    lateinit var binding: ActivitySplashBinding
    var appOpenManager: AppOpenManager? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        AdSdk.initialize(
            applicationContext as MyApp
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