package com.appyhigh.adutils

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.LayoutRes
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import kotlin.concurrent.fixedRateTimer


object AdSdk {

    private var application: Application? = null
    private var TAG = "AdSdk"
    private var bannerAdRefreshTimer = 45000L
    private var nativeAdRefreshTimer = 45000L

    /**
     * Call initialize with you Application class object
     *
     * @param app -> Pass your application context here
     * @param appOpenAdUnit -> Pass an app open ad unit id if you wish to ad an app open ad
     * @param appOpenAdCallback -> This is the nullable listener for app open ad callbacks
     * @param bannerRefreshTimer -> Pass 0L to stop refresh or pass your required refresh interval in milliseconds. (Default Value is 45 seconds)
     * @param nativeRefreshTimer -> Pass 0L to stop refresh or pass your required refresh interval in milliseconds. (Default Value is 45 seconds)
     */
    fun initialize(
        app: Application,
        appOpenAdUnit: String = "",
        appOpenAdCallback: AppOpenAdCallback? = null,
        bannerRefreshTimer: Long = 45000L,
        nativeRefreshTimer: Long = 45000L
    ) {
        if (isGooglePlayServicesAvailable(app)) {
            if (application == null) {
                bannerAdRefreshTimer = bannerRefreshTimer
                nativeAdRefreshTimer = nativeRefreshTimer

                if (bannerAdRefreshTimer != 0L) {
                    fixedRateTimer("bannerAdTimer", false, 0L, bannerAdRefreshTimer) {
                        for (item in AdUtilConstants.bannerAdLifeCycleHashMap) {
                            if (item.value.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                                Handler(Looper.getMainLooper()).post {
                                    loadBannerAd(
                                        item.value.id,
                                        item.value.lifecycle,
                                        item.value.viewGroup,
                                        item.value.adUnit,
                                        item.value.adSize,
                                        item.value.bannerAdLoadCallback
                                    )
                                }
                            }
                        }
                    }
                }

                if (nativeAdRefreshTimer != 0L) {
                    fixedRateTimer("nativeAdTimer", false, 0L, nativeAdRefreshTimer) {
                        for (item in AdUtilConstants.nativeAdLifeCycleHashMap) {
                            if (item.value.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                                Handler(Looper.getMainLooper()).post {
                                    loadNativeAd(
                                        item.value.id,
                                        item.value.lifecycle,
                                        item.value.adUnit,
                                        item.value.viewGroup,
                                        item.value.nativeAdLoadCallback,
                                        item.value.layoutId,
                                        item.value.populator
                                    )
                                }
                            }
                        }
                    }
                }
            }

            application = app
            application?.let { myApp ->
                MobileAds.initialize(myApp) {
                    if (appOpenAdUnit.isNotEmpty()) {
                        attachAppOpenAdManager(appOpenAdUnit, true, appOpenAdCallback)
                    }
                }
            }
        }
    }

    private fun isGooglePlayServicesAvailable(application: Application): Boolean {
        val googleApiAvailability: GoogleApiAvailability = GoogleApiAvailability.getInstance()
        val status: Int = googleApiAvailability.isGooglePlayServicesAvailable(application)
        if (status != ConnectionResult.SUCCESS) {
            Toast.makeText(application, "Some Features might misbehave as Google Play Services are not available!", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    /**
     * Call initialize with you Application class object
     *
     * @param appOpenAdUnit -> Pass an app open ad unit id if you wish to ad an app open ad
     * @param isShownOnlyOnce -> Pass true if you want to show ad only once
     * @param appOpenAdCallback -> This is the nullable listener for app open ad callbacks
     **/
    fun attachAppOpenAdManager(
        appOpenAdUnit: String,
        isShownOnlyOnce: Boolean = false,
        appOpenAdCallback: AppOpenAdCallback? = null,
    ) {
        if (application != null) {
            val appOpenManager = AppOpenManager(application!!, appOpenAdUnit, isShownOnlyOnce, appOpenAdCallback)
            appOpenAdCallback?.onInitSuccess(appOpenManager)
        } else {
            throw Exception("Please make sure that you have initialized the AdSdk using AdSdk.initialize!!!")
        }
    }

    /**
     * Call loadBannerAd with following parameters to load a banner ad
     *
     *
     * @param viewGroup -> Pass the parent ViewGroup in which your ad unit will be loaded
     * @param adSize -> Pass the adUnit id in this parameter
     * @param adSize -> Pass the AdSize for banner that you want to load eg: AdSize.BANNER
     * @param bannerAdLoadCallback -> it is a nullable callback to register ad load events, pass null if you don't need callbacks
     *
     */
    fun loadBannerAd(
        lifecycle: Lifecycle,
        viewGroup: ViewGroup,
        adUnit: String,
        adSize: AdSize,
        bannerAdLoadCallback: BannerAdLoadCallback?) {
        loadBannerAd(System.currentTimeMillis(), lifecycle, viewGroup, adUnit, adSize, bannerAdLoadCallback)
    }

    private fun loadBannerAd(
        id: Long,
        lifecycle: Lifecycle,
        viewGroup: ViewGroup,
        adUnit: String,
        adSize: AdSize,
        bannerAdLoadCallback: BannerAdLoadCallback?
    ) {
        if (application != null) {
            if(adUnit.isBlank()) return
            if(AdUtilConstants.nativeAdLifeCycleHashMap[id] == null) {
                AdUtilConstants.bannerAdLifeCycleHashMap[id] =
                    BannerAdItem(id, lifecycle, viewGroup, adUnit, adSize, bannerAdLoadCallback)
            }
            lifecycle.addObserver(object : LifecycleObserver {
                @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
                fun onDestroy() {
                    AdUtilConstants.bannerAdLifeCycleHashMap.remove(id)
                }
            })
            val adRequest = AdRequest.Builder().build()
            val mAdView = AdView(viewGroup.context)
            mAdView.adSize = adSize
            mAdView.adUnitId = adUnit
            mAdView.loadAd(adRequest)
            viewGroup.removeAllViews()
            viewGroup.addView(mAdView)

            mAdView.adListener = object : AdListener() {
                // Code to be executed when an ad finishes loading.
                override fun onAdLoaded() {
                    bannerAdLoadCallback?.onAdLoaded()
                }

                // Code to be executed when an ad request fails.
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    bannerAdLoadCallback?.onAdFailedToLoad(adError)
                }

                // Code to be executed when an ad opens an overlay that
                // covers the screen.
                override fun onAdOpened() {
                    bannerAdLoadCallback?.onAdOpened()
                }

                // Code to be executed when the user clicks on an ad.
                override fun onAdClicked() {
                    bannerAdLoadCallback?.onAdClicked()
                }

                // Code to be executed when the user is about to return
                // to the app after tapping on an ad.
                override fun onAdClosed() {
                    bannerAdLoadCallback?.onAdClosed()
                }
            }
        }
    }

    /**
     * Call loadInterstitialAd with following params to load an interstitial ad
     *
     * @param adUnit -> Pass the adUnit id in this parameter
     * @param interstitialAdUtilLoadCallback -> nullable callback to register interstitial ad load events
     *
     * IMPORTANT: You wont be able to show ad if you pass a null callback
     */
    fun loadInterstitialAd(
        adUnit: String,
        interstitialAdUtilLoadCallback: InterstitialAdUtilLoadCallback?
    ) {
        if (application != null) {
            var mInterstitialAd: InterstitialAd?
            val adRequest = AdRequest.Builder().build()
            InterstitialAd.load(
                application!!,
                adUnit,
                adRequest,
                object : InterstitialAdLoadCallback() {
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        Log.d(TAG, adError.message)
                        mInterstitialAd = null
                        interstitialAdUtilLoadCallback?.onAdFailedToLoad(adError, mInterstitialAd)
                    }

                    override fun onAdLoaded(interstitialAd: InterstitialAd) {
                        Log.d(TAG, "Ad was loaded.")
                        mInterstitialAd = interstitialAd
                        interstitialAdUtilLoadCallback?.onAdLoaded(interstitialAd)

                        mInterstitialAd?.fullScreenContentCallback =
                            object : FullScreenContentCallback() {
                                override fun onAdDismissedFullScreenContent() {
                                    Log.d(TAG, "Ad was dismissed.")
                                    interstitialAdUtilLoadCallback?.onAdDismissedFullScreenContent()
                                }

                                override fun onAdFailedToShowFullScreenContent(adError: AdError?) {
                                    Log.d(TAG, "Ad failed to show.")
                                    interstitialAdUtilLoadCallback?.onAdFailedToShowFullScreenContent(
                                        adError,
                                    )
                                }

                                override fun onAdShowedFullScreenContent() {
                                    Log.d(TAG, "Ad showed fullscreen content.")
                                    mInterstitialAd = null
                                    interstitialAdUtilLoadCallback?.onAdShowedFullScreenContent()
                                }
                            }
                    }
                },
            )
        }
    }

    /**
     * Call loadRewardedAd with following params to load an rewarded ad
     *
     * @param adUnit -> Pass the adUnit id in this parameter
     * @param rewardedAdUtilLoadCallback -> nullable callback to register rewarded ad load events
     *
     * IMPORTANT: You wont be able to show ad if you pass a null callback
     */

    fun loadRewardedAd(
        adUnit: String,
        rewardedAdUtilLoadCallback: RewardedAdUtilLoadCallback?
    ) {
        if (application != null) {
            var mRewardedAd: RewardedAd?
            val adRequest = AdRequest.Builder().build()
            RewardedAd.load(
                application!!,
                adUnit,
                adRequest,
                object : RewardedAdLoadCallback() {
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        Log.d(TAG, adError.message)
                        mRewardedAd = null
                        rewardedAdUtilLoadCallback?.onAdFailedToLoad(adError, mRewardedAd)
                    }

                    override fun onAdLoaded(rewardedAd: RewardedAd) {
                        Log.d(TAG, "Ad was loaded.")
                        mRewardedAd = rewardedAd
                        rewardedAdUtilLoadCallback?.onAdLoaded(rewardedAd)

                        mRewardedAd?.fullScreenContentCallback =
                            object : FullScreenContentCallback() {
                                override fun onAdDismissedFullScreenContent() {
                                    Log.d(TAG, "Ad was dismissed.")
                                    rewardedAdUtilLoadCallback?.onAdDismissedFullScreenContent()
                                }

                                override fun onAdFailedToShowFullScreenContent(adError: AdError?) {
                                    Log.d(TAG, "Ad failed to show.")
                                    rewardedAdUtilLoadCallback?.onAdFailedToShowFullScreenContent(
                                        adError,
                                    )
                                }

                                override fun onAdShowedFullScreenContent() {
                                    Log.d(TAG, "Ad showed fullscreen content.")
                                    mRewardedAd = null
                                    rewardedAdUtilLoadCallback?.onAdShowedFullScreenContent()
                                }
                            }
                    }
                },
            )
        }
    }

    /**
     * Call loadNativeAd with following params to load an interstitial ad
     *
     * @param adUnit -> Pass the adUnit id in this parameter
     * @param viewGroup -> Pass the parent ViewGroup to add a native ad in that layout
     * @param callback -> nullable callback to register native ad load events
     * @param layoutId -> nullable layoutId, if you want a custom layout, pass a custom layout otherwise its load default UI
     */
    fun loadNativeAd(
        lifecycle: Lifecycle,
        adUnit: String,
        viewGroup: ViewGroup,
        callback: NativeAdLoadCallback?,
        @LayoutRes layoutId: Int = R.layout.ad_item
    ) {
        loadNativeAd(lifecycle, adUnit, viewGroup, callback, layoutId, null)
    }

    /**
     * Call loadNativeAd with following params to load an interstitial ad
     *
     * @param adUnit -> Pass the adUnit id in this parameter
     * @param viewGroup -> Pass the parent ViewGroup to add a native ad in that layout
     * @param layoutId -> nullable layoutId, if you want a custom layout, pass a custom layout otherwise its load default UI
     * @param populator -> nullable populator, if you want a custom population method, pass a custom populator which takes (NativeAd, NativeAdView?) as params
     */
    fun loadNativeAd(
        lifecycle: Lifecycle,
        adUnit: String,
        viewGroup: ViewGroup,
        @LayoutRes layoutId: Int = R.layout.ad_item,
        populator: ((nativeAd: NativeAd, adView: NativeAdView) -> Unit)? = null
    ) {
        loadNativeAd(lifecycle, adUnit, viewGroup, null, layoutId, populator)
    }

    /**
     * Call loadNativeAd with following params to load an interstitial ad
     *
     * @param adUnit -> Pass the adUnit id in this parameter
     * @param viewGroup -> Pass the parent ViewGroup to add a native ad in that layout
     * @param layoutId -> nullable layoutId, if you want a custom layout, pass a custom layout otherwise its load default UI
     * @param nativeAdLoadCallback -> nullable callback to register native ad load events
     * @param populator -> nullable populator, if you want a custom population method, pass a custom populator which takes (NativeAd, NativeAdView?) as params
     */
    fun loadNativeAd(
        lifecycle: Lifecycle,
        adUnit: String,
        viewGroup: ViewGroup,
        nativeAdLoadCallback: NativeAdLoadCallback?,
        @LayoutRes layoutId: Int = R.layout.ad_item,
        populator: ((nativeAd: NativeAd, adView: NativeAdView) -> Unit)? = null) {
        loadNativeAd(System.currentTimeMillis(), lifecycle, adUnit, viewGroup, nativeAdLoadCallback, layoutId, populator)
    }

    /**
     * Call loadNativeAd with following params to load an interstitial ad
     *
     * @param adUnit -> Pass the adUnit id in this parameter
     * @param viewGroup -> Pass the parent ViewGroup to add a native ad in that layout
     * @param layoutId -> nullable layoutId, if you want a custom layout, pass a custom layout otherwise its load default UI
     * @param nativeAdLoadCallback -> nullable callback to register native ad load events
     * @param populator -> nullable populator, if you want a custom population method, pass a custom populator which takes (NativeAd, NativeAdView?) as params
     */
    private fun loadNativeAd(
        id: Long,
        lifecycle: Lifecycle,
        adUnit: String,
        viewGroup: ViewGroup,
        nativeAdLoadCallback: NativeAdLoadCallback?,
        @LayoutRes layoutId: Int = R.layout.ad_item,
        populator: ((nativeAd: NativeAd, adView: NativeAdView) -> Unit)? = null
    ) {
        if (application != null) {
            if(adUnit.isBlank()) return
            if(AdUtilConstants.nativeAdLifeCycleHashMap[id] == null) {
                AdUtilConstants.nativeAdLifeCycleHashMap[id] = NativeAdItem(
                    id, lifecycle, adUnit, viewGroup, nativeAdLoadCallback, layoutId, populator
                )
            }
            lifecycle.addObserver(object : LifecycleObserver {
                @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
                fun onDestroy() {
                    AdUtilConstants.nativeAdLifeCycleHashMap.remove(id)
                }
            })
            var nativeAd: NativeAd? = null
            val adLoader: AdLoader? = AdLoader.Builder(application!!, adUnit)
                .forNativeAd { ad: NativeAd ->
                    nativeAd = ad
                }
                .withAdListener(object : AdListener() {

                    override fun onAdClicked() {
                        super.onAdClicked()
                        nativeAdLoadCallback?.onAdClicked()
                    }

                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        Log.e("$TAG: Ad Load Failed", adError.toString())
                        nativeAdLoadCallback?.onAdFailed(adError)
                    }

                    override fun onAdLoaded() {
                        super.onAdLoaded()
                        nativeAdLoadCallback?.onAdLoaded()
                        if (nativeAd != null) {
                            val adView =
                                View.inflate(
                                    application!!,
                                    layoutId,
                                    null
                                ) as NativeAdView
                            if(populator != null)
                                populator.invoke(nativeAd!!, adView)
                            else
                                populateUnifiedNativeAdView(nativeAd!!, adView)
                            viewGroup.removeAllViews()
                            viewGroup.addView(adView)
                        }
                    }
                })
                .withNativeAdOptions(
                    NativeAdOptions.Builder()
                        .build()
                )
                .build()
            adLoader?.loadAd(AdRequest.Builder().build())
        }
    }

    fun populateUnifiedNativeAdView(nativeAd: NativeAd, adView: NativeAdView?) {
        val iconView = adView?.findViewById(R.id.icon) as ImageView
        Log.e("$TAG: nativead", "ad body : " + nativeAd.body)

        val icon = nativeAd.icon
        adView.iconView = iconView
        if (icon == null) {
            adView.iconView?.visibility = View.GONE
        } else {
            (adView.iconView as ImageView).setImageDrawable(icon.drawable)
            adView.iconView?.visibility = View.VISIBLE
        }

        val ratingBar = adView.findViewById(R.id.stars) as View
        adView.starRatingView = ratingBar
        if (nativeAd.starRating == null) {
            adView.starRatingView?.visibility = View.INVISIBLE
        } else {
            (adView.starRatingView as RatingBar).rating = nativeAd.starRating!!.toFloat()
            adView.starRatingView?.visibility = View.VISIBLE
        }

        val adHeadline = adView.findViewById(R.id.headline) as TextView
        adView.headlineView = adHeadline
        adView.headlineView?.visibility = View.VISIBLE
        (adView.headlineView as TextView).text = nativeAd.headline

        val adBody = adView.findViewById(R.id.body) as TextView
        adView.bodyView = adBody
        adView.bodyView?.visibility = View.VISIBLE
        (adView.bodyView as TextView).text = nativeAd.body

        val cta = adView.findViewById(R.id.call_to_action) as Button
        adView.callToActionView = cta
        adView.callToActionView?.visibility = View.VISIBLE
        (adView.callToActionView as Button).text = nativeAd.callToAction
        adView.setNativeAd(nativeAd)
    }


}