package com.appyhigh.adutils

import android.app.Activity
import android.app.Application
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.*
import androidx.annotation.LayoutRes
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.appyhigh.adutils.callbacks.*
import com.appyhigh.adutils.models.BannerAdItem
import com.appyhigh.adutils.models.NativeAdItem
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.*
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
        activity: Activity?,
        app: Application,
        appOpenAdUnit: String = "",
        appOpenAdCallback: AppOpenAdCallback? = null,
        bannerRefreshTimer: Long = 45000L,
        nativeRefreshTimer: Long = 45000L,
        loadSplashAppOpenAd: Boolean = false,
        showBGToFGAdOnlyOnce: Boolean = false
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
                                        item.value.activity,
                                        item.value.id,
                                        item.value.lifecycle,
                                        item.value.adUnit,
                                        item.value.viewGroup,
                                        item.value.nativeAdLoadCallback,
                                        item.value.layoutId,
                                        item.value.populator,
                                        item.value.viewId,
                                        item.value.background,
                                        item.value.textColor1,
                                        item.value.textColor2,
                                        item.value.maxHeight
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
                        attachAppOpenAdManager(
                            appOpenAdUnit,
                            showBGToFGAdOnlyOnce,
                            appOpenAdCallback
                        )
                        Log.d("aishik", "initialize: ")
                        if (loadSplashAppOpenAd) {
                            AppOpenManager.loadSplashAppOpenAd(
                                myApp,
                                appOpenAdUnit
                            )
                        }
                    }
                }
            }
        }
//        AppOpenManager.loadSplashAppOpenAd(application!!, appOpenAdUnit)
    }

    private fun isGooglePlayServicesAvailable(application: Application): Boolean {
        val googleApiAvailability: GoogleApiAvailability = GoogleApiAvailability.getInstance()
        val status: Int = googleApiAvailability.isGooglePlayServicesAvailable(application)
        if (status != ConnectionResult.SUCCESS) {
            Toast.makeText(
                application,
                "Some Features might misbehave as Google Play Services are not available!",
                Toast.LENGTH_SHORT
            ).show()
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
            val appOpenManager =
                AppOpenManager(application!!, appOpenAdUnit, isShownOnlyOnce, appOpenAdCallback)
            appOpenAdCallback?.onInitSuccess(appOpenManager)
        } else {
            throw Exception("Please make sure that you have initialized the AdSdk using AdSdk.initialize!!!")
        }
    }

    /**
     * Call loadBannerAd with following parameters to load a banner ad
     *
     * @param lifecycle -> Lifecycle of activity in which ad will be loaded
     * @param viewGroup -> Pass the parent ViewGroup in which your ad unit will be loaded
     * @param adUnit -> Pass the adUnit id in this parameter
     * @param adSize -> Pass the AdSize for banner that you want to load eg: AdSize.BANNER
     * @param bannerAdLoadCallback -> it is a nullable callback to register ad load events, pass null if you don't need callbacks
     *
     */
    fun loadBannerAd(
        lifecycle: Lifecycle,
        viewGroup: ViewGroup,
        adUnit: String,
        adSize: AdSize,
        bannerAdLoadCallback: BannerAdLoadCallback?,
    ) {
        loadBannerAd(
            System.currentTimeMillis(),
            lifecycle,
            viewGroup,
            adUnit,
            adSize,
            bannerAdLoadCallback
        )
    }

    private fun loadBannerAd(
        id: Long,
        lifecycle: Lifecycle,
        viewGroup: ViewGroup,
        adUnit: String,
        adSize: AdSize,
        bannerAdLoadCallback: BannerAdLoadCallback?,
    ) {
        if (application != null) {
            if (adUnit.isBlank()) return
            if (AdUtilConstants.nativeAdLifeCycleHashMap[id] == null) {
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
        interstitialAdUtilLoadCallback: InterstitialAdUtilLoadCallback?,
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
                        mInterstitialAd = null
                        interstitialAdUtilLoadCallback?.onAdFailedToLoad(adError, mInterstitialAd)
                    }

                    override fun onAdLoaded(interstitialAd: InterstitialAd) {
                        mInterstitialAd = interstitialAd
                        interstitialAdUtilLoadCallback?.onAdLoaded(interstitialAd)

                        mInterstitialAd?.fullScreenContentCallback =
                            object : FullScreenContentCallback() {
                                override fun onAdDismissedFullScreenContent() {
                                    interstitialAdUtilLoadCallback?.onAdDismissedFullScreenContent()
                                }

                                override fun onAdFailedToShowFullScreenContent(adError: AdError?) {
                                    interstitialAdUtilLoadCallback?.onAdFailedToShowFullScreenContent(
                                        adError,
                                    )
                                }

                                override fun onAdShowedFullScreenContent() {
                                    mInterstitialAd = null
                                    interstitialAdUtilLoadCallback?.onAdShowedFullScreenContent()
                                }
                            }
                    }
                },
            )
        }
    }

    fun loadSplashAd(
        adUnit: String,
        activity: Activity?,
        callback: SplashInterstitialCallback,
        timer: Long = 5000L,
    ) {
        if (activity != null) {
            var splash: InterstitialAd? = null
            val ctd = object : CountDownTimer(timer, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    if (splash != null) {
                        splash?.show(activity)
                        this.cancel()
                    }
                }

                override fun onFinish() {
                    callback.moveNext()
                }
            }.start()
            val adRequest = AdRequest.Builder().build()
            InterstitialAd.load(
                application!!,
                adUnit,
                adRequest, object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(splashInters: InterstitialAd) {
                        super.onAdLoaded(splashInters)
                        splash = splashInters
                        splash?.fullScreenContentCallback =
                            object : FullScreenContentCallback() {
                                override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                                    super.onAdFailedToShowFullScreenContent(p0)
                                    callback.moveNext()
                                }

                                override fun onAdDismissedFullScreenContent() {
                                    super.onAdDismissedFullScreenContent()
                                    callback.moveNext()
                                }
                            }
                    }

                    override fun onAdFailedToLoad(p0: LoadAdError) {
                        super.onAdFailedToLoad(p0)
                        callback.moveNext()
                    }
                }
            )
        } else {
            callback.moveNext()
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
        rewardedAdUtilLoadCallback: RewardedAdUtilLoadCallback?,
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
                        mRewardedAd = null
                        rewardedAdUtilLoadCallback?.onAdFailedToLoad(adError, mRewardedAd)
                    }

                    override fun onAdLoaded(rewardedAd: RewardedAd) {
                        mRewardedAd = rewardedAd
                        rewardedAdUtilLoadCallback?.onAdLoaded(rewardedAd)

                        mRewardedAd?.fullScreenContentCallback =
                            object : FullScreenContentCallback() {
                                override fun onAdDismissedFullScreenContent() {
                                    rewardedAdUtilLoadCallback?.onAdDismissedFullScreenContent()
                                }

                                override fun onAdFailedToShowFullScreenContent(adError: AdError?) {
                                    rewardedAdUtilLoadCallback?.onAdFailedToShowFullScreenContent(
                                        adError,
                                    )
                                }

                                override fun onAdShowedFullScreenContent() {
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
     * Call loadNativeAd with following params to load a Native Ad
     *
     *
     * @param lifecycle -> Lifecycle of activity in which ad will be loaded
     * @param adUnit -> Pass the adUnit id in this parameter
     * @param viewGroup -> Pass the parent ViewGroup to add a native ad in that layout
     * @param callback -> nullable callback to register native ad load events
     * @param layoutId -> nullable layoutId, if you want a custom layout, pass a custom layout otherwise its load default UI
     * @param viewId -> according to the value from remote config, layout is loaded.
     */

    class ADType {
        companion object {
            val SMALLEST = "3"
            val SMALLER = "4"
            val SEMIMEDIUM = "2"
            val MEDIUM = "1"
            val BIG = "5"
            val DEFAULT_NATIVE_SMALL = "6"
        }
    }

    fun loadNativeAd(
        activity: Activity?,
        lifecycle: Lifecycle,
        adUnit: String,
        viewGroup: ViewGroup,
        callback: NativeAdLoadCallback?,
        adType: String,
        background: Any?,
        textColor1: Int?,
        textColor2: Int?,
        maxHeight: Int = 300
    ) {
        @LayoutRes val layoutId = when (adType) {
            "1" -> R.layout.native_admob_ad_t1/*MEDIUM*/
            "2" -> R.layout.native_admob_ad_t2/*SEMIMEDIUM*/
            "3" -> R.layout.native_admob_ad_t3/*SMALLEST*/
            "4" -> R.layout.native_admob_ad_t4/*SMALLER*/
            "5" -> R.layout.native_admob_ad_t5/*BIG*/
            "6" -> R.layout.native_admob_ad_t6/*DEFAULT NATIVE SMALL*/
            else -> R.layout.native_admob_ad_t1
        }
        loadNativeAd(
            activity,
            lifecycle,
            adUnit,
            viewGroup,
            callback,
            layoutId,
            null,
            adType,
            background = background,
            textColor1,
            textColor2,
            maxHeight
        )

    }

    /**
     * Call loadNativeAd with following params to load a Native Ad
     *
     *
     * @param lifecycle -> Lifecycle of activity in which ad will be loaded
     * @param adUnit -> Pass the adUnit id in this parameter
     * @param viewGroup -> Pass the parent ViewGroup to add a native ad in that layout
     * @param layoutId -> nullable layoutId, if you want a custom layout, pass a custom layout otherwise its load default UI
     * @param populator -> nullable populator, if you want a custom population method, pass a custom populator which takes (NativeAd, NativeAdView?) as params
     */
//    fun loadNativeAd(
//        lifecycle: Lifecycle,
//        adUnit: String,
//        viewGroup: ViewGroup,
//        @LayoutRes layoutId: Int = R.layout.ad_item,
//        populator: ((nativeAd: NativeAd, adView: NativeAdView) -> Unit)? = null
//    ) {
//        loadNativeAd(lifecycle, adUnit, viewGroup, null, layoutId, populator)
//    }

    /**
     * Call loadNativeAd with following params to load a Native Ad
     *
     *
     * @param lifecycle -> Lifecycle of activity in which ad will be loaded
     * @param adUnit -> Pass the adUnit id in this parameter
     * @param viewGroup -> Pass the parent ViewGroup to add a native ad in that layout
     * @param nativeAdLoadCallback -> nullable callback to register native ad load events
     * @param layoutId -> nullable layoutId, if you want a custom layout, pass a custom layout otherwise its load default UI
     * @param populator -> nullable populator, if you want a custom population method, pass a method which takes (NativeAd, NativeAdView?) as params
     */
    fun loadNativeAd(
        activity: Activity?,
        lifecycle: Lifecycle,
        adUnit: String,
        viewGroup: ViewGroup,
        nativeAdLoadCallback: NativeAdLoadCallback?,
        @LayoutRes layoutId: Int = R.layout.native_admob_ad_t1,
        populator: ((nativeAd: NativeAd, adView: NativeAdView) -> Unit)? = null,
        adType: String = "1",
        background: Any?,
        textColor1: Int?,
        textColor2: Int?,
        maxHeight: Int = 300,
    ) {
        loadNativeAd(
            activity,
            System.currentTimeMillis(),
            lifecycle,
            adUnit,
            viewGroup,
            nativeAdLoadCallback,
            layoutId,
            populator,
            adType,
            background = background,
            textColor1,
            textColor2,
            maxHeight
        )
    }

    /**
     * Call loadNativeAd with following params to load an interstitial ad
     *
     * @param adUnit -> Pass the adUnit id in this parameter
     * @param viewGroup -> Pass the parent ViewGroup to add a native ad in that layout
     * @param layoutId -> nullable layoutId, if you want a custom layout, pass a custom layout otherwise its load default UI
     * @param nativeAdLoadCallback -> nullable callback to register native ad load events
     * @param populator -> nullable populator, if you want a custom population method, pass a custom populator which takes (NativeAd, NativeAdView) as params
     */
    private fun loadNativeAd(
        activity: Activity?,
        id: Long = System.currentTimeMillis(),
        lifecycle: Lifecycle,
        adUnit: String,
        viewGroup: ViewGroup,
        nativeAdLoadCallback: NativeAdLoadCallback?,
        @LayoutRes layoutId: Int = R.layout.native_admob_ad_t1,
        populator: ((nativeAd: NativeAd, adView: NativeAdView) -> Unit)? = null,
        adType: String = "1",
        background: Any?,
        textColor1: Int?,
        textColor2: Int?,
        maxHeight: Int = 300,
    ) {
        viewGroup.visibility = VISIBLE
        if (activity != null) {
            val inflate = activity.layoutInflater.inflate(R.layout.ad_loading_layout, null)
            val id1 = inflate.findViewById<View>(R.id.rl)
            val tv = inflate.findViewById<TextView>(R.id.tv)
            if (textColor1 != null) {
                tv.setTextColor(textColor1)
            }
            when (background) {
                is String -> {
                    id1.setBackgroundColor(Color.parseColor(background))
                }
                is Drawable -> {
                    id1.background = background
                }
                is Int -> {
                    id1.setBackgroundColor(background)
                }
            }
            viewGroup.removeAllViews()
            viewGroup.addView(inflate)
            if (adUnit.isBlank()) return
            if (AdUtilConstants.nativeAdLifeCycleHashMap[id] == null) {
                AdUtilConstants.nativeAdLifeCycleHashMap[id] = NativeAdItem(
                    activity,
                    id,
                    lifecycle,
                    adUnit,
                    viewGroup,
                    nativeAdLoadCallback,
                    layoutId,
                    populator,
                    adType,
                    background,
                    textColor1,
                    textColor2,
                    maxHeight
                )
            }
            lifecycle.addObserver(object : LifecycleObserver {
                @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
                fun onDestroy() {
                    AdUtilConstants.nativeAdLifeCycleHashMap.remove(id)
                }
            })
            var nativeAd: NativeAd? = null
            val adLoader: AdLoader? = AdLoader.Builder(activity, adUnit)
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
                                activity.layoutInflater.inflate(layoutId, null)
                                        as NativeAdView
                            if (background != null) {
                                when (background) {
                                    is String -> {
                                        adView.setBackgroundColor(Color.parseColor(background))
                                    }
                                    is Drawable -> {
                                        adView.background = background
                                    }
                                    is Int -> {
                                        adView.setBackgroundColor(background)
                                    }
                                }
                            }
                            if (populator != null) {
                                populator.invoke(nativeAd!!, adView)
                            } else {
                                populateUnifiedNativeAdView(
                                    nativeAd!!,
                                    adView,
                                    adType,
                                    textColor1,
                                    textColor2,
                                    maxHeight
                                )
                            }
                            viewGroup.removeAllViews()
                            viewGroup.addView(adView)
                        }
                    }
                })
                .withNativeAdOptions(
                    NativeAdOptions.Builder()
                        .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                        .setRequestCustomMuteThisAd(true)
                        .build()
                )
                .build()
            adLoader?.loadAd(AdRequest.Builder().build())
        }
    }

    fun populateUnifiedNativeAdView(
        nativeAd: NativeAd,
        adView: NativeAdView?,
        adType: String,
        textColor1: Int?,
        textColor2: Int?,
        maxHeight: Int = 300,
    ) {
        val iconView = adView?.findViewById(R.id.icon) as ImageView
        Log.e("$TAG: nativead", "ad body : " + nativeAd.body)
        var icon = nativeAd.icon
        adView.iconView = iconView
        val iconView1 = adView.iconView
        if (icon == null) {
            if (adType == ADType.DEFAULT_NATIVE_SMALL) {
                val iconHeight = maxHeight
                iconView1.layoutParams = LinearLayout.LayoutParams(1, iconHeight)
            }
            iconView1?.visibility = View.INVISIBLE
        } else {
            if (adType == ADType.DEFAULT_NATIVE_SMALL) {
                val iconHeight = maxHeight
                iconView1.layoutParams = LinearLayout.LayoutParams(iconHeight, iconHeight)
            }
            (iconView1 as ImageView).setImageDrawable(icon.drawable)
            iconView1?.visibility = VISIBLE
        }

        val mediaView = adView.findViewById<MediaView>(R.id.ad_media)
        adView.mediaView = mediaView
        mediaView.setImageScaleType(ImageView.ScaleType.FIT_CENTER)
        mediaView.setOnHierarchyChangeListener(object : ViewGroup.OnHierarchyChangeListener {
            override fun onChildViewAdded(parent: View, child: View) {
                val scale: Float = adView.mediaView.context.resources.displayMetrics.density
                val maxHeightPixels = maxHeight
//                val maxHeightDp = (maxHeightPixels * scale + 0.5f).toInt()
                if (child is ImageView) { //Images
                    child.adjustViewBounds = true
                    val layoutParams1 = child.layoutParams
                    layoutParams1.width = MATCH_PARENT
                    layoutParams1.height = maxHeight
                    child.layoutParams = layoutParams1
                } else { //Videos
                    val params = child.layoutParams
                    params.width = MATCH_PARENT
                    params.height = maxHeightPixels
                    child.layoutParams = params
                }
            }

            override fun onChildViewRemoved(parent: View, child: View) {}
        })
        val mediaIcon = nativeAd.mediaContent
        if (mediaIcon == null || adType == "4") {
            adView.mediaView?.visibility = View.GONE
            adView.mediaView
        } else {
            adView.mediaView?.visibility = VISIBLE
            (adView.mediaView as MediaView).setMediaContent(mediaIcon)
        }

        val adHeadline = adView.findViewById(R.id.headline) as TextView
        adView.headlineView = adHeadline
        val headlineView = adView.headlineView
        headlineView?.visibility = VISIBLE
        val textView = headlineView as TextView
        textView.text = nativeAd.headline
        if (textColor1 != null) {
            textView.setTextColor(textColor1)
        }

        val adBody = adView.findViewById(R.id.body) as TextView
        adView.bodyView = adBody
        val bodyView = adView.bodyView
        if (adType == "2") {
            bodyView?.visibility = View.GONE
        } else {
            bodyView?.visibility = View.GONE
            val textView1 = bodyView as TextView
            textView1.text = nativeAd.body
            if (textColor2 != null) {
                textView1.setTextColor(textColor2)
            }
        }

        val adStore = adView.findViewById<TextView>(R.id.ad_store)
        adView.storeView = adStore
        if (nativeAd.store != null && adType == "4") {
            adView.storeView?.visibility = VISIBLE
            val textView1 = adView.storeView as TextView
            textView1.text = nativeAd.store
            if (textColor2 != null) {
                textView1.setTextColor(textColor2)
            }
        } else {
            adView.storeView?.visibility = View.GONE
        }


        val cta = adView.findViewById(R.id.call_to_action) as Button
        adView.callToActionView = cta
        adView.callToActionView?.visibility = VISIBLE
        (adView.callToActionView as Button).text = nativeAd.callToAction
        adView.setNativeAd(nativeAd)

        if (nativeAd.adChoicesInfo != null && adView.adChoicesView != null) {
            val choicesView = AdChoicesView(adView.adChoicesView.context)
            adView.adChoicesView = choicesView
        }


    }


}