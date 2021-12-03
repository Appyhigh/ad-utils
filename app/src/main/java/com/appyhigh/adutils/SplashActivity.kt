package com.appyhigh.adutils

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import com.appyhigh.adutils.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity() {
    lateinit var binding: ActivitySplashBinding
    var appOpenManager: AppOpenManager? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        AdSdk().initialize(
            applicationContext as MyApp,
            "ca-app-pub-3940256099942544/3419835294",
            appOpenAdCallback
        )
        AdSdk().setAdRefreshInterval(10000,10000)
        Handler(Looper.getMainLooper()).postDelayed({
            if (appOpenManager == null) {
                startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                finish()
            }
        }, 4000)
    }

    private val appOpenAdCallback = object : AppOpenAdCallback {
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
}