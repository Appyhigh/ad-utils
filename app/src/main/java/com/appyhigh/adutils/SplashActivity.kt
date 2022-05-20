package com.appyhigh.adutils

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.appyhigh.adutils.databinding.ActivitySplashBinding
import com.google.android.gms.ads.appopen.AppOpenAd

class SplashActivity : AppCompatActivity() {
    lateinit var binding: ActivitySplashBinding
    var appOpenManager: AppOpenManager? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        AdSdk.initialize(
            activity = null,
            app = applicationContext as MyApp,
            appOpenAdUnit = "ca-app-pub-3940256099942544/3419835294",
            appOpenAdCallback = null,
            bannerRefreshTimer = 0L,
            nativeRefreshTimer = 0L,
            loadSplashAppOpenAd = true,
            showBGToFGAdOnlyOnce = false
        )
/*
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
*/
        AppOpenManager.loadSplashAppOpenAd(application, "ca-app-pub-3940256099942544/3419835294")
        loadAppOpenAd()
        /*Handler(Looper.getMainLooper()).postDelayed({
            if (appOpenManager == null) {
                startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                finish()
            }
        }, 4000)*/
    }

    var waitTimeinSec = 5
    private fun loadAppOpenAd() {
        AppOpenManager.showAdIfAvailable(this,
            object : AppOpenManager.Companion.appOpenCallBack {
                override fun adDismissed() {
                    Log.d("aishik", "adDismissed: ")
                    finish()
                    startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                }

                override fun adLoaded(appOpenAd: AppOpenAd) {
                    Log.d("aishik", "adLoaded: 11 ")
                    runOnUiThread {
                        appOpenAd.show(this@SplashActivity)
                    }
                }

                override fun adError(message: String?) {
                    Log.d("aishik", "adError: $message")
                    finish()
                    startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                }

                override fun adShown() {
                    Log.d("aishik", "adShown: ")
                }

                override fun adClicked() {
                    Log.d("aishik", "adClicked: ")
                }

                override fun adNotLoadedYet(reason: String?) {
                    finish()
                    startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                }
            })
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