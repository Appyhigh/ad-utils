package com.appyhigh.adutils

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.appyhigh.adutils.callbacks.AppOpenAdCallback
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.appopen.AppOpenAd.AppOpenAdLoadCallback
import java.util.*


/**
 * Prefetches App Open Ads.
 */
class AppOpenManager(
    private val myApplication: Application,
    private val appOpenAdUnit: String,
    private val isShownOnlyOnce: Boolean,
    private var appOpenAdCallback: AppOpenAdCallback?
) :
    LifecycleObserver,
    ActivityLifecycleCallbacks {
    private var currentActivity: Activity? = null
    private var loadCallback: AppOpenAdLoadCallback? = null
    private var loadTime: Long = 0
    private var backgroundTime: Long = 0

    /**
     * Creates and returns ad request.
     */
    private val adRequest: AdRequest
        get() = AdRequest.Builder().build()

    /**
     * Utility method to check if ad was loaded more than n hours ago.
     */
    private fun wasLoadTimeLessThanNHoursAgo(numHours: Long): Boolean {
        val dateDifference = Date().time - loadTime
        val numMilliSecondsPerHour: Long = 3600000
        return dateDifference < numMilliSecondsPerHour * numHours
    }

    /**
     * Utility method that checks if ad exists and can be shown.
     */
    private val isAdAvailable: Boolean
        get() = appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4)

    private fun fetchAd() {
        // Have unused ad, no need to fetch another.
        if (isAdAvailable) {
            return
        }
        loadCallback = object : AppOpenAdLoadCallback() {
            override fun onAdLoaded(ad: AppOpenAd) {
                super.onAdLoaded(ad)
                Log.d("aishik", "onAdLoaded: ")
                appOpenAd = ad
                loadTime = Date().time
                appOpenAdCallback?.onAdLoaded()
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                super.onAdFailedToLoad(loadAdError)
                Log.d("aishik", "onAdFailedToLoad: " + loadAdError.message)
                appOpenAdCallback?.onAdFailedToLoad(loadAdError)
            }

        }
        val request = adRequest
/*
        AppOpenAd.load(
            myApplication, appOpenAdUnit, request,
            AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT, loadCallback!!
        )
*/
    }

    fun showIfAdLoaded(activity: Activity): Boolean {
        currentActivity = activity
        return showAdIfAvailable()
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityStopped(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {
        currentActivity = null
    }

    /**
     * Shows the ad if one isn't already showing.
     *
     * @return True if the ad is available and will try to show ad, False if the ad is not loaded yet
     */
    private fun showAdIfAvailable(): Boolean {
        // Only show ad if there is not already an app open ad currently showing
        // and an ad is available.
        if (!isShowingAd && isAdAvailable) {
            appOpenAd!!.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    // Set the reference to null so isAdAvailable() returns false.
                    appOpenAd = null
                    isShowingAd = false
                    if (isShownOnlyOnce) {
                        ProcessLifecycleOwner.get().lifecycle.removeObserver(this@AppOpenManager)
                        currentActivity = null
                    } else {
                        fetchAd()
                    }
                    appOpenAdCallback?.onAdClosed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    appOpenAdCallback?.onAdFailedToShow(adError)
                }

                override fun onAdShowedFullScreenContent() {
                    isShowingAd = true
                }
            }
            currentActivity?.let {
                appOpenAd!!.show(it)
                return true
            }
            return false
        } else {
            fetchAd()
            return false
        }
    }

    /**
     * LifecycleObserver methods
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onStart() {
        val appBackgroundTime = System.currentTimeMillis() - backgroundTime
        if (appBackgroundTime > 30000)
            showAdIfAvailable()
    }

    /**
     * LifecycleObserver methods
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onStop() {
        backgroundTime = System.currentTimeMillis()
    }


    private var appOpenAd: AppOpenAd? = null

    companion object {
        private const val LOG_TAG = "aishik"
        private var isShowingAd = false
        private var splashAppOpenAd: AppOpenAd? = null
        private var reason: String? = null

        interface appOpenCallBack {
            fun adDismissed()
            fun adError(message: String?)
            fun adShown()
            fun adClicked()
            fun adLoaded(appOpenAd: AppOpenAd)
            fun adNotLoadedYet(reason: String?)
        }

        fun loadSplashAppOpenAd(application: Application, adUnit: String) {
            Log.d("aishik", "loadSplashAppOpenAd: aaa " + application)
            val build = AdRequest.Builder().build()
            val adLoadCallBack = object : AppOpenAdLoadCallback() {
                override fun onAdLoaded(p0: AppOpenAd) {
                    super.onAdLoaded(p0)
                    Log.d("aishik", "onAdLoaded: 123 " + p0)
                    splashAppOpenAd = p0
                    reason = null
                }

                override fun onAdFailedToLoad(p0: LoadAdError) {
                    super.onAdFailedToLoad(p0)
                    Log.d("aishik", "onAdFailedToLoad: 456" + p0.message)
                    splashAppOpenAd = null
                    reason = p0.message
                }
            }
            val applicationContext = application.applicationContext
            Log.d("aishik", "loadSplashAppOpenAd: 1 $adUnit $build $applicationContext")
            AppOpenAd.load(
                applicationContext, adUnit, build,
                AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT, adLoadCallBack
            )
            Log.d("aishik", "loadSplashAppOpenAd: 2 ")
        }

        fun showAdIfAvailable(activity: Activity, appOpenCallBack: appOpenCallBack) {
            Log.d("aishik", "showAdIfAvailable: 111")
            if (splashAppOpenAd != null) {
                Log.d("aishik", "showAdIfAvailable: 222")
                splashAppOpenAd!!.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                        super.onAdFailedToShowFullScreenContent(p0)
                        appOpenCallBack.adError(p0.message)
                    }

                    override fun onAdShowedFullScreenContent() {
                        super.onAdShowedFullScreenContent()
                        appOpenCallBack.adShown()
                    }

                    override fun onAdDismissedFullScreenContent() {
                        super.onAdDismissedFullScreenContent()
                        appOpenCallBack.adDismissed()
                    }

                    override fun onAdClicked() {
                        super.onAdClicked()
                        appOpenCallBack.adClicked()
                    }
                }
                Log.d("aishik", "showAdIfAvailable: SHOW")
                appOpenCallBack.adLoaded(splashAppOpenAd!!)
//                splashAppOpenAd!!.show(activity)
            } else {
                appOpenCallBack.adNotLoadedYet(reason)
            }
        }
    }

    /**
     * Constructor
     */
    //Todo Fix App open fg to bg
    init {
        if (!isShownOnlyOnce) {
            myApplication.registerActivityLifecycleCallbacks(this)
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }
}