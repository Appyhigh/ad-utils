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
    private var appOpenAdCallback: AppOpenAdCallback?
) :
    LifecycleObserver,
    ActivityLifecycleCallbacks {
    private var appOpenAd: AppOpenAd? = null
    private var currentActivity: Activity? = null
    private var loadCallback: AppOpenAdLoadCallback? = null
    private var loadTime: Long = 0
    private var isShown = false

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
                appOpenAd = ad
                loadTime = Date().time
                appOpenAdCallback?.onAdLoaded()
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                super.onAdFailedToLoad(loadAdError)
                Log.d(LOG_TAG, loadAdError.message)
            }

        }
        val request = adRequest
        AppOpenAd.load(
            myApplication, appOpenAdUnit, request,
            AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT, loadCallback!!
        )
    }

    fun showIfAdLoaded(activity: Activity) {
        currentActivity = activity
        showAdIfAvailable()
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
     */
    private fun showAdIfAvailable() {
        // Only show ad if there is not already an app open ad currently showing
        // and an ad is available.
        if (!isShowingAd && isAdAvailable) {
            Log.d(LOG_TAG, "Will show ad.")
            val fullScreenContentCallback: FullScreenContentCallback =
                object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        // Set the reference to null so isAdAvailable() returns false.
                        appOpenAd = null
                        isShowingAd = false
                        isShown = true
                        appOpenAdCallback?.onAdClosed()
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {}
                    override fun onAdShowedFullScreenContent() {
                        isShowingAd = true
                    }
                }
            appOpenAd!!.fullScreenContentCallback = fullScreenContentCallback
            appOpenAd!!.show(currentActivity!!)
        } else {
            Log.d(LOG_TAG, "Ad not loaded yet")
            fetchAd()
        }
    }

    /**
     * LifecycleObserver methods
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onStart() {
        if (!isShown)
        showAdIfAvailable()
        Log.d(LOG_TAG, "onStart")
    }

    companion object {
        private const val LOG_TAG = "AppOpenManager"
        private var isShowingAd = false
    }

    /**
     * Constructor
     */
    init {
        myApplication.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }
}