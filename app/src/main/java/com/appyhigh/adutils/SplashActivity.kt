package com.appyhigh.adutils

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.appyhigh.adutils.callbacks.AppOpenAdLoadCallback
import com.appyhigh.adutils.callbacks.SplashInterstitialCallback
import com.appyhigh.adutils.callbacks.VersionControlCallback
import com.appyhigh.adutils.databinding.ActivitySplashBinding
import com.appyhigh.adutils.models.PreloadNativeAds
import com.appyhigh.adutils.models.apimodels.AppsData
import com.appyhigh.adutils.utils.AdMobUtil
import com.appyhigh.adutils.utils.AdMobUtil.printData
import com.google.android.gms.ads.LoadAdError
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
                AdSdk.ADType.MEDIUM,
                loadTimeOut = 4000
            )
        )
        preloadingNativeAdList.put(
            "util_native_preload1",
            PreloadNativeAds(
                "ca-app-pub-3940256099942544/2247696110",
                "util_native_preload1",
                AdSdk.ADType.MEDIUM,
                loadTimeOut = 4000
            )
        )

        AdSdk.initialize(
            app = applicationContext as MyApp,
            activity = this@SplashActivity,
            version = BuildConfig.VERSION_CODE,
            anyView = binding.root.rootView,
            testDevice = "B3EEABB8EE11C2BE770B684D95219ECB",
            preloadingNativeAdList = preloadingNativeAdList,
            fetchingCallback = object : AdSdk.FetchingCallback {
                override fun OnComplete(app: AppsData?) {
                }
                override fun OnInitialized() {
                    applicationContext.printData()
                    AdSdk.loadAppOpenAd(
                        this@SplashActivity,
                        "ca-app-pub-3940256099942544/3419835294",
                        "util_appopen",
                        true,
                        object : AppOpenAdLoadCallback() {
                            override fun onAdLoaded(ad: AppOpenAd) {
                                super.onAdLoaded(ad)
                            }

                            override fun onAdFailedToLoad(loadAdError: LoadAdError?) {
                                super.onAdFailedToLoad(loadAdError)
                                Log.d("Appopen", "onAdFailedToLoad: " + loadAdError?.message)
                            }
                        },
                        true,
                        loadTimeOut = 4000
                    )

                    AdSdk.loadSplashAd(
                        "ca-app-pub-3940256099942544/1033173712",
                        "util_interstitial",
                        this@SplashActivity,
                        object : SplashInterstitialCallback {
                            override fun moveNext() {
                                finish()
                                startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                            }

                            override fun OnError(msg: String) {
                                Log.d("Splashinterstial", "onAdFailedToLoad: " + msg)
                            }
                        }, 6000
                    )
                }
            },
            versionControlCallback = object : VersionControlCallback {
                override fun OnSoftUpdate() {
                }

                override fun OnHardUpdate() {
                }
            }
        )
    }
}