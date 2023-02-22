package com.appyhigh.adutils

import android.app.Activity
import android.app.AlertDialog
import android.app.Application
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.*
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.*
import androidx.annotation.LayoutRes
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.appyhigh.adutils.AdUtilConstants.preloadNativeAdList
import com.appyhigh.adutils.appupdatemanager.VersionControlConstants
import com.appyhigh.adutils.appupdatemanager.VersionControlListener
import com.appyhigh.adutils.appupdatemanager.VersionControlSdk
import com.appyhigh.adutils.callbacks.*
import com.appyhigh.adutils.interfaces.*
import com.appyhigh.adutils.models.BannerAdItem
import com.appyhigh.adutils.models.NativeAdItem
import com.appyhigh.adutils.models.NativeAdItemService
import com.appyhigh.adutils.models.PreloadNativeAds
import com.appyhigh.adutils.models.apimodels.AppsData
import com.appyhigh.adutils.utils.AdMobUtil.fetchAdLoadTimeout
import com.appyhigh.adutils.utils.AdMobUtil.fetchAdSize
import com.appyhigh.adutils.utils.AdMobUtil.fetchAdStatusFromAdId
import com.appyhigh.adutils.utils.AdMobUtil.fetchBannerAdSize
import com.appyhigh.adutils.utils.AdMobUtil.fetchColor
import com.appyhigh.adutils.utils.AdMobUtil.fetchPrimaryById
import com.appyhigh.adutils.utils.AdMobUtil.fetchRefreshTime
import com.appyhigh.adutils.utils.AdMobUtil.fetchSecondaryById
import com.appyhigh.adutils.utils.container.AppPref
import com.google.ads.consent.*
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.*
import com.google.android.gms.ads.admanager.AdManagerAdRequest
import com.google.android.gms.ads.admanager.AdManagerInterstitialAd
import com.google.android.gms.ads.admanager.AdManagerInterstitialAdLoadCallback
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.*
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.GoogleApiAvailability.GOOGLE_PLAY_SERVICES_VERSION_CODE
import java.net.MalformedURLException
import java.net.URL
import java.util.*
import kotlin.concurrent.fixedRateTimer


object AdSdk {

    enum class REFRESH_STATE {
        REFRESH_ON, REFRESH_OFF
    }

    private var isInitialized: Boolean = false
    private var TAG = "AdSdk"
    private var bannerAdRefreshTimer = 45000L
    private var nativeAdRefreshTimer = 45000L
    private var nativeRefresh = REFRESH_STATE.REFRESH_ON
    private var bannerRefresh = REFRESH_STATE.REFRESH_ON

    private var lastBGColor: Any? = null
    private var lastTColor1: Int? = null
    private var lastTColor2: Int? = null
    private var lastHeight: Int = 300

    var consentInformation: ConsentInformation? = null

    fun loadNPAForm(
        privacyPolicyLink: String,
        activity: Activity,
        pubValue: String,
        testDevice: String = "E35970227779CE2270F80558896619BC"
    ) {
        var form: ConsentForm? = null
        consentInformation = ConsentInformation.getInstance(activity)
        consentInformation?.addTestDevice(testDevice)
        consentInformation?.debugGeography = DebugGeography.DEBUG_GEOGRAPHY_EEA
        val publisherIds = arrayOf(pubValue)
        consentInformation?.requestConsentInfoUpdate(
            publisherIds,
            object : ConsentInfoUpdateListener {
                override fun onConsentInfoUpdated(consentStatus: ConsentStatus) {
                    var privacyUrl: URL? = null
                    try {
                        privacyUrl = URL(privacyPolicyLink)
                    } catch (e: MalformedURLException) {
                        e.printStackTrace()
                    }

                    form = ConsentForm.Builder(activity, privacyUrl)
                        .withListener(object : ConsentFormListener() {
                            override fun onConsentFormLoaded() {
                                activity.runOnUiThread {
                                    try {
                                        if (!activity.isFinishing) {
                                            form?.show()
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }

                            override fun onConsentFormOpened() {
                            }

                            override fun onConsentFormClosed(
                                consentStatus: ConsentStatus, userPrefersAdFree: Boolean
                            ) {
                                if (consentStatus == ConsentStatus.NON_PERSONALIZED) {
                                    extras.putString("npa", "1")
                                }
                            }

                            override fun onConsentFormError(errorDescription: String) {
                            }
                        })
                        .withPersonalizedAdsOption()
                        .withNonPersonalizedAdsOption()
                        .build()
                    if (consentInformation?.consentStatus == ConsentStatus.UNKNOWN) {
                        form?.load()
                    }
                }

                override fun onFailedToUpdateConsentInfo(errorDescription: String) {
                }
            })


    }


    /**
     * Call initialize with you Application class object
     *
     * @param app -> Pass your application context here
     * @param bannerRefreshTimer -> Pass 0L to stop refresh or pass your required refresh interval in milliseconds. (Default Value is 45 seconds)
     * @param nativeRefreshTimer -> Pass 0L to stop refresh or pass your required refresh interval in milliseconds. (Default Value is 45 seconds)
     */

    fun isInitialised(): Boolean = isInitialized
    lateinit var app:Application

    fun initialize(
        app: Application,
        activity: Activity,
        version: Int,
        anyView:View,
        bannerRefreshTimer: Long = 45000L,
        nativeRefreshTimer: Long = 45000L,
        testDevice: String? = null,
        preloadingNativeAdList: HashMap<String, PreloadNativeAds>? = null,
        packageName: String = app.packageName,
        dynamicAdsFetchThresholdInSecs: Int = 24 * 60 * 60,
        fetchingCallback: FetchingCallback? = null,
        versionControlCallback:VersionControlCallback?
    ) {
        this.app = app
        val inflater = LayoutInflater.from(app)
        AppPref.getInstance(app.applicationContext)
        if (consentInformation == null) {
            consentInformation = ConsentInformation.getInstance(app)
        }
        if (consentInformation?.consentStatus == ConsentStatus.NON_PERSONALIZED) {
            extras.putString("npa", "1")
        }
        if (testDevice != null) {
            val build = RequestConfiguration.Builder()
                .setTestDeviceIds(listOf(testDevice)).build()
            MobileAds.setRequestConfiguration(build)
        }
        MobileAds.initialize(app) {
            isInitialized = true
            preloadNativeAdList = preloadingNativeAdList
            val context = app.applicationContext
            if (context != null) {
                if (preloadNativeAdList != null && inflater != null) {
                    preloadAds(inflater, context)
                }

                DynamicsAds.getDynamicAds(
                    context,
                    packageName,
                    dynamicAdsFetchThresholdInSecs,
                    preloadingNativeAdList,
                    fetchingCallback
                )
            }
            initVersionController(
                activity,
                version,
                anyView,
                versionControlCallback
            )
            fetchingCallback?.OnInitialized()
        }

        if (isGooglePlayServicesAvailable(app)) {
            if (app == null) {
                bannerAdRefreshTimer = bannerRefreshTimer
                nativeAdRefreshTimer = nativeRefreshTimer
                refreshBanner(null)
                refreshNative(null)
                refreshNativeService(null)
            }
        }
    }

    fun initializeForService(
        app: Application,
        bannerRefreshTimer: Long = 45000L,
        nativeRefreshTimer: Long = 45000L,
        testDevice: String? = null,
        preloadingNativeAdList: HashMap<String, PreloadNativeAds>? = null,
        packageName: String = app.packageName,
        dynamicAdsFetchThresholdInSecs: Int = 24 * 60 * 60,
        fetchingCallback: FetchingCallback? = null
    ) {
        this.app = app
        val inflater = LayoutInflater.from(app)
        AppPref.getInstance(app.applicationContext)
        if (consentInformation == null) {
            consentInformation = ConsentInformation.getInstance(app)
        }
        if (consentInformation?.consentStatus == ConsentStatus.NON_PERSONALIZED) {
            extras.putString("npa", "1")
        }
        if (testDevice != null) {
            val build = RequestConfiguration.Builder()
                .setTestDeviceIds(listOf(testDevice)).build()
            MobileAds.setRequestConfiguration(build)
        }
        MobileAds.initialize(app) {
            isInitialized = true
            preloadNativeAdList = preloadingNativeAdList
            val context = app?.applicationContext
            if (context != null) {
                if (preloadNativeAdList != null && inflater != null) {
                    preloadAds(inflater, context)
                }

                DynamicsAds.getDynamicAds(
                    context,
                    packageName,
                    dynamicAdsFetchThresholdInSecs,
                    preloadingNativeAdList,
                    fetchingCallback
                )
            }
            fetchingCallback?.OnInitialized()
        }

        if (isGooglePlayServicesAvailable(app)) {
            if (app == null) {
                bannerAdRefreshTimer = bannerRefreshTimer
                nativeAdRefreshTimer = nativeRefreshTimer
                refreshBanner(null)
                refreshNative(null)
                refreshNativeService(null)
            }
        }
    }

    fun preloadAds(application: Application,preloadingNativeAdList: HashMap<String, PreloadNativeAds>){
        val context = application.applicationContext
        val inflater = LayoutInflater.from(app)
        preloadNativeAdList = preloadingNativeAdList
        if (preloadNativeAdList != null && inflater != null) {
            preloadAds(inflater, context)
        }
    }

    internal fun refreshBanner(adName:String?){
        if (isGooglePlayServicesAvailable(app)) {
            for (item in AdUtilConstants.bannerAdLifeCycleHashMap) {
                if (bannerRefresh == REFRESH_STATE.REFRESH_ON && adName.equals(item.value.adName) && item.value.activity.fetchRefreshTime(item.value.adName) != 0L) {
                    fixedRateTimer(item.value.adName, false, item.value.activity.fetchRefreshTime(item.value.adName), item.value.activity.fetchRefreshTime(item.value.adName)) {
                        if (item.value.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) && bannerRefresh == REFRESH_STATE.REFRESH_ON &&
                            item.value.activity.fetchAdStatusFromAdId(item.value.adName))
                            Handler(Looper.getMainLooper()).post {
                                loadBannerAdRefresh(
                                    item.value.activity,
                                    item.value.id,
                                    item.value.lifecycle,
                                    item.value.viewGroup,
                                    item.value.adUnit,
                                    item.value.adName,
                                    item.value.adSize,
                                    item.value.bannerAdLoadCallback,
                                    showLoadingMessage = item.value.showLoadingMessage,
                                    isAdmanager = item.value.isAdManager
                                )
                                Log.d(TAG, "refreshBanner: "+"${item.value.adName}:"+ System.currentTimeMillis()/1000)
                            }
                    }
                }
            }
        }
    }

    internal fun refreshNative(adName:String?){
        if (isGooglePlayServicesAvailable(app)) {
            for (item in AdUtilConstants.nativeAdLifeCycleHashMap) {
                if (nativeRefresh == REFRESH_STATE.REFRESH_ON && adName.equals(item.value.adName) && item.value.viewGroup.context.fetchRefreshTime(item.value.adName) != 0L) {
                    fixedRateTimer(item.value.adName, false, item.value.viewGroup.context.fetchRefreshTime(item.value.adName), item.value.viewGroup.context.fetchRefreshTime(item.value.adName)) {
                        val value = item.value
                        if (value.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) && nativeRefresh == REFRESH_STATE.REFRESH_ON &&
                            item.value.viewGroup.context.fetchAdStatusFromAdId(value.adName)) {
                            Handler(Looper.getMainLooper()).post {
                                loadNativeAdRefresh(
                                    value.application,
                                    value.id,
                                    value.lifecycle,
                                    value.adUnit,
                                    value.adName,
                                    value.viewGroup,
                                    value.nativeAdLoadCallback,
                                    value.layoutId,
                                    value.populator,
                                    adType = value.viewId,
                                    background = value.background,
                                    textColor1 = value.textColor1,
                                    textColor2 = value.textColor2,
                                    mediaMaxHeight = value.mediaMaxHeight,
                                    loadingTextSize = value.textSize,
                                    contentURL = value.contentURL,
                                    showLoadingMessage = value.showLoadingMessage,
                                    neighbourContentURL = value.neighbourContentURL,
                                    isAdmanager = value.isAdManager
                                )
                                Log.d(TAG, "refreshNative: "+"${value.adName}:"+ System.currentTimeMillis()/1000)
                            }
                        }
                    }
                }
            }
        }

    }

    internal fun refreshNativeService(adName:String?){
        if (isGooglePlayServicesAvailable(app)) {
            for (item in AdUtilConstants.nativeAdLifeCycleServiceHashMap) {
                if (nativeRefresh == REFRESH_STATE.REFRESH_ON && adName.equals(item.value.adName) && item.value.context.fetchRefreshTime(item.value.adName) != 0L) {
                    fixedRateTimer(item.value.adName, false, item.value.context.fetchRefreshTime(item.value.adName), item.value.context.fetchRefreshTime(item.value.adName)) {
                        val value = item.value
                        if (value.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) && nativeRefresh == REFRESH_STATE.REFRESH_ON &&
                            value.autoRefresh && item.value.context.fetchAdStatusFromAdId(value.adName)) {
                            Handler(Looper.getMainLooper()).post {
                                loadNativeAdFromServiceRefresh(
                                    value.layoutInflater,
                                    value.context,
                                    value.lifecycle,
                                    value.adUnit,
                                    value.adName,
                                    value.viewGroup,
                                    value.nativeAdLoadCallback,
                                    background = value.background,
                                    textColor1 = value.textColor1,
                                    textColor2 = value.textColor2,
                                    mediaMaxHeight = value.mediaMaxHeight,
                                    loadingTextSize = value.textSize,
                                    id = value.id,
                                    populator = value.populator,
                                    adType = value.viewId,
                                    preloadAds = value.preloadAds,
                                    autoRefresh = value.preloadAds,
                                    contentURL = value.contentURL,
                                    neighbourContentURL = value.neighbourContentURL,
                                    showLoadingMessage = value.showLoadingMessage,
                                    isAdmanager = value.isAdmanager
                                )
                                Log.d(TAG, "refreshNativeService: "+"${value.adName}:"+ System.currentTimeMillis()/1000)
                            }
                        }
                    }
                }
            }
        }
    }

    interface FetchingCallback{
        fun OnComplete(app: AppsData?)
        fun OnInitialized()
    }

    lateinit var listener: VersionControlCallback
    var versionControlListener = object : VersionControlListener {
        override fun onUpdateDetectionSuccess(updateType: VersionControlConstants.UpdateType) {
            when (updateType) {
                VersionControlConstants.UpdateType.SOFT_UPDATE -> {
                    listener.OnSoftUpdate()
                }
                VersionControlConstants.UpdateType.HARD_UPDATE -> {
                    listener.OnHardUpdate()
                }
                else -> {
                }
            }
        }
    }

    fun initVersionController(activity: Activity,
                              version:Int,
                              view:View,
                              listener:VersionControlCallback?){
        if (listener == null)
            throw NullPointerException()
        this.listener = listener
        VersionControlSdk.initializeSdk(
            activity,
            view,
            version,
            versionControlListener
        )
    }


    fun preloadAds(layoutInflater: LayoutInflater, context: Context) {
        preloadNativeAdList?.keys?.iterator()?.forEach {
            val preloadNativeAds = preloadNativeAdList!![it]
            if (preloadNativeAds != null && preloadNativeAds.ad == null) {
                preLoadNativeAd(
                    layoutInflater,
                    context,
                    adName = preloadNativeAds.adName,
                    adType = preloadNativeAds.adSize,
                    mediaMaxHeight = preloadNativeAds.mediaMaxHeight,
                    loadingTextSize = preloadNativeAds.loadingTextSize,
                    adUnit = preloadNativeAds.adId,
                    background = null,
                    textColor1 = null,
                    textColor2 = null,
                    isAdmanager = preloadNativeAds.isAdmanager,
                    loadTimeOut = preloadNativeAds.loadTimeOut
                )
            }
        }
    }

    internal fun isGooglePlayServicesAvailable(application: Application): Boolean {
        try {
            val googleApiAvailability: GoogleApiAvailability = GoogleApiAvailability.getInstance()
            val status: Int = googleApiAvailability.isGooglePlayServicesAvailable(
                application,
                GOOGLE_PLAY_SERVICES_VERSION_CODE
            )
            if (status != ConnectionResult.SUCCESS) {
                return false
            }
            return true
        } catch (e: Exception) {
            return false
        }
    }

    /**
     * Call initialize with you Application class object
     *
     * @param appOpenAdUnit -> Pass an app open ad unit id if you wish to ad an app open ad
     * @param appOpenAdCallback -> This is the nullable listener for app open ad callbacks
     * @param backgroundThreshold -> Minimum time in millis that app should remain in background before showing [AppOpenAd]
     **/
    fun attachAppOpenAdManager(
        application: Application,
        appOpenAdUnit: String,
        adName: String,
         appOpenAdCallback: AppOpenAdCallback? = null,
        backgroundThreshold: Int = 30000,
        isShownOnlyOnce: Boolean = false,
        isAdmanager:Boolean = false,
        loadTimeOut:Int,
        isPremium:Boolean = false
    ) {
        if (isInitialized){
            if (application != null){
                if (AppPref.getBoolean(application,AppPref.showAppAds) && application.fetchAdStatusFromAdId(adName)) {
                    if (!AppOpenManager.initialized) {
                        val appOpenManager =
                            AppOpenManager(
                                application,
                                appOpenAdUnit,
                                adName,
                                isShownOnlyOnce,
                                backgroundThreshold,
                                appOpenAdCallback,
                                isAdmanager,
                                loadTimeOut,
                                isPremium
                            )
                        appOpenAdCallback?.onInitSuccess(appOpenManager)
                    }
                    try {
                        AppOpenManager.Companion.isPremiumUser = isPremium
                    }catch (e:Exception){
                        e.printStackTrace()
                    }
                }
            }
            else {
                appOpenAdCallback?.onContextFailed()
            }
        }
        else{
            appOpenAdCallback?.onAdFailedToLoad(LoadAdError(404, "Ads SDK not (initialized / properly initial). Try initializing again.", "",null,null))
        }

    }

    /**
     * Load an App Open Ad for Splash Screen
     *
     * @param activity -> instance of your Activity which will load and display AppOpen Ad
     * @param appOpenAdUnit -> the ad-unit id for app open ad
     * @param showWhenLoaded -> if true, ad will be shown as soon as its loaded after calling callbacks
     * @param appOpenAdCallback a nullable callback, __*if this is null ad will be shown as soon as its loaded*__
     */
    fun loadAppOpenAd(
        activity: Activity,
        appOpenAdUnit: String,
        adName:String,
        showWhenLoaded: Boolean,
        appOpenAdCallback: AppOpenAdLoadCallback? = null,
        isAdmanager: Boolean = false,
        loadTimeOut:Int
    ) {
        if (isInitialized){
            if (activity != null){
                if (AppPref.getBoolean(activity.applicationContext,AppPref.showAppAds) && activity.fetchAdStatusFromAdId(adName)) {
                    var fetchedTimer:Int = activity.fetchAdLoadTimeout(adName)
                    if (fetchedTimer == 0){
                        fetchedTimer = loadTimeOut
                    }
                    var primaryIds = activity.fetchPrimaryById(adName)
                    var secondaryIds = activity.fetchSecondaryById(adName)

                    Log.d("appopen_new",adName+"OnStart:" + System.currentTimeMillis()/1000)
                    if (primaryIds.size > 0){
                        loadAppOpenAd(
                            activity,
                            adName,
                            fetchedTimer,
                            primaryIds,
                            appOpenAdCallback,
                            object: AppOpenInternalCallback{
                                override fun onSuccess(ad: AppOpenAd) {
                                    Log.d("appopen_new", adName+"onSuccess: Primary Shown" + System.currentTimeMillis()/1000)

                                    appOpenAdCallback?.onAdLoaded(ad)
                                    if (showWhenLoaded)
                                        ad.show(activity)
                                }

                                override fun onFailed(loadAdError: LoadAdError?) {
                                    if (secondaryIds.size > 0){
                                        loadAppOpenAd(
                                            activity,
                                            adName,
                                            fetchedTimer,
                                            secondaryIds,
                                            appOpenAdCallback,
                                            object: AppOpenInternalCallback{
                                                override fun onSuccess(ad: AppOpenAd) {
                                                    Log.d("appopen_new", adName+"onSuccess: First Secondary Shown" + System.currentTimeMillis()/1000)
                                                    appOpenAdCallback?.onAdLoaded(ad)
                                                    if (showWhenLoaded)
                                                        ad.show(activity)
                                                }

                                                override fun onFailed(loadAdError: LoadAdError?) {
                                                    loadAppOpenAd(
                                                        activity,
                                                        adName,
                                                        fetchedTimer,
                                                        listOf(appOpenAdUnit),
                                                        appOpenAdCallback,
                                                        object: AppOpenInternalCallback{
                                                            override fun onSuccess(ad: AppOpenAd) {
                                                                Log.d("appopen_new", adName+"onSuccess: First Fallback Shown" + System.currentTimeMillis()/1000)
                                                                appOpenAdCallback?.onAdLoaded(ad)
                                                                if (showWhenLoaded)
                                                                    ad.show(activity)
                                                            }

                                                            override fun onFailed(loadAdError: LoadAdError?) {
                                                                appOpenAdCallback?.onAdFailedToLoad(loadAdError)
                                                            }

                                                        },
                                                        isAdmanager
                                                    )
                                                }

                                            },
                                            isAdmanager
                                        )
                                    }
                                    else {
                                        loadAppOpenAd(
                                            activity,
                                            adName,
                                            fetchedTimer,
                                            listOf(appOpenAdUnit),
                                            appOpenAdCallback,
                                            object: AppOpenInternalCallback{
                                                override fun onSuccess(ad: AppOpenAd) {
                                                    Log.d("appopen_new", adName+"onSuccess: First else Fallback Shown" + System.currentTimeMillis()/1000)
                                                    appOpenAdCallback?.onAdLoaded(ad)
                                                    if (showWhenLoaded)
                                                        ad.show(activity)
                                                }

                                                override fun onFailed(loadAdError: LoadAdError?) {
                                                    appOpenAdCallback?.onAdFailedToLoad(loadAdError)
                                                }

                                            },
                                            isAdmanager
                                        )
                                    }
                                }

                            },
                            isAdmanager
                        )
                    }
                    else if (secondaryIds.size > 0){
                        loadAppOpenAd(
                            activity,
                            adName,
                            fetchedTimer,
                            secondaryIds,
                            appOpenAdCallback,
                            object: AppOpenInternalCallback{
                                override fun onSuccess(ad: AppOpenAd) {
                                    Log.d("appopen_new", adName+"onSuccess: Second Secondary Shown" + System.currentTimeMillis() / 1000)
                                    appOpenAdCallback?.onAdLoaded(ad)
                                    if (showWhenLoaded)
                                        ad.show(activity)
                                }

                                override fun onFailed(loadAdError: LoadAdError?) {
                                    loadAppOpenAd(
                                        activity,
                                        adName,
                                        fetchedTimer,
                                        listOf(appOpenAdUnit),
                                        appOpenAdCallback,
                                        object: AppOpenInternalCallback{
                                            override fun onSuccess(ad: AppOpenAd) {
                                                Log.d("appopen_new", adName+"onSuccess: Second Fallback Shown" + System.currentTimeMillis() / 1000)
                                                appOpenAdCallback?.onAdLoaded(ad)
                                                if (showWhenLoaded)
                                                    ad.show(activity)
                                            }

                                            override fun onFailed(loadAdError: LoadAdError?) {
                                                appOpenAdCallback?.onAdFailedToLoad(loadAdError)
                                            }

                                        },
                                        isAdmanager
                                    )
                                }

                            },
                            isAdmanager
                        )
                    }
                    else {
                        loadAppOpenAd(
                            activity,
                            adName,
                            fetchedTimer,
                            listOf(appOpenAdUnit),
                            appOpenAdCallback,
                            object: AppOpenInternalCallback{
                                override fun onSuccess(ad: AppOpenAd) {
                                    Log.d("appopen_new", adName+"onSuccess: Else Fallback Shown" + System.currentTimeMillis() / 1000)
                                    appOpenAdCallback?.onAdLoaded(ad)
                                    if (showWhenLoaded)
                                        ad.show(activity)
                                }

                                override fun onFailed(loadAdError: LoadAdError?) {
                                    appOpenAdCallback?.onAdFailedToLoad(loadAdError)
                                }

                            },
                            isAdmanager
                        )
                    }
                }
            }
            else{
                appOpenAdCallback?.onContextFailed()
            }
        }
        else {
            appOpenAdCallback?.onAdFailedToLoad(LoadAdError(404, "Ads SDK not (initialized / properly initial). Try initializing again.", "",null,null))
        }
    }

    internal fun loadAppOpenAd(
        application:Context,
        adName:String,
        fetchedTimer:Int,
        primaryIds:List<String>,
        appOpenAdCallback: AppOpenAdLoadCallback? = null,
        appOpenInternalCallback: AppOpenInternalCallback? = null,
        isAdmanager: Boolean = false
    ){
        var appOpenAd: AppOpenAd? = null
        var AdError: LoadAdError? = null
        object : CountDownTimer(fetchedTimer.toLong(), 500) {
            override fun onTick(millisUntilFinished: Long) {
                if (appOpenAd != null) {
                    appOpenInternalCallback?.onSuccess(appOpenAd!!)
                    this.cancel()
                }
            }

            override fun onFinish() {
                if (appOpenAd != null) {
                    appOpenInternalCallback?.onSuccess(appOpenAd!!)
                }
                else
                    appOpenInternalCallback?.onFailed(AdError)
            }
        }.start()
        for (appOpenAdUnit in primaryIds){
            val loadCallback = object : AppOpenAd.AppOpenAdLoadCallback() {

                override fun onAdLoaded(ad: AppOpenAd) {
                    if (appOpenAd == null)
                        appOpenAd = ad
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            appOpenAdCallback?.onAdClosed()
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            appOpenAdCallback?.onAdFailedToShow(adError)
                        }
                    }
//                    appOpenAdCallback?.onAdLoaded(ad)
//                    if (showWhenLoaded)
//                        ad.show(activity)
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.d(TAG, loadAdError.message)
                    if (appOpenAd == null)
                        appOpenAd = null
                    AdError = loadAdError
                    Log.d(TAG, "onAdFailedToLoad: "+adName+" : "+appOpenAdUnit+" : "+loadAdError.message)
//                    appOpenAdCallback?.onAdFailedToLoad(loadAdError)
                }

            }
            if (!isAdmanager){
                AppOpenAd.load(
                application, appOpenAdUnit,
                AdRequest.Builder()
                    .addNetworkExtrasBundle(
                        AdMobAdapter::class.java,
                        getConsentEnabledBundle()
                    )
                    .build(),
                AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT, loadCallback
            )
            }
            else {
                AppOpenAd.load(
                application, appOpenAdUnit,
                AdManagerAdRequest.Builder()
                    .build(),
                AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT, loadCallback
            )
            }
        }

    }

    val extras = Bundle()
    fun getConsentEnabledBundle(): Bundle {
        return extras
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
        application: Context,
        lifecycle: Lifecycle,
        viewGroup: ViewGroup,
        adUnit: String,
        adName: String,
        adSize: AdSize,
        bannerAdLoadCallback: BannerAdLoadCallback?,
        contentURL: String? = null,
        neighbourContentURL: List<String>? = null,
        loadingTextSize: Int = 24,
        textColor1: Int = Color.BLACK,
        background: Int = Color.LTGRAY,
        showLoadingMessage: Boolean = true,
        isAdmanager:Boolean = false,
        loadTimeOut:Int
    ) {
        if (isInitialized){
            if (application != null){
                if (AppPref.getBoolean(application,AppPref.showAppAds) && application.fetchAdStatusFromAdId(adName)) {
                    loadBannerAd(
                        application,
                        System.currentTimeMillis(),
                        lifecycle,
                        viewGroup,
                        adUnit,
                        adName,
                        application.fetchBannerAdSize(adName,adSize),
                        bannerAdLoadCallback,
                        contentURL,
                        neighbourContentURL,
                        loadingTextSize,
                        textColor1,
                        background,
                        showLoadingMessage,
                        isAdmanager,
                        loadTimeOut
                    )
                }
                else {
                    viewGroup.visibility = GONE
                }
            }
            else{
                bannerAdLoadCallback?.onContextFailed()
                viewGroup.visibility = GONE
            }

        }
        else{
            viewGroup.visibility = GONE
            bannerAdLoadCallback?.onAdFailedToLoad(LoadAdError(404, "Ads SDK not (initialized / properly initial). Try initializing again.", "",null,null))
        }
    }

    internal fun loadBannerAd(
        application: Context,
        id: Long,
        lifecycle: Lifecycle,
        viewGroup: ViewGroup,
        adUnit: String,
        adName: String,
        adSize: AdSize,
        bannerAdLoadCallback: BannerAdLoadCallback?,
        contentURL: String? = null,
        neighbourContentURL: List<String>? = null,
        loadingTextSize: Int = 24,
        textColor1: Int = Color.BLACK,
        background: Int = Color.LTGRAY,
        showLoadingMessage: Boolean = true,
        isAdmanager:Boolean = false,
        loadTimeOut:Int
    ) {
        if (application != null && adUnit != "STOP" && AppPref.getBoolean(application,AppPref.showAppAds) && application.fetchAdStatusFromAdId(adName)) {
            viewGroup.visibility = VISIBLE
            if (adUnit.isBlank()) return

            var fetchedTimer:Int = application.fetchAdLoadTimeout(adName)
            if (fetchedTimer == 0){
                fetchedTimer = loadTimeOut
            }
            var primaryIds = application.fetchPrimaryById(adName)
            var secondaryIds = application.fetchSecondaryById(adName)


            val inflate = View.inflate(application, R.layout.shimmer_banner, null)
            when (adSize) {
                AdSize.BANNER -> {
                    inflate.findViewById<LinearLayout>(R.id.shim_default).visibility = View.VISIBLE
                }
                AdSize.LARGE_BANNER -> {
                    inflate.findViewById<LinearLayout>(R.id.shim_small).visibility = View.VISIBLE
                }
                AdSize.MEDIUM_RECTANGLE -> {
                    inflate.findViewById<LinearLayout>(R.id.shim_bigv1).visibility = View.VISIBLE
                }
                else -> {
                    inflate.findViewById<LinearLayout>(R.id.shim_default).visibility = View.VISIBLE
                }
            }
            viewGroup.removeAllViews()

            if (showLoadingMessage) {
                viewGroup.addView(inflate)
            }

            if (!isAdmanager){
                Log.d("banner",adName+"OnStart:" + System.currentTimeMillis()/1000)
                if (primaryIds.size > 0){
                    loadBannerAd(
                        application,
                        id,
                        lifecycle,
                        viewGroup,
                        adName,
                        adSize,
                        showLoadingMessage,
                        bannerAdLoadCallback,
                        contentURL,
                        neighbourContentURL,
                        primaryIds,
                        fetchedTimer,
                        isAdmanager,
                        object :BannerInternalCallback{
                            override fun onSuccess() {
                                refreshBanner(adName)
                                Log.d("banner", adName+"onSuccess: Primary Shown" + System.currentTimeMillis()/1000)

                                bannerAdLoadCallback?.onAdLoaded()
                            }

                            override fun onFailed(loadAdError: LoadAdError?) {
                                if(secondaryIds.size > 0){
                                    loadBannerAd(
                                        application,
                                        id,
                                        lifecycle,
                                        viewGroup,
                                        adName,
                                        adSize,
                                        showLoadingMessage,
                                        bannerAdLoadCallback,
                                        contentURL,
                                        neighbourContentURL,
                                        secondaryIds,
                                        fetchedTimer,
                                        isAdmanager,
                                        object :BannerInternalCallback{
                                            override fun onSuccess() {
                                                refreshBanner(adName)
                                                Log.d("banner", adName+"onSuccess: Second Secondary Shown" + System.currentTimeMillis() / 1000)
                                                bannerAdLoadCallback?.onAdLoaded()
                                            }

                                            override fun onFailed(loadAdError: LoadAdError?) {
                                                loadBannerAd(
                                                    application,
                                                    id,
                                                    lifecycle,
                                                    viewGroup,
                                                    adName,
                                                    adSize,
                                                    showLoadingMessage,
                                                    bannerAdLoadCallback,
                                                    contentURL,
                                                    neighbourContentURL,
                                                    listOf(adUnit),
                                                    fetchedTimer,
                                                    isAdmanager,
                                                    object :BannerInternalCallback{
                                                        override fun onSuccess() {
                                                            refreshBanner(adName)
                                                            Log.d("banner", adName+"onSuccess: Second Fallback Shown" + System.currentTimeMillis() / 1000)
                                                            bannerAdLoadCallback?.onAdLoaded()
                                                        }

                                                        override fun onFailed(loadAdError: LoadAdError?) {
                                                            bannerAdLoadCallback?.onAdFailedToLoad(loadAdError)
                                                        }

                                                    }
                                                )
                                            }

                                        }
                                    )
                                }
                                else {
                                    loadBannerAd(
                                        application,
                                        id,
                                        lifecycle,
                                        viewGroup,
                                        adName,
                                        adSize,
                                        showLoadingMessage,
                                        bannerAdLoadCallback,
                                        contentURL,
                                        neighbourContentURL,
                                        listOf(adUnit),
                                        fetchedTimer,
                                        isAdmanager,
                                        object :BannerInternalCallback{
                                            override fun onSuccess() {
                                                refreshBanner(adName)
                                                Log.d("banner", adName+"onSuccess: Else Fallback Shown" + System.currentTimeMillis() / 1000)
                                                bannerAdLoadCallback?.onAdLoaded()
                                            }

                                            override fun onFailed(loadAdError: LoadAdError?) {
                                                bannerAdLoadCallback?.onAdFailedToLoad(loadAdError)
                                            }

                                        }
                                    )
                                }
                            }

                        }
                    )
                }
                else if(secondaryIds.size > 0){
                    loadBannerAd(
                        application,
                        id,
                        lifecycle,
                        viewGroup,
                        adName,
                        adSize,
                        showLoadingMessage,
                        bannerAdLoadCallback,
                        contentURL,
                        neighbourContentURL,
                        secondaryIds,
                        fetchedTimer,
                        isAdmanager,
                        object :BannerInternalCallback{
                            override fun onSuccess() {
                                refreshBanner(adName)
                                Log.d("banner", adName+"onSuccess: Second Secondary Shown" + System.currentTimeMillis() / 1000)
                                bannerAdLoadCallback?.onAdLoaded()
                            }

                            override fun onFailed(loadAdError: LoadAdError?) {
                                loadBannerAd(
                                    application,
                                    id,
                                    lifecycle,
                                    viewGroup,
                                    adName,
                                    adSize,
                                    showLoadingMessage,
                                    bannerAdLoadCallback,
                                    contentURL,
                                    neighbourContentURL,
                                    listOf(adUnit),
                                    fetchedTimer,
                                    isAdmanager,
                                    object :BannerInternalCallback{
                                        override fun onSuccess() {
                                            refreshBanner(adName)
                                            Log.d("banner", adName+"onSuccess: Second Fallback Shown" + System.currentTimeMillis() / 1000)
                                            bannerAdLoadCallback?.onAdLoaded()
                                        }

                                        override fun onFailed(loadAdError: LoadAdError?) {
                                            bannerAdLoadCallback?.onAdFailedToLoad(loadAdError)
                                        }

                                    }
                                )
                            }

                        }
                    )
                }
                else {
                    loadBannerAd(
                        application,
                        id,
                        lifecycle,
                        viewGroup,
                        adName,
                        adSize,
                        showLoadingMessage,
                        bannerAdLoadCallback,
                        contentURL,
                        neighbourContentURL,
                        listOf(adUnit),
                        fetchedTimer,
                        isAdmanager,
                        object :BannerInternalCallback{
                            override fun onSuccess() {
                                refreshBanner(adName)
                                Log.d("banner", adName+"onSuccess: Else Fallback Shown" + System.currentTimeMillis() / 1000)
                                bannerAdLoadCallback?.onAdLoaded()
                            }

                            override fun onFailed(loadAdError: LoadAdError?) {
                                bannerAdLoadCallback?.onAdFailedToLoad(loadAdError)
                            }

                        }
                    )
                }
            }
            else {
                Log.d("banner_admanager",adName+"OnStart:" + System.currentTimeMillis()/1000)
                if (primaryIds.size > 0){
                    loadBannerAdManager(
                        application,
                        id,
                        lifecycle,
                        viewGroup,
                        adName,
                        adSize,
                        showLoadingMessage,
                        bannerAdLoadCallback,
                        contentURL,
                        neighbourContentURL,
                        primaryIds,
                        fetchedTimer,
                        isAdmanager,
                        object :BannerInternalCallback{
                            override fun onSuccess() {
                                refreshBanner(adName)
                                Log.d("banner_admanager", adName+"onSuccess: Primary Shown" + System.currentTimeMillis()/1000)

                                bannerAdLoadCallback?.onAdLoaded()
                            }

                            override fun onFailed(loadAdError: LoadAdError?) {
                                if(secondaryIds.size > 0){
                                    loadBannerAdManager(
                                        application,
                                        id,
                                        lifecycle,
                                        viewGroup,
                                        adName,
                                        adSize,
                                        showLoadingMessage,
                                        bannerAdLoadCallback,
                                        contentURL,
                                        neighbourContentURL,
                                        secondaryIds,
                                        fetchedTimer,
                                        isAdmanager,
                                        object :BannerInternalCallback{
                                            override fun onSuccess() {
                                                refreshBanner(adName)
                                                Log.d("banner_admanager", adName+"onSuccess: Second Secondary Shown" + System.currentTimeMillis() / 1000)
                                                bannerAdLoadCallback?.onAdLoaded()
                                            }

                                            override fun onFailed(loadAdError: LoadAdError?) {
                                                loadBannerAdManager(
                                                    application,
                                                    id,
                                                    lifecycle,
                                                    viewGroup,
                                                    adName,
                                                    adSize,
                                                    showLoadingMessage,
                                                    bannerAdLoadCallback,
                                                    contentURL,
                                                    neighbourContentURL,
                                                    listOf(adUnit),
                                                    fetchedTimer,
                                                    isAdmanager,
                                                    object :BannerInternalCallback{
                                                        override fun onSuccess() {
                                                            refreshBanner(adName)
                                                            Log.d("banner_admanager", adName+"onSuccess: Second Fallback Shown" + System.currentTimeMillis() / 1000)
                                                            bannerAdLoadCallback?.onAdLoaded()
                                                        }

                                                        override fun onFailed(loadAdError: LoadAdError?) {
                                                            bannerAdLoadCallback?.onAdFailedToLoad(loadAdError)
                                                        }

                                                    }
                                                )
                                            }

                                        }
                                    )
                                }
                                else {
                                    loadBannerAdManager(
                                        application,
                                        id,
                                        lifecycle,
                                        viewGroup,
                                        adName,
                                        adSize,
                                        showLoadingMessage,
                                        bannerAdLoadCallback,
                                        contentURL,
                                        neighbourContentURL,
                                        listOf(adUnit),
                                        fetchedTimer,
                                        isAdmanager,
                                        object :BannerInternalCallback{
                                            override fun onSuccess() {
                                                refreshBanner(adName)
                                                Log.d("banner_admanager", adName+"onSuccess: Else Fallback Shown" + System.currentTimeMillis() / 1000)
                                                bannerAdLoadCallback?.onAdLoaded()
                                            }

                                            override fun onFailed(loadAdError: LoadAdError?) {
                                                bannerAdLoadCallback?.onAdFailedToLoad(loadAdError)
                                            }

                                        }
                                    )
                                }
                            }

                        }
                    )
                }
                else if(secondaryIds.size > 0){
                    loadBannerAdManager(
                        application,
                        id,
                        lifecycle,
                        viewGroup,
                        adName,
                        adSize,
                        showLoadingMessage,
                        bannerAdLoadCallback,
                        contentURL,
                        neighbourContentURL,
                        secondaryIds,
                        fetchedTimer,
                        isAdmanager,
                        object :BannerInternalCallback{
                            override fun onSuccess() {
                                refreshBanner(adName)
                                Log.d("banner_admanager", adName+"onSuccess: Second Secondary Shown" + System.currentTimeMillis() / 1000)
                                bannerAdLoadCallback?.onAdLoaded()
                            }

                            override fun onFailed(loadAdError: LoadAdError?) {
                                loadBannerAdManager(
                                    application,
                                    id,
                                    lifecycle,
                                    viewGroup,
                                    adName,
                                    adSize,
                                    showLoadingMessage,
                                    bannerAdLoadCallback,
                                    contentURL,
                                    neighbourContentURL,
                                    listOf(adUnit),
                                    fetchedTimer,
                                    isAdmanager,
                                    object :BannerInternalCallback{
                                        override fun onSuccess() {
                                            refreshBanner(adName)
                                            Log.d("banner_admanager", adName+"onSuccess: Second Fallback Shown" + System.currentTimeMillis() / 1000)
                                            bannerAdLoadCallback?.onAdLoaded()
                                        }

                                        override fun onFailed(loadAdError: LoadAdError?) {
                                            bannerAdLoadCallback?.onAdFailedToLoad(loadAdError)
                                        }

                                    }
                                )
                            }

                        }
                    )
                }
                else {
                    loadBannerAdManager(
                        application,
                        id,
                        lifecycle,
                        viewGroup,
                        adName,
                        adSize,
                        showLoadingMessage,
                        bannerAdLoadCallback,
                        contentURL,
                        neighbourContentURL,
                        listOf(adUnit),
                        fetchedTimer,
                        isAdmanager,
                        object :BannerInternalCallback{
                            override fun onSuccess() {
                                refreshBanner(adName)
                                Log.d("banner_admanager", adName+"onSuccess: Else Fallback Shown" + System.currentTimeMillis() / 1000)
                                bannerAdLoadCallback?.onAdLoaded()
                            }

                            override fun onFailed(loadAdError: LoadAdError?) {
                                bannerAdLoadCallback?.onAdFailedToLoad(loadAdError)
                            }

                        }
                    )
                }
            }
        }
    }

    internal fun loadBannerAd(
        activity: Context,
        id: Long,
        lifecycle: Lifecycle,
        viewGroup: ViewGroup,
        adName: String,
        adSize: AdSize,
        showLoadingMessage: Boolean,
        bannerAdLoadCallback: BannerAdLoadCallback?,
        contentURL: String?,
        neighbourContentURL: List<String>?,
        primaryIds: List<String>,
        fetchedTimer: Int,
        isAdmanager:Boolean,
        bannerInternalCallback: BannerInternalCallback
    ) {
        var isShown = false
        var AdError: LoadAdError? = null
        object : CountDownTimer(fetchedTimer.toLong(), 500) {
            override fun onTick(millisUntilFinished: Long) {
                if (isShown) {
                    bannerInternalCallback.onSuccess()
                    this.cancel()
                }
            }

            override fun onFinish() {
                if (isShown) {
                    bannerInternalCallback.onSuccess()
                }
                else
                    bannerInternalCallback.onFailed(AdError)
            }
        }.start()
        for (adUnit in primaryIds){

            lifecycle.addObserver(object : LifecycleObserver {
                @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
                fun onDestroy() {
                    AdUtilConstants.bannerAdLifeCycleHashMap.remove(id)
                }
            })
            val builder = AdRequest.Builder()
                .addNetworkExtrasBundle(AdMobAdapter::class.java, getConsentEnabledBundle())
            contentURL?.let { builder.setContentUrl(it) }
            neighbourContentURL?.let { builder.setNeighboringContentUrls(it) }
            val adRequest = builder
                .build()
            val mAdView = AdView(viewGroup.context)
            mAdView.setAdSize(adSize)
            mAdView.adUnitId = adUnit
            mAdView.loadAd(adRequest)

            mAdView.adListener = object : AdListener() {
                // Code to be executed when an ad finishes loading.
                override fun onAdLoaded() {
                    if (!isShown){
                        viewGroup.removeAllViews()
                        viewGroup.addView(mAdView)
                        if (AdUtilConstants.bannerAdLifeCycleHashMap[id] == null) {
                            AdUtilConstants.bannerAdLifeCycleHashMap[id] =
                                BannerAdItem(
                                    activity,
                                    id,
                                    lifecycle,
                                    viewGroup,
                                    mAdView.adUnitId,
                                    adSize,
                                    adName,
                                    showLoadingMessage,
                                    bannerAdLoadCallback,
                                    contentURL, neighbourContentURL,
                                    isAdmanager
                                )
                        }
                        isShown = true
                    }
                }

                // Code to be executed when an ad request fails.
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    AdError = adError
                    Log.d(TAG, "onAdFailedToLoad: "+adName+" : "+adUnit+" : "+adError.message)
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

    internal fun loadBannerAdManager(
        activity: Context,
        id: Long,
        lifecycle: Lifecycle,
        viewGroup: ViewGroup,
        adName: String,
        adSize: AdSize,
        showLoadingMessage: Boolean,
        bannerAdLoadCallback: BannerAdLoadCallback?,
        contentURL: String?,
        neighbourContentURL: List<String>?,
        primaryIds: List<String>,
        fetchedTimer: Int,
        isAdmanager: Boolean,
        bannerInternalCallback: BannerInternalCallback
    ) {

        var isShown = false
        var AdError: LoadAdError? = null
        object : CountDownTimer(fetchedTimer.toLong(), 500) {
            override fun onTick(millisUntilFinished: Long) {
                if (isShown) {
                    bannerInternalCallback.onSuccess()
                    this.cancel()
                }
            }

            override fun onFinish() {
                if (isShown) {
                    bannerInternalCallback.onSuccess()
                }
                else
                    bannerInternalCallback.onFailed(AdError)
            }
        }.start()
        for (adUnit in primaryIds){

            lifecycle.addObserver(object : LifecycleObserver {
                @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
                fun onDestroy() {
                    AdUtilConstants.bannerAdLifeCycleHashMap.remove(id)
                }
            })
            val builder = AdManagerAdRequest.Builder()
                .addNetworkExtrasBundle(AdMobAdapter::class.java, getConsentEnabledBundle())
            contentURL?.let { builder.setContentUrl(it) }
            neighbourContentURL?.let { builder.setNeighboringContentUrls(it) }
            val adRequest = builder
                .build()
            val mAdView = AdView(viewGroup.context)
            mAdView.setAdSize(adSize)
            mAdView.adUnitId = adUnit
            mAdView.loadAd(adRequest)

            mAdView.adListener = object : AdListener() {
                // Code to be executed when an ad finishes loading.
                override fun onAdLoaded() {
                    if (!isShown){
                        viewGroup.removeAllViews()
                        viewGroup.addView(mAdView)
                        if (AdUtilConstants.bannerAdLifeCycleHashMap[id] == null) {
                            AdUtilConstants.bannerAdLifeCycleHashMap[id] =
                                BannerAdItem(
                                    activity,
                                    id,
                                    lifecycle,
                                    viewGroup,
                                    mAdView.adUnitId,
                                    adSize,
                                    adName,
                                    showLoadingMessage,
                                    bannerAdLoadCallback,
                                    contentURL, neighbourContentURL,
                                    isAdmanager
                                )
                        }
                        isShown = true
                    }
                }

                // Code to be executed when an ad request fails.
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    AdError = adError
                    Log.d(TAG, "onAdFailedToLoad: "+adName+" : "+adUnit+" : "+adError.message)
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

    internal fun loadBannerAdRefresh(
        application: Context,
        id: Long,
        lifecycle: Lifecycle,
        viewGroup: ViewGroup,
        adUnit: String,
        adName: String,
        adSize: AdSize,
        bannerAdLoadCallback: BannerAdLoadCallback?,
        contentURL: String? = null,
        neighbourContentURL: List<String>? = null,
        loadingTextSize: Int = 24,
        textColor1: Int = Color.BLACK,
        background: Int = Color.LTGRAY,
        showLoadingMessage: Boolean = true,
        isAdmanager: Boolean
    ) {
        if (isInitialized){
            if (application != null){
                if (adUnit != "STOP" && AppPref.getBoolean(application,AppPref.showAppAds) && application.fetchAdStatusFromAdId(adName)) {
                    if (application != null) {
                        if (adUnit.isBlank()) return
                        val inflate = View.inflate(application, R.layout.shimmer_banner, null)
                        when (adSize) {
                            AdSize.BANNER -> {
                                inflate.findViewById<LinearLayout>(R.id.shim_default).visibility = View.VISIBLE
                            }
                            AdSize.LARGE_BANNER -> {
                                inflate.findViewById<LinearLayout>(R.id.shim_small).visibility = View.VISIBLE
                            }
                            AdSize.MEDIUM_RECTANGLE -> {
                                inflate.findViewById<LinearLayout>(R.id.shim_bigv1).visibility = View.VISIBLE
                            }
                            else -> {
                                inflate.findViewById<LinearLayout>(R.id.shim_default).visibility = View.VISIBLE
                            }
                        }
                        viewGroup.removeAllViews()

                        if (showLoadingMessage) {
                            viewGroup.addView(inflate)
                        }

                        if (AdUtilConstants.bannerAdLifeCycleHashMap[id] == null) {
                            AdUtilConstants.bannerAdLifeCycleHashMap[id] =
                                BannerAdItem(
                                    application,
                                    id,
                                    lifecycle,
                                    viewGroup,
                                    adUnit,
                                    adSize,
                                    adName,
                                    showLoadingMessage,
                                    bannerAdLoadCallback,
                                    contentURL, neighbourContentURL,
                                    isAdmanager
                                )
                        }
                        lifecycle.addObserver(object : LifecycleObserver {
                            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
                            fun onDestroy() {
                                AdUtilConstants.bannerAdLifeCycleHashMap.remove(id)
                            }
                        })
                        var adRequest: AdRequest? = null
                        if (!isAdmanager){
                            val builder = AdRequest.Builder()
                                .addNetworkExtrasBundle(AdMobAdapter::class.java, getConsentEnabledBundle())
                            contentURL?.let { builder.setContentUrl(it) }
                            neighbourContentURL?.let { builder.setNeighboringContentUrls(it) }
                            adRequest = builder
                                .build()
                        }
                        else {
                            val builder = AdManagerAdRequest.Builder()
                                .addNetworkExtrasBundle(AdMobAdapter::class.java, getConsentEnabledBundle())
                            contentURL?.let { builder.setContentUrl(it) }
                            neighbourContentURL?.let { builder.setNeighboringContentUrls(it) }
                            adRequest = builder
                                .build()
                        }

                        val mAdView = AdView(viewGroup.context)
                        mAdView.setAdSize(adSize)
                        mAdView.adUnitId = adUnit
                        if (adRequest != null) {
                            mAdView.loadAd(adRequest)
                        }

                        mAdView.adListener = object : AdListener() {
                            // Code to be executed when an ad finishes loading.
                            override fun onAdLoaded() {
                                viewGroup.removeAllViews()
                                viewGroup.addView(mAdView)
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
            }
            else {
                bannerAdLoadCallback?.onContextFailed()
            }
        }
        else{
            bannerAdLoadCallback?.onAdFailedToLoad(LoadAdError(404, "Ads SDK not (initialized / properly initial). Try initializing again.", "",null,null))
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
        application: Context,
        adName: String,
        adUnit: String,
        interstitialAdUtilLoadCallback: InterstitialAdUtilLoadCallback?,
        isAdmanager:Boolean = false,
        loadTimeOut:Int
    ) {
        if (isInitialized){
            if (application != null){
                if (adUnit != "STOP" && AppPref.getBoolean(application,AppPref.showAppAds) && application.fetchAdStatusFromAdId(adName)) {
                    var mInterstitialAd: InterstitialAd? = null
                    var fetchedTimer:Int = application.fetchAdLoadTimeout(adName)
                    if (fetchedTimer == 0){
                        fetchedTimer = loadTimeOut
                    }
                    var primaryIds = application.fetchPrimaryById(adName)
                    var secondaryIds = application.fetchSecondaryById(adName)
                    Log.d("main_interstitial","OnStart:" + System.currentTimeMillis()/1000)
                    if (!isAdmanager){
                        if (primaryIds.size > 0){
                            loadInterstitialAd(
                                application,
                                adName,
                                fetchedTimer.toLong(),
                                primaryIds,
                                interstitialAdUtilLoadCallback,
                                object :InterstitialInternalCallback{
                                    override fun onSuccess(interstitialAd: InterstitialAd?,ad: AdManagerInterstitialAd?) {
                                        Log.d("main_interstitial", "onSuccess: Primary Shown" + System.currentTimeMillis()/1000)

                                        interstitialAdUtilLoadCallback?.onAdLoaded(interstitialAd = interstitialAd)
                                    }

                                    override fun onFailed(loadAdError: LoadAdError?) {
                                        if (secondaryIds.size > 0){
                                            loadInterstitialAd(
                                                application,
                                                adName,
                                                fetchedTimer.toLong(),
                                                secondaryIds,
                                                interstitialAdUtilLoadCallback,
                                                object :InterstitialInternalCallback{
                                                    override fun onSuccess(interstitialAd: InterstitialAd?,ad: AdManagerInterstitialAd?) {
                                                        Log.d("main_interstitial", "onSuccess: First Secondary Shown" + System.currentTimeMillis()/1000)
                                                        interstitialAdUtilLoadCallback?.onAdLoaded(interstitialAd = interstitialAd)
                                                    }

                                                    override fun onFailed(loadAdError: LoadAdError?) {
                                                        loadInterstitialAd(
                                                            application,
                                                            adName,
                                                            fetchedTimer.toLong(),
                                                            listOf(adUnit),
                                                            interstitialAdUtilLoadCallback,
                                                            object :InterstitialInternalCallback{
                                                                override fun onSuccess(interstitialAd: InterstitialAd?,ad: AdManagerInterstitialAd?) {
                                                                    Log.d("main_interstitial", "onSuccess: First Fallback Shown" + System.currentTimeMillis()/1000)
                                                                    interstitialAdUtilLoadCallback?.onAdLoaded(interstitialAd = interstitialAd)
                                                                }

                                                                override fun onFailed(loadAdError: LoadAdError?) {
                                                                    interstitialAdUtilLoadCallback?.onAdFailedToLoad(loadAdError, mInterstitialAd)
                                                                }
                                                            }
                                                        )
                                                    }
                                                }
                                            )
                                        }
                                        else{
                                            loadInterstitialAd(
                                                application,
                                                adName,
                                                fetchedTimer.toLong(),
                                                listOf(adUnit),
                                                interstitialAdUtilLoadCallback,
                                                object :InterstitialInternalCallback{
                                                    override fun onSuccess(interstitialAd: InterstitialAd?,ad: AdManagerInterstitialAd?) {
                                                        Log.d("main_interstitial", "onSuccess: First else Fallback Shown" + System.currentTimeMillis()/1000)
                                                        interstitialAdUtilLoadCallback?.onAdLoaded(interstitialAd = interstitialAd)
                                                    }

                                                    override fun onFailed(loadAdError: LoadAdError?) {
                                                        interstitialAdUtilLoadCallback?.onAdFailedToLoad(loadAdError, mInterstitialAd)
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            )
                        }
                        else if (secondaryIds.size > 0){
                            loadInterstitialAd(
                                application,
                                adName,
                                fetchedTimer.toLong(),
                                secondaryIds,
                                interstitialAdUtilLoadCallback,
                                object :InterstitialInternalCallback{
                                    override fun onSuccess(interstitialAd: InterstitialAd?,ad: AdManagerInterstitialAd?) {
                                        Log.d("main_interstitial", "onSuccess: Second Secondary Shown" + System.currentTimeMillis() / 1000)
                                        interstitialAdUtilLoadCallback?.onAdLoaded(interstitialAd = interstitialAd)
                                    }

                                    override fun onFailed(loadAdError: LoadAdError?) {
                                        loadInterstitialAd(
                                            application,
                                            adName,
                                            fetchedTimer.toLong(),
                                            listOf(adUnit),
                                            interstitialAdUtilLoadCallback,
                                            object :InterstitialInternalCallback{
                                                override fun onSuccess(interstitialAd: InterstitialAd?,ad: AdManagerInterstitialAd?) {
                                                    Log.d("main_interstitial", "onSuccess: Second Fallback Shown" + System.currentTimeMillis() / 1000)
                                                    interstitialAdUtilLoadCallback?.onAdLoaded(interstitialAd = interstitialAd)
                                                }

                                                override fun onFailed(loadAdError: LoadAdError?) {
                                                    interstitialAdUtilLoadCallback?.onAdFailedToLoad(loadAdError, mInterstitialAd)
                                                }
                                            }
                                        )
                                    }
                                }
                            )
                        }
                        else{
                            loadInterstitialAd(
                                application,
                                adName,
                                fetchedTimer.toLong(),
                                listOf(adUnit),
                                interstitialAdUtilLoadCallback,
                                object :InterstitialInternalCallback{
                                    override fun onSuccess(interstitialAd: InterstitialAd?,ad: AdManagerInterstitialAd?) {
                                        Log.d("main_interstitial", "onSuccess: Else Fallback Shown" + System.currentTimeMillis() / 1000)
                                        interstitialAdUtilLoadCallback?.onAdLoaded(interstitialAd = interstitialAd)
                                    }

                                    override fun onFailed(loadAdError: LoadAdError?) {
                                        interstitialAdUtilLoadCallback?.onAdFailedToLoad(loadAdError, mInterstitialAd)
                                    }
                                }
                            )
                        }
                    }
                    else {
                        if (primaryIds.size > 0){
                            loadInterstitialAdManager(
                                application,
                                adName,
                                fetchedTimer.toLong(),
                                primaryIds,
                                interstitialAdUtilLoadCallback,
                                object :InterstitialInternalCallback{
                                    override fun onSuccess(interstitialAd: InterstitialAd?,ad: AdManagerInterstitialAd?) {
                                        Log.d("main_interstitial", "onSuccess: Primary Shown" + System.currentTimeMillis()/1000)

                                        interstitialAdUtilLoadCallback?.onAdLoaded(adManagerInterstitialAd = ad)
                                    }

                                    override fun onFailed(loadAdError: LoadAdError?) {
                                        if (secondaryIds.size > 0){
                                            loadInterstitialAdManager(
                                                application,
                                                adName,
                                                fetchedTimer.toLong(),
                                                secondaryIds,
                                                interstitialAdUtilLoadCallback,
                                                object :InterstitialInternalCallback{
                                                    override fun onSuccess(interstitialAd: InterstitialAd?,ad: AdManagerInterstitialAd?) {
                                                        Log.d("main_interstitial", "onSuccess: First Secondary Shown" + System.currentTimeMillis()/1000)
                                                        interstitialAdUtilLoadCallback?.onAdLoaded(adManagerInterstitialAd = ad)
                                                    }

                                                    override fun onFailed(loadAdError: LoadAdError?) {
                                                        loadInterstitialAdManager(
                                                            application,
                                                            adName,
                                                            fetchedTimer.toLong(),
                                                            listOf(adUnit),
                                                            interstitialAdUtilLoadCallback,
                                                            object :InterstitialInternalCallback{
                                                                override fun onSuccess(interstitialAd: InterstitialAd?,ad: AdManagerInterstitialAd?) {
                                                                    Log.d("main_interstitial", "onSuccess: First Fallback Shown" + System.currentTimeMillis()/1000)
                                                                    interstitialAdUtilLoadCallback?.onAdLoaded(adManagerInterstitialAd = ad)
                                                                }

                                                                override fun onFailed(loadAdError: LoadAdError?) {
                                                                    interstitialAdUtilLoadCallback?.onAdFailedToLoad(loadAdError, mInterstitialAd)
                                                                }
                                                            }
                                                        )
                                                    }
                                                }
                                            )
                                        }
                                        else{
                                            loadInterstitialAdManager(
                                                application,
                                                adName,
                                                fetchedTimer.toLong(),
                                                listOf(adUnit),
                                                interstitialAdUtilLoadCallback,
                                                object :InterstitialInternalCallback{
                                                    override fun onSuccess(interstitialAd: InterstitialAd?,ad: AdManagerInterstitialAd?) {
                                                        Log.d("main_interstitial", "onSuccess: First else Fallback Shown" + System.currentTimeMillis()/1000)
                                                        interstitialAdUtilLoadCallback?.onAdLoaded(adManagerInterstitialAd = ad)
                                                    }

                                                    override fun onFailed(loadAdError: LoadAdError?) {
                                                        interstitialAdUtilLoadCallback?.onAdFailedToLoad(loadAdError, mInterstitialAd)
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            )
                        }
                        else if (secondaryIds.size > 0){
                            loadInterstitialAdManager(
                                application,
                                adName,
                                fetchedTimer.toLong(),
                                secondaryIds,
                                interstitialAdUtilLoadCallback,
                                object :InterstitialInternalCallback{
                                    override fun onSuccess(interstitialAd: InterstitialAd?,ad: AdManagerInterstitialAd?) {
                                        Log.d("main_interstitial", "onSuccess: Second Secondary Shown" + System.currentTimeMillis() / 1000)
                                        interstitialAdUtilLoadCallback?.onAdLoaded(adManagerInterstitialAd = ad)
                                    }

                                    override fun onFailed(loadAdError: LoadAdError?) {
                                        loadInterstitialAdManager(
                                            application,
                                            adName,
                                            fetchedTimer.toLong(),
                                            listOf(adUnit),
                                            interstitialAdUtilLoadCallback,
                                            object :InterstitialInternalCallback{
                                                override fun onSuccess(interstitialAd: InterstitialAd?,ad: AdManagerInterstitialAd?) {
                                                    Log.d("main_interstitial", "onSuccess: Second Fallback Shown" + System.currentTimeMillis() / 1000)
                                                    interstitialAdUtilLoadCallback?.onAdLoaded(adManagerInterstitialAd = ad)
                                                }

                                                override fun onFailed(loadAdError: LoadAdError?) {
                                                    interstitialAdUtilLoadCallback?.onAdFailedToLoad(loadAdError, mInterstitialAd)
                                                }
                                            }
                                        )
                                    }
                                }
                            )
                        }
                        else{
                            loadInterstitialAdManager(
                                application,
                                adName,
                                fetchedTimer.toLong(),
                                listOf(adUnit),
                                interstitialAdUtilLoadCallback,
                                object :InterstitialInternalCallback{
                                    override fun onSuccess(interstitialAd: InterstitialAd?,ad: AdManagerInterstitialAd?) {
                                        Log.d("main_interstitial", "onSuccess: Else Fallback Shown" + System.currentTimeMillis() / 1000)
                                        interstitialAdUtilLoadCallback?.onAdLoaded(adManagerInterstitialAd = ad)
                                    }

                                    override fun onFailed(loadAdError: LoadAdError?) {
                                        interstitialAdUtilLoadCallback?.onAdFailedToLoad(loadAdError, mInterstitialAd)
                                    }
                                }
                            )
                        }
                    }
                }
            }
            else{
                interstitialAdUtilLoadCallback?.onContextFailed()
            }
        }
        else {
            interstitialAdUtilLoadCallback?.onAdFailedToLoad(LoadAdError(404, "Ads SDK not (initialized / properly initial). Try initializing again.", "",null,null),null)
        }
    }

    internal fun loadInterstitialAd(
        application: Context,
        adName: String,
        timer: Long,
        primaryIds: List<String>,
        interstitialAdUtilLoadCallback: InterstitialAdUtilLoadCallback?,
        interstitialInternalCallback: InterstitialInternalCallback
    ){
        var mInterstitialAd: InterstitialAd? = null
        var AdError: LoadAdError? = null
        object : CountDownTimer(timer.toLong(), 500) {
            override fun onTick(millisUntilFinished: Long) {
                if (mInterstitialAd != null) {
                    interstitialInternalCallback.onSuccess(interstitialAd = mInterstitialAd!!)
                    this.cancel()
                }
            }

            override fun onFinish() {
                if (mInterstitialAd != null) {
                    interstitialInternalCallback.onSuccess(interstitialAd = mInterstitialAd!!)
                }
                else
                    interstitialInternalCallback.onFailed(AdError)
            }
        }.start()
        for (adUnit in primaryIds){
            val adRequest = AdRequest.Builder()
                .addNetworkExtrasBundle(AdMobAdapter::class.java, getConsentEnabledBundle())
                .build()
            InterstitialAd.load(
                application,
                adUnit,
                adRequest,
                object : InterstitialAdLoadCallback() {
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        if (mInterstitialAd == null)
                            mInterstitialAd = null
                        AdError = adError
                        Log.d(TAG, "onAdFailedToLoad: "+adName+" : "+adUnit+" : "+adError.message)
                    }

                    override fun onAdLoaded(interstitialAd: InterstitialAd) {
                        mInterstitialAd = interstitialAd

                        mInterstitialAd?.fullScreenContentCallback =
                            object : FullScreenContentCallback() {
                                override fun onAdImpression() {
                                    super.onAdImpression()
                                    interstitialAdUtilLoadCallback?.onAdImpression()
                                }

                                override fun onAdDismissedFullScreenContent() {
                                    interstitialAdUtilLoadCallback?.onAdDismissedFullScreenContent()
                                }

                                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                    super.onAdFailedToShowFullScreenContent(adError)
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

    internal fun loadInterstitialAdManager(
        application:Context,
        adName: String,
        timer: Long,
        primaryIds: List<String>,
        interstitialAdUtilLoadCallback: InterstitialAdUtilLoadCallback?,
        interstitialInternalCallback: InterstitialInternalCallback
    ){
        var mAdManagerInterstitialAd: AdManagerInterstitialAd? = null
        var loadAdError: LoadAdError? = null
        object : CountDownTimer(timer.toLong(), 500) {
            override fun onTick(millisUntilFinished: Long) {
                if (mAdManagerInterstitialAd != null) {
                    interstitialInternalCallback.onSuccess(adManagerInterstitialAd = mAdManagerInterstitialAd!!)
                    this.cancel()
                }
            }

            override fun onFinish() {
                if (mAdManagerInterstitialAd != null) {
                    interstitialInternalCallback.onSuccess(adManagerInterstitialAd = mAdManagerInterstitialAd!!)
                }
                else
                    interstitialInternalCallback.onFailed(loadAdError)
            }
        }.start()
        for (adUnit in primaryIds){
            var adRequest = AdManagerAdRequest.Builder()
                            .build()

            AdManagerInterstitialAd.load(application.applicationContext,adUnit, adRequest, object : AdManagerInterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    if (mAdManagerInterstitialAd == null)
                        mAdManagerInterstitialAd = null
                    loadAdError = adError
                    Log.d(TAG, "onAdFailedToLoad: "+adName+" : "+adUnit+" : "+adError.message)
                }

                override fun onAdLoaded(interstitialAd: AdManagerInterstitialAd) {
                    if (mAdManagerInterstitialAd == null){
                        mAdManagerInterstitialAd = interstitialAd
                        mAdManagerInterstitialAd?.fullScreenContentCallback =
                            object : FullScreenContentCallback() {
                                override fun onAdImpression() {
                                    super.onAdImpression()
                                    interstitialAdUtilLoadCallback?.onAdImpression()
                                }

                                override fun onAdShowedFullScreenContent() {
                                    mAdManagerInterstitialAd = null
                                    interstitialAdUtilLoadCallback?.onAdShowedFullScreenContent()
                                }

                                override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                                    interstitialAdUtilLoadCallback?.onAdFailedToShowFullScreenContent(
                                        p0,
                                    )
                                }

                                override fun onAdDismissedFullScreenContent() {
                                    interstitialAdUtilLoadCallback?.onAdDismissedFullScreenContent()
                                }
                            }
                    }
                }
            })
        }
    }

    fun loadSplashAd(
        adUnit: String,
        adName: String,
        application: Activity,
        callback: SplashInterstitialCallback,
        timer: Long = 5000L,
        isAdmanager:Boolean = false
    ) {
        if (isInitialized){
            if (application != null){
                if (adUnit != "STOP" && AppPref.getBoolean(application,AppPref.showAppAds) && application.fetchAdStatusFromAdId(adName)) {

                    var fetchedTimer:Int = application.fetchAdLoadTimeout(adName)
                    if (fetchedTimer == 0){
                        fetchedTimer = timer.toInt()
                    }
                    var primaryIds = application.fetchPrimaryById(adName)
                    var secondaryIds = application.fetchSecondaryById(adName)


                    if (!isAdmanager){
                        Log.d("interstitial","OnStart:" + System.currentTimeMillis()/1000)
                        if (primaryIds.size > 0){
                            loadSplashAd(
                                adName,
                                application,
                                callback,
                                fetchedTimer,
                                primaryIds,
                                object :InterstitialInternalCallback{
                                    override fun onSuccess(interstitialAd: InterstitialAd?,ad: AdManagerInterstitialAd?) {
                                        Log.d("interstitial", "onSuccess: Primary Shown" + System.currentTimeMillis()/1000)

                                    }

                                    override fun onFailed(loadAdError: LoadAdError?) {
                                        if (secondaryIds.size > 0)
                                            loadSplashAd(
                                                adName,
                                                application,
                                                callback,
                                                fetchedTimer,
                                                secondaryIds,
                                                object :InterstitialInternalCallback{
                                                    override fun onSuccess(interstitialAd: InterstitialAd?,ad: AdManagerInterstitialAd?) {
                                                        Log.d("interstitial", "onSuccess: First Secondary Shown" + System.currentTimeMillis()/1000)
                                                    }

                                                    override fun onFailed(loadAdError: LoadAdError?) {
                                                        loadSplashAd(
                                                            adName,
                                                            application,
                                                            callback,
                                                            fetchedTimer,
                                                            listOf(adUnit),
                                                            object :InterstitialInternalCallback{
                                                                override fun onSuccess(interstitialAd: InterstitialAd?,ad: AdManagerInterstitialAd?) {
                                                                    Log.d("interstitial", "onSuccess: First Fallback Shown" + System.currentTimeMillis()/1000)
                                                                }

                                                                override fun onFailed(loadAdError: LoadAdError?) {
                                                                    callback.moveNext()
                                                                }
                                                            }
                                                        )
                                                    }
                                                }
                                            )
                                        else
                                            loadSplashAd(
                                                adName,
                                                application,
                                                callback,
                                                fetchedTimer,
                                                listOf(adUnit),
                                                object :InterstitialInternalCallback{
                                                    override fun onSuccess(interstitialAd: InterstitialAd?,ad: AdManagerInterstitialAd?) {
                                                        Log.d("interstitial", "onSuccess: First else Fallback Shown" + System.currentTimeMillis()/1000)
                                                    }

                                                    override fun onFailed(loadAdError: LoadAdError?) {
                                                        callback.moveNext()
                                                    }
                                                }
                                            )
                                    }
                                }
                            )
                        }
                        else if (secondaryIds.size > 0) {
                            loadSplashAd(
                                adName,
                                application,
                                callback,
                                fetchedTimer,
                                secondaryIds,
                                object : InterstitialInternalCallback {
                                    override fun onSuccess(interstitialAd: InterstitialAd?,ad: AdManagerInterstitialAd?) {
                                        Log.d("interstitial", "onSuccess: Second Secondary Shown" + System.currentTimeMillis() / 1000)
                                    }

                                    override fun onFailed(loadAdError: LoadAdError?) {
                                        loadSplashAd(
                                            adName,
                                            application,
                                            callback,
                                            fetchedTimer,
                                            listOf(adUnit),
                                            object : InterstitialInternalCallback {
                                                override fun onSuccess(interstitialAd: InterstitialAd?,ad: AdManagerInterstitialAd?) {
                                                    Log.d("interstitial", "onSuccess: Second Fallback Shown" + System.currentTimeMillis() / 1000)
                                                }

                                                override fun onFailed(loadAdError: LoadAdError?) {
                                                    callback.moveNext()
                                                }
                                            }
                                        )
                                    }
                                }
                            )
                        }
                        else {
                            loadSplashAd(
                                adName,
                                application,
                                callback,
                                fetchedTimer,
                                listOf(adUnit),
                                object : InterstitialInternalCallback {
                                    override fun onSuccess(interstitialAd: InterstitialAd?,ad: AdManagerInterstitialAd?) {
                                        Log.d("interstitial", "onSuccess: Else Fallback Shown" + System.currentTimeMillis() / 1000)
                                    }

                                    override fun onFailed(loadAdError: LoadAdError?) {
                                        callback.moveNext()
                                    }
                                }
                            )
                        }
                    }
                    else{
                        Log.d("interstitial-admanager","OnStart:" + System.currentTimeMillis()/1000)
                        if (primaryIds.size > 0){
                            loadSplashAdManager(
                                adName,
                                application,
                                callback,
                                fetchedTimer,
                                primaryIds,
                                object :InterstitialInternalCallback{
                                    override fun onSuccess(interstitialAd: InterstitialAd?,ad: AdManagerInterstitialAd?) {
                                        Log.d("interstitial-admanager", "onSuccess: Primary Shown" + System.currentTimeMillis()/1000)

                                    }

                                    override fun onFailed(loadAdError: LoadAdError?) {
                                        if (secondaryIds.size > 0)
                                            loadSplashAdManager(
                                                adName,
                                                application,
                                                callback,
                                                fetchedTimer,
                                                secondaryIds,
                                                object :InterstitialInternalCallback{
                                                    override fun onSuccess(interstitialAd: InterstitialAd?,ad: AdManagerInterstitialAd?) {
                                                        Log.d("interstitial-admanager", "onSuccess: First Secondary Shown" + System.currentTimeMillis()/1000)
                                                    }

                                                    override fun onFailed(loadAdError: LoadAdError?) {
                                                        loadSplashAdManager(
                                                            adName,
                                                            application,
                                                            callback,
                                                            fetchedTimer,
                                                            listOf(adUnit),
                                                            object :InterstitialInternalCallback{
                                                                override fun onSuccess(interstitialAd: InterstitialAd?,ad: AdManagerInterstitialAd?) {
                                                                    Log.d("interstitial-admanager", "onSuccess: First Fallback Shown" + System.currentTimeMillis()/1000)
                                                                }

                                                                override fun onFailed(loadAdError: LoadAdError?) {
                                                                    callback.moveNext()
                                                                }
                                                            }
                                                        )
                                                    }
                                                }
                                            )
                                        else
                                            loadSplashAdManager(
                                                adName,
                                                application,
                                                callback,
                                                fetchedTimer,
                                                listOf(adUnit),
                                                object :InterstitialInternalCallback{
                                                    override fun onSuccess(interstitialAd: InterstitialAd?,ad: AdManagerInterstitialAd?) {
                                                        Log.d("interstitial-admanager", "onSuccess: First else Fallback Shown" + System.currentTimeMillis()/1000)
                                                    }

                                                    override fun onFailed(loadAdError: LoadAdError?) {
                                                        callback.moveNext()
                                                    }
                                                }
                                            )
                                    }
                                }
                            )
                        }
                        else if (secondaryIds.size > 0) {
                            loadSplashAdManager(
                                adName,
                                application,
                                callback,
                                fetchedTimer,
                                secondaryIds,
                                object : InterstitialInternalCallback {
                                    override fun onSuccess(interstitialAd: InterstitialAd?,ad: AdManagerInterstitialAd?) {
                                        Log.d("interstitial-admanager", "onSuccess: Second Secondary Shown" + System.currentTimeMillis() / 1000)
                                    }

                                    override fun onFailed(loadAdError: LoadAdError?) {
                                        loadSplashAdManager(
                                            adName,
                                            application,
                                            callback,
                                            fetchedTimer,
                                            listOf(adUnit),
                                            object : InterstitialInternalCallback {
                                                override fun onSuccess(interstitialAd: InterstitialAd?,ad: AdManagerInterstitialAd?) {
                                                    Log.d("interstitial-admanager", "onSuccess: Second Fallback Shown" + System.currentTimeMillis() / 1000)
                                                }

                                                override fun onFailed(loadAdError: LoadAdError?) {
                                                    callback.moveNext()
                                                }
                                            }
                                        )
                                    }
                                }
                            )
                        }
                        else {
                            loadSplashAdManager(
                                adName,
                                application,
                                callback,
                                fetchedTimer,
                                listOf(adUnit),
                                object : InterstitialInternalCallback {
                                    override fun onSuccess(interstitialAd: InterstitialAd?,ad: AdManagerInterstitialAd?) {
                                        Log.d("interstitial-admanager", "onSuccess: Else Fallback Shown" + System.currentTimeMillis() / 1000)
                                    }

                                    override fun onFailed(loadAdError: LoadAdError?) {
                                        callback.moveNext()
                                    }
                                }
                            )
                        }
                    }

                } else {
                    callback.moveNext()
                }
            }
            else {
                callback?.onContextFailed()
            }

        }
        else {
            callback.OnError("Ads SDK not (initialized / properly initial). Try initializing again.")
        }
    }

    internal fun loadSplashAd(
        adName: String,
        activity: Activity,
        callback: SplashInterstitialCallback,
        fetchedTimer: Int,
        primaryIds: List<String>,
        interstitialInternalCallback: InterstitialInternalCallback) {

        var splash: InterstitialAd? = null
        var AdError: LoadAdError? = null
        object : CountDownTimer(fetchedTimer.toLong(), 500) {
            override fun onTick(millisUntilFinished: Long) {
                if (splash != null) {
                    splash?.show(activity)
                    interstitialInternalCallback.onSuccess(splash!!)
                    this.cancel()
                }
            }

            override fun onFinish() {
                if (splash != null) {
                    splash?.show(activity)
                    interstitialInternalCallback.onSuccess(splash!!)
                }
                else
                    interstitialInternalCallback.onFailed(AdError)
            }
        }.start()
        for (adUnit in primaryIds){
            val adRequest = AdRequest.Builder()
                .addNetworkExtrasBundle(AdMobAdapter::class.java, getConsentEnabledBundle())
                .build()
            InterstitialAd.load(
                activity,
                adUnit,
                adRequest, object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(splashInters: InterstitialAd) {
                        super.onAdLoaded(splashInters)
                        if (splash == null){
                            splash = splashInters
                            splash?.fullScreenContentCallback =
                                object : FullScreenContentCallback() {
                                    override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                                        callback.moveNext()
                                    }

                                    override fun onAdDismissedFullScreenContent() {
                                        callback.moveNext()
                                    }
                                }
                        }
                    }

                    override fun onAdFailedToLoad(p0: LoadAdError) {
                        super.onAdFailedToLoad(p0)
                        AdError = p0
                        Log.d(TAG, "onAdFailedToLoad: "+adName+" : "+adUnit+" : "+p0.message)
                    }
                }
            )
        }

    }

    internal fun loadSplashAdManager(
        adName: String,
        activity: Activity,
        callback: SplashInterstitialCallback,
        fetchedTimer: Int,
        primaryIds: List<String>,
        interstitialInternalCallback: InterstitialInternalCallback
    ) {

        var mAdManagerInterstitialAd: AdManagerInterstitialAd? = null
        var loadAdError: LoadAdError? = null
        object : CountDownTimer(fetchedTimer.toLong(), 500) {
            override fun onTick(millisUntilFinished: Long) {
                if (mAdManagerInterstitialAd != null) {
                    mAdManagerInterstitialAd?.show(activity)
                    interstitialInternalCallback.onSuccess(mAdManagerInterstitialAd!!)
                    this.cancel()
                }
            }

            override fun onFinish() {
                if (mAdManagerInterstitialAd != null) {
                    mAdManagerInterstitialAd?.show(activity)
                    interstitialInternalCallback.onSuccess(mAdManagerInterstitialAd!!)
                }
                else
                    interstitialInternalCallback.onFailed(loadAdError)
            }
        }.start()
        for (adUnit in primaryIds){

            var adRequest = AdManagerAdRequest.Builder().build()

            AdManagerInterstitialAd.load(activity.applicationContext,adUnit, adRequest, object : AdManagerInterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    if (mAdManagerInterstitialAd == null)
                        mAdManagerInterstitialAd = null
                    loadAdError = adError
                    Log.d(TAG, "onAdFailedToLoad: "+adName+" : "+adUnit+" : "+adError.message)
                }

                override fun onAdLoaded(interstitialAd: AdManagerInterstitialAd) {
                    if (mAdManagerInterstitialAd == null){
                        mAdManagerInterstitialAd = interstitialAd
                        mAdManagerInterstitialAd?.fullScreenContentCallback =
                            object : FullScreenContentCallback() {
                                override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                                    callback.moveNext()
                                }

                                override fun onAdDismissedFullScreenContent() {
                                    callback.moveNext()
                                }
                            }
                    }
                }
            })
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
        application: Activity,
        adUnit: String,
        adName: String,
        rewardedAdUtilLoadCallback: RewardedAdUtilLoadCallback?,
        showLoaderScreen: Boolean = false,
        isAdmanager:Boolean = false,
        loadTimeOut:Int
    ) {
        if (isInitialized){
            if (application != null){
                if (adUnit != "STOP" && AppPref.getBoolean(application,AppPref.showAppAds) && application.fetchAdStatusFromAdId(adName))
                {
                    var fetchedTimer:Int = application.fetchAdLoadTimeout(adName)
                    if (fetchedTimer == 0){
                        fetchedTimer = loadTimeOut
                    }
                    var primaryIds = application.fetchPrimaryById(adName)
                    var secondaryIds = application.fetchSecondaryById(adName)
                    if (showLoaderScreen)
                        showAdLoaderLayout(application)
                    Log.d("rewarded","OnStart:" + System.currentTimeMillis()/1000)
                    if (!isAdmanager){
                        if (primaryIds.size > 0){
                            loadRewardedAd(
                                adName,
                                application,
                                rewardedAdUtilLoadCallback,
                                fetchedTimer.toLong(),
                                primaryIds,
                                object :RewardInternalCallback{
                                    override fun onSuccess(rewardAds: RewardedAd) {
                                        Log.d("rewarded", "onSuccess: Primary Shown" + System.currentTimeMillis()/1000)
                                        if (showLoaderScreen)
                                            dismissAdLoaderLayout(application)
                                        rewardedAdUtilLoadCallback?.onAdLoaded(rewardAds)
                                    }

                                    override fun onFailed(adError: LoadAdError?, ad: RewardedAd?) {
                                        if (secondaryIds.size > 0)
                                            loadRewardedAd(
                                                adName,
                                                application,
                                                rewardedAdUtilLoadCallback,
                                                fetchedTimer.toLong(),
                                                secondaryIds,
                                                object :RewardInternalCallback{
                                                    override fun onSuccess(rewardAds: RewardedAd) {
                                                        Log.d("rewarded", "onSuccess: First Secondary Shown" + System.currentTimeMillis()/1000)
                                                        if (showLoaderScreen)
                                                            dismissAdLoaderLayout(application)
                                                        rewardedAdUtilLoadCallback?.onAdLoaded(rewardAds)
                                                    }

                                                    override fun onFailed(
                                                        adError: LoadAdError?,
                                                        ad: RewardedAd?
                                                    ) {
                                                        loadRewardedAd(
                                                            adName,
                                                            application,
                                                            rewardedAdUtilLoadCallback,
                                                            fetchedTimer.toLong(),
                                                            listOf(adUnit),
                                                            object :RewardInternalCallback{
                                                                override fun onSuccess(rewardAds: RewardedAd) {
                                                                    Log.d("rewarded", "onSuccess: First Fallback Shown" + System.currentTimeMillis()/1000)
                                                                    if (showLoaderScreen)
                                                                        dismissAdLoaderLayout(application)
                                                                    rewardedAdUtilLoadCallback?.onAdLoaded(rewardAds)
                                                                }

                                                                override fun onFailed(
                                                                    adError: LoadAdError?,
                                                                    ad: RewardedAd?
                                                                ) {
                                                                    dismissAdLoaderLayout(application)
                                                                    rewardedAdUtilLoadCallback?.onAdFailedToLoad(adError,ad)
                                                                }
                                                            }
                                                        )
                                                    }
                                                }
                                            )
                                        else
                                            loadRewardedAd(
                                                adName,
                                                application,
                                                rewardedAdUtilLoadCallback,
                                                fetchedTimer.toLong(),
                                                listOf(adUnit),
                                                object :RewardInternalCallback{
                                                    override fun onSuccess(rewardAds: RewardedAd) {
                                                        Log.d("rewarded", "onSuccess: First else Fallback Shown" + System.currentTimeMillis()/1000)
                                                        if (showLoaderScreen)
                                                            dismissAdLoaderLayout(application)
                                                        rewardedAdUtilLoadCallback?.onAdLoaded(rewardAds)
                                                    }

                                                    override fun onFailed(
                                                        adError: LoadAdError?,
                                                        ad: RewardedAd?
                                                    ) {
                                                        dismissAdLoaderLayout(application)
                                                        rewardedAdUtilLoadCallback?.onAdFailedToLoad(adError,ad)
                                                    }
                                                }
                                            )
                                    }
                                }
                            )
                        }
                        else if (secondaryIds.size > 0) {
                            loadRewardedAd(
                                adName,
                                application,
                                rewardedAdUtilLoadCallback,
                                fetchedTimer.toLong(),
                                secondaryIds,
                                object : RewardInternalCallback {
                                    override fun onSuccess(rewardAds: RewardedAd) {
                                        Log.d(
                                            "rewarded",
                                            "onSuccess: Second Secondary Shown" + System.currentTimeMillis() / 1000
                                        )
                                        if (showLoaderScreen)
                                            dismissAdLoaderLayout(application)
                                        rewardedAdUtilLoadCallback?.onAdLoaded(rewardAds)
                                    }

                                    override fun onFailed(
                                        adError: LoadAdError?,
                                        ad: RewardedAd?
                                    ) {
                                        loadRewardedAd(
                                            adName,
                                            application,
                                            rewardedAdUtilLoadCallback,
                                            fetchedTimer.toLong(),
                                            listOf(adUnit),
                                            object : RewardInternalCallback {
                                                override fun onSuccess(rewardAds: RewardedAd) {
                                                    Log.d(
                                                        "rewarded",
                                                        "onSuccess: Second Fallback Shown" + System.currentTimeMillis() / 1000
                                                    )
                                                    if (showLoaderScreen)
                                                        dismissAdLoaderLayout(application)
                                                    rewardedAdUtilLoadCallback?.onAdLoaded(rewardAds)
                                                }

                                                override fun onFailed(
                                                    adError: LoadAdError?,
                                                    ad: RewardedAd?
                                                ) {
                                                    dismissAdLoaderLayout(application)
                                                    rewardedAdUtilLoadCallback?.onAdFailedToLoad(adError, ad)
                                                }
                                            }
                                        )
                                    }
                                }
                            )
                        }
                        else {
                            loadRewardedAd(
                                adName,
                                application,
                                rewardedAdUtilLoadCallback,
                                fetchedTimer.toLong(),
                                listOf(adUnit),
                                object : RewardInternalCallback {
                                    override fun onSuccess(rewardAds: RewardedAd) {
                                        Log.d(
                                            "rewarded",
                                            "onSuccess: else Fallback Shown" + System.currentTimeMillis() / 1000
                                        )
                                        if (showLoaderScreen)
                                            dismissAdLoaderLayout(application)
                                        rewardedAdUtilLoadCallback?.onAdLoaded(rewardAds)
                                    }

                                    override fun onFailed(
                                        adError: LoadAdError?,
                                        ad: RewardedAd?
                                    ) {
                                        dismissAdLoaderLayout(application)
                                        rewardedAdUtilLoadCallback?.onAdFailedToLoad(adError, ad)
                                    }
                                }
                            )
                        }
                    }
                    else {
                        if (primaryIds.size > 0){
                            loadRewardedAdManager(
                                adName,
                                application,
                                rewardedAdUtilLoadCallback,
                                fetchedTimer.toLong(),
                                primaryIds,
                                object :RewardInternalCallback{
                                    override fun onSuccess(rewardAds: RewardedAd) {
                                        Log.d("rewarded", "onSuccess: Primary Shown" + System.currentTimeMillis()/1000)
                                        if (showLoaderScreen)
                                            dismissAdLoaderLayout(application)
                                        rewardedAdUtilLoadCallback?.onAdLoaded(rewardAds)
                                    }

                                    override fun onFailed(adError: LoadAdError?, ad: RewardedAd?) {
                                        if (secondaryIds.size > 0)
                                            loadRewardedAdManager(
                                                adName,
                                                application,
                                                rewardedAdUtilLoadCallback,
                                                fetchedTimer.toLong(),
                                                secondaryIds,
                                                object :RewardInternalCallback{
                                                    override fun onSuccess(rewardAds: RewardedAd) {
                                                        Log.d("rewarded", "onSuccess: First Secondary Shown" + System.currentTimeMillis()/1000)
                                                        if (showLoaderScreen)
                                                            dismissAdLoaderLayout(application)
                                                        rewardedAdUtilLoadCallback?.onAdLoaded(rewardAds)
                                                    }

                                                    override fun onFailed(
                                                        adError: LoadAdError?,
                                                        ad: RewardedAd?
                                                    ) {
                                                        loadRewardedAdManager(
                                                            adName,
                                                            application,
                                                            rewardedAdUtilLoadCallback,
                                                            fetchedTimer.toLong(),
                                                            listOf(adUnit),
                                                            object :RewardInternalCallback{
                                                                override fun onSuccess(rewardAds: RewardedAd) {
                                                                    Log.d("rewarded", "onSuccess: First Fallback Shown" + System.currentTimeMillis()/1000)
                                                                    if (showLoaderScreen)
                                                                        dismissAdLoaderLayout(application)
                                                                    rewardedAdUtilLoadCallback?.onAdLoaded(rewardAds)
                                                                }

                                                                override fun onFailed(
                                                                    adError: LoadAdError?,
                                                                    ad: RewardedAd?
                                                                ) {
                                                                    dismissAdLoaderLayout(application)
                                                                    rewardedAdUtilLoadCallback?.onAdFailedToLoad(adError,ad)
                                                                }
                                                            }
                                                        )
                                                    }
                                                }
                                            )
                                        else
                                            loadRewardedAdManager(
                                                adName,
                                                application,
                                                rewardedAdUtilLoadCallback,
                                                fetchedTimer.toLong(),
                                                listOf(adUnit),
                                                object :RewardInternalCallback{
                                                    override fun onSuccess(rewardAds: RewardedAd) {
                                                        Log.d("rewarded", "onSuccess: First else Fallback Shown" + System.currentTimeMillis()/1000)
                                                        if (showLoaderScreen)
                                                            dismissAdLoaderLayout(application)
                                                        rewardedAdUtilLoadCallback?.onAdLoaded(rewardAds)
                                                    }

                                                    override fun onFailed(
                                                        adError: LoadAdError?,
                                                        ad: RewardedAd?
                                                    ) {
                                                        dismissAdLoaderLayout(application)
                                                        rewardedAdUtilLoadCallback?.onAdFailedToLoad(adError,ad)
                                                    }
                                                }
                                            )
                                    }
                                }
                            )
                        }
                        else if (secondaryIds.size > 0) {
                            loadRewardedAdManager(
                                adName,
                                application,
                                rewardedAdUtilLoadCallback,
                                fetchedTimer.toLong(),
                                secondaryIds,
                                object : RewardInternalCallback {
                                    override fun onSuccess(rewardAds: RewardedAd) {
                                        Log.d(
                                            "rewarded",
                                            "onSuccess: Second Secondary Shown" + System.currentTimeMillis() / 1000
                                        )
                                        if (showLoaderScreen)
                                            dismissAdLoaderLayout(application)
                                        rewardedAdUtilLoadCallback?.onAdLoaded(rewardAds)
                                    }

                                    override fun onFailed(
                                        adError: LoadAdError?,
                                        ad: RewardedAd?
                                    ) {
                                        loadRewardedAdManager(
                                            adName,
                                            application,
                                            rewardedAdUtilLoadCallback,
                                            fetchedTimer.toLong(),
                                            listOf(adUnit),
                                            object : RewardInternalCallback {
                                                override fun onSuccess(rewardAds: RewardedAd) {
                                                    Log.d(
                                                        "rewarded",
                                                        "onSuccess: Second Fallback Shown" + System.currentTimeMillis() / 1000
                                                    )
                                                    if (showLoaderScreen)
                                                        dismissAdLoaderLayout(application)
                                                    rewardedAdUtilLoadCallback?.onAdLoaded(rewardAds)
                                                }

                                                override fun onFailed(
                                                    adError: LoadAdError?,
                                                    ad: RewardedAd?
                                                ) {
                                                    dismissAdLoaderLayout(application)
                                                    rewardedAdUtilLoadCallback?.onAdFailedToLoad(adError, ad)
                                                }
                                            }
                                        )
                                    }
                                }
                            )
                        }
                        else {
                            loadRewardedAdManager(
                                adName,
                                application,
                                rewardedAdUtilLoadCallback,
                                fetchedTimer.toLong(),
                                listOf(adUnit),
                                object : RewardInternalCallback {
                                    override fun onSuccess(rewardAds: RewardedAd) {
                                        Log.d(
                                            "rewarded",
                                            "onSuccess: else Fallback Shown" + System.currentTimeMillis() / 1000
                                        )
                                        if (showLoaderScreen)
                                            dismissAdLoaderLayout(application)
                                        rewardedAdUtilLoadCallback?.onAdLoaded(rewardAds)
                                    }

                                    override fun onFailed(
                                        adError: LoadAdError?,
                                        ad: RewardedAd?
                                    ) {
                                        dismissAdLoaderLayout(application)
                                        rewardedAdUtilLoadCallback?.onAdFailedToLoad(adError, ad)
                                    }
                                }
                            )
                        }
                    }

                }
            }
            else {
                rewardedAdUtilLoadCallback?.onContextFailed()
            }

        }
        else {
            rewardedAdUtilLoadCallback?.onAdFailedToLoad(LoadAdError(404, "Ads SDK not (initialized / properly initial). Try initializing again.", "",null,null),null)
        }

    }

    internal fun loadRewardedAd(
        adName: String,
        activity: Context,
        rewardedAdUtilLoadCallback: RewardedAdUtilLoadCallback?,
        timer: Long,
        primaryIds: List<String>,
        rewardInternalCallback: RewardInternalCallback
    ){
        var mRewardedAd: RewardedAd? = null
        var loadAdError: LoadAdError? = null
        object : CountDownTimer(timer.toLong(), 500) {
            override fun onTick(millisUntilFinished: Long) {
                if (mRewardedAd != null) {
                    rewardInternalCallback.onSuccess(mRewardedAd!!)
                    this.cancel()
                }
            }

            override fun onFinish() {
                if (mRewardedAd != null) {
                    rewardInternalCallback.onSuccess(mRewardedAd!!)
                }
                else
                    rewardInternalCallback.onFailed(loadAdError,mRewardedAd)
            }
        }.start()
        for (adUnit in primaryIds){
            val adRequest = AdRequest.Builder()
                .addNetworkExtrasBundle(AdMobAdapter::class.java, getConsentEnabledBundle())
                .build()
            RewardedAd.load(
                activity,
                adUnit,
                adRequest,
                object : RewardedAdLoadCallback() {
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        if (mRewardedAd == null)
                            mRewardedAd = null
                        loadAdError = adError
                        Log.d(TAG, "onAdFailedToLoad: "+adName+" : "+adUnit+" : "+adError.message)
                    }

                    override fun onAdLoaded(rewardedAd: RewardedAd) {
                        mRewardedAd = rewardedAd

                        mRewardedAd?.fullScreenContentCallback =
                            object : FullScreenContentCallback() {
                                override fun onAdDismissedFullScreenContent() {
                                    rewardedAdUtilLoadCallback?.onAdDismissedFullScreenContent()
                                }

                                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
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

    internal fun loadRewardedAdManager(
        adName: String,
        activity: Context,
        rewardedAdUtilLoadCallback: RewardedAdUtilLoadCallback?,
        timer: Long,
        primaryIds: List<String>,
        rewardInternalCallback: RewardInternalCallback
    ){
        var mRewardedAd: RewardedAd? = null
        var loadAdError: LoadAdError? = null
        object : CountDownTimer(timer.toLong(), 500) {
            override fun onTick(millisUntilFinished: Long) {
                if (mRewardedAd != null) {
                    rewardInternalCallback.onSuccess(mRewardedAd!!)
                    this.cancel()
                }
            }

            override fun onFinish() {
                if (mRewardedAd != null) {
                    rewardInternalCallback.onSuccess(mRewardedAd!!)
                }
                else
                    rewardInternalCallback.onFailed(loadAdError,mRewardedAd)
            }
        }.start()
        for (adUnit in primaryIds){
            var adRequest = AdManagerAdRequest.Builder().build()
            RewardedAd.load(
                activity,
                adUnit,
                adRequest,
                object : RewardedAdLoadCallback() {
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        if (mRewardedAd == null)
                            mRewardedAd = null
                        loadAdError = adError
                        Log.d(TAG, "onAdFailedToLoad: "+adName+" : "+adUnit+" : "+adError.message)

                    }

                    override fun onAdLoaded(rewardedAd: RewardedAd) {
                        mRewardedAd = rewardedAd

                        mRewardedAd?.fullScreenContentCallback =
                            object : FullScreenContentCallback() {
                                override fun onAdDismissedFullScreenContent() {
                                    rewardedAdUtilLoadCallback?.onAdDismissedFullScreenContent()
                                }

                                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
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
            val DEFAULT_AD = "6"
            val SMALL = "3"
            val MEDIUM = "4"
            val BIGV1 = "1"
            val BIGV2 = "5"
            val BIGV3 = "2"
            val GRID_AD = "7"
        }
    }

    fun loadNativeAd(
        application:Context,
        lifecycle: Lifecycle,
        adUnit: String,
        adName: String,
        viewGroup: ViewGroup,
        callback: NativeAdLoadCallback?,
        adType: String = ADType.DEFAULT_AD,
        background: Any?,
        textColor1: Int?,
        textColor2: Int?,
        mediaMaxHeight: Int = 300,
        loadingTextSize: Int = 48,
        contentURL: String? = null,
        neighbourContentURL: List<String>? = null,
        showLoadingMessage: Boolean = true,
        isAdmanager:Boolean = false,
        loadTimeOut:Int
    ) {
        if (isInitialized){
            if (application != null){
                if (AppPref.getBoolean(application,AppPref.showAppAds) && application.fetchAdStatusFromAdId(adName)) {
                    var mediaMaxHeight1 = mediaMaxHeight
                    var newAdSize = application.fetchAdSize(adName,adType)
                    @LayoutRes val layoutId = when (newAdSize) {
                        "6" -> {
                            mediaMaxHeight1 = 200
                            R.layout.native_admob_ad_t6
                        }/*DEFAULT_AD*/
                        "3" -> R.layout.native_admob_ad_t3/*SMALL*/
                        "4" -> R.layout.native_admob_ad_t4/*MEDIUM*/
                        "1" -> R.layout.native_admob_ad_t1/*BIGV1*/
                        "5" -> R.layout.native_admob_ad_t5/*BIGV2*/
                        "2" -> R.layout.native_admob_ad_t2/*BIGV3*/
                        "7" -> R.layout.native_admob_ad_t7/*GRID_AD*/
                        else -> R.layout.native_admob_ad_t1
                    }

                    loadNativeAd(
                        application,
                        lifecycle,
                        adUnit,
                        adName,
                        viewGroup,
                        callback,
                        layoutId,
                        null,
                        newAdSize,
                        background = background,
                        textColor1,
                        textColor2,
                        mediaMaxHeight1,
                        loadingTextSize,
                        contentURL,
                        neighbourContentURL,
                        showLoadingMessage = showLoadingMessage,
                        isAdmanager = isAdmanager,
                        loadTimeOut = loadTimeOut
                    )
                }
                else {
                    viewGroup.visibility = GONE
                }
            }
            else {
                callback?.onContextFailed()
                viewGroup.visibility = GONE
            }

        }
        else {
            viewGroup.visibility = GONE
            callback?.onAdFailed(LoadAdError(404, "Ads SDK not (initialized / properly initial). Try initializing again.", "",null,null))
        }
    }

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
    internal fun loadNativeAd(
        application: Context,
        lifecycle: Lifecycle,
        adUnit: String,
        adName: String,
        viewGroup: ViewGroup,
        nativeAdLoadCallback: NativeAdLoadCallback?,
        @LayoutRes layoutId: Int = R.layout.native_admob_ad_t1,
        populator: ((nativeAd: NativeAd, adView: NativeAdView) -> Unit)? = null,
        adType: String = "1",
        background: Any?,
        textColor1: Int?,
        textColor2: Int?,
        mediaMaxHeight: Int = 300,
        loadingTextSize: Int,
        contentURL: String? = null,
        neighbourContentURL: List<String>? = null,
        showLoadingMessage: Boolean,
        isAdmanager:Boolean = false,
        loadTimeOut:Int
    ) {
        loadNativeAd(
            application,
            System.currentTimeMillis(),
            lifecycle,
            adUnit,
            adName,
            viewGroup,
            nativeAdLoadCallback,
            layoutId,
            populator,
            adType,
            background = background,
            textColor1,
            textColor2,
            mediaMaxHeight,
            loadingTextSize,
            contentURL,
            neighbourContentURL,
            showLoadingMessage,
            isAdmanager = isAdmanager,
            loadTimeOut
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
    internal fun loadNativeAd(
        application: Context,
        id: Long = System.currentTimeMillis(),
        lifecycle: Lifecycle,
        adUnit: String,
        adName: String,
        viewGroup: ViewGroup,
        nativeAdLoadCallback: NativeAdLoadCallback?,
        @LayoutRes layoutId: Int = R.layout.native_admob_ad_t1,
        populator: ((nativeAd: NativeAd, adView: NativeAdView) -> Unit)? = null,
        adType: String = "1",
        background: Any?,
        textColor1: Int?,
        textColor2: Int?,
        mediaMaxHeight: Int = 300,
        loadingTextSize: Int,
        contentURL: String? = null,
        neighbourContentURL: List<String>? = null,
        showLoadingMessage: Boolean,
        isAdmanager:Boolean = false,
        loadTimeOut:Int
    ) {
        if (adUnit != "STOP" && AppPref.getBoolean(application,AppPref.showAppAds) && application.fetchAdStatusFromAdId(adName)) {
            viewGroup.visibility = VISIBLE
            if (application != null) {
                if (adUnit.isBlank()) return

                var fetchedTimer:Int = application.fetchAdLoadTimeout(adName)
                if (fetchedTimer == 0){
                    fetchedTimer = loadTimeOut
                }
                var primaryIds = application.fetchPrimaryById(adName)
                var secondaryIds = application.fetchSecondaryById(adName)

                val inflate = View.inflate(application, R.layout.shimmer, null)
                when (adType) {
                    "6" -> {
                        inflate.findViewById<LinearLayout>(R.id.shim_default).visibility = View.VISIBLE
                    }/*DEFAULT_AD*/
                    "3" -> {
                        inflate.findViewById<LinearLayout>(R.id.shim_small).visibility = View.VISIBLE
                    }
                    /*SMALL*/
                    "4" -> {
                        inflate.findViewById<LinearLayout>(R.id.shim_medium).visibility = View.VISIBLE
                    }
                    /*MEDIUM*/
                    "1" -> {
                        inflate.findViewById<LinearLayout>(R.id.shim_bigv1).visibility = View.VISIBLE
                    }
                    /*BIGV1*/
                    "5" -> {
                        inflate.findViewById<LinearLayout>(R.id.shim_bigv2).visibility = View.VISIBLE
                    }
                    /*BIGV2*/
                    "2" -> {
                        inflate.findViewById<LinearLayout>(R.id.shim_bigv3).visibility = View.VISIBLE
                    }
                    /*BIGV3*/
                    "7" -> {
                        inflate.findViewById<LinearLayout>(R.id.shim_gridad).visibility = View.VISIBLE
                    }
                    /*GRID_AD*/
                    else -> {
                        inflate.findViewById<LinearLayout>(R.id.shim_default).visibility = View.VISIBLE
                    }

                }

                viewGroup.removeAllViews()
                if (showLoadingMessage) {
                    viewGroup.addView(inflate)
                }

                Log.d("native","OnStart:" + System.currentTimeMillis()/1000)
                if (!isAdmanager){
                    if (primaryIds.size > 0){
                        loadNativeAd(
                            application,
                            id,
                            lifecycle,
                            adName,
                            viewGroup,
                            nativeAdLoadCallback,
                            layoutId,
                            populator,
                            adType,
                            background,
                            textColor1,
                            textColor2,
                            mediaMaxHeight,
                            loadingTextSize,
                            contentURL,
                            neighbourContentURL,
                            showLoadingMessage,
                            primaryIds,
                            fetchedTimer,
                            object: NativeInternalCallback{

                                override fun onSuccess(nativeAd: NativeAd?) {
                                    Log.d("native", "onSuccess: Primary Shown" + System.currentTimeMillis()/1000)
                                    nativeAdLoadCallback?.onAdLoaded()
                                    if (nativeAd != null) {
                                        val adView =
                                            View.inflate(application, layoutId, null)
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
                                            if (layoutId == R.layout.native_admob_ad_t6){
                                                when (background) {
                                                    is String -> {
                                                        adView.findViewById<LinearLayout>(R.id.main_layout).setBackgroundColor(Color.parseColor(background))
                                                    }
                                                    is Drawable -> {
                                                        adView.findViewById<LinearLayout>(R.id.main_layout).background = background
                                                    }
                                                    is Int -> {
                                                        adView.findViewById<LinearLayout>(R.id.main_layout).setBackgroundColor(background)
                                                    }
                                                }
                                            }
                                        }
                                        if (populator != null) {
                                            nativeAd?.let {
                                                populator.invoke(it, adView)
                                            }
                                        } else {
                                            nativeAd?.let {
                                                try {
                                                populateUnifiedNativeAdView(
                                                    it,
                                                    adView,
                                                    adType,
                                                    textColor1,
                                                    textColor2,
                                                    application.fetchColor(adName),
                                                    mediaMaxHeight
                                                )
                                            }
                                                catch (e:Exception){

                                                }
                                            }
                                        }
                                        viewGroup.removeAllViews()
                                        viewGroup.addView(adView)
                                        refreshNative(adName)
                                    }
                                }

                                override fun onFailure(loadAdError: LoadAdError?) {
                                    if (secondaryIds.size >0){
                                        loadNativeAd(
                                            application,
                                            id,
                                            lifecycle,
                                            adName,
                                            viewGroup,
                                            nativeAdLoadCallback,
                                            layoutId,
                                            populator,
                                            adType,
                                            background,
                                            textColor1,
                                            textColor2,
                                            mediaMaxHeight,
                                            loadingTextSize,
                                            contentURL,
                                            neighbourContentURL,
                                            showLoadingMessage,
                                            secondaryIds,
                                            fetchedTimer,
                                            object: NativeInternalCallback{
                                                override fun onSuccess(nativeAd: NativeAd?) {
                                                    Log.d("native", "onSuccess: First Secondary Shown" + System.currentTimeMillis()/1000)
                                                    nativeAdLoadCallback?.onAdLoaded()
                                                    if (nativeAd != null) {
                                                        val adView =
                                                            View.inflate(application, layoutId, null)
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
                                                            if (layoutId == R.layout.native_admob_ad_t6){
                                                                when (background) {
                                                                    is String -> {
                                                                        adView.findViewById<LinearLayout>(R.id.main_layout).setBackgroundColor(Color.parseColor(background))
                                                                    }
                                                                    is Drawable -> {
                                                                        adView.findViewById<LinearLayout>(R.id.main_layout).background = background
                                                                    }
                                                                    is Int -> {
                                                                        adView.findViewById<LinearLayout>(R.id.main_layout).setBackgroundColor(background)
                                                                    }
                                                                }
                                                            }
                                                        }
                                                        if (populator != null) {
                                                            nativeAd?.let {
                                                                populator.invoke(it, adView)
                                                            }
                                                        } else {
                                                            nativeAd?.let {
                                                                try {
                                                                populateUnifiedNativeAdView(
                                                                    it,
                                                                    adView,
                                                                    adType,
                                                                    textColor1,
                                                                    textColor2,
                                                                    application.fetchColor(adName),
                                                                    mediaMaxHeight
                                                                )
                                                            }
                                                                catch (e:Exception){

                                                                }
                                                            }
                                                        }
                                                        viewGroup.removeAllViews()
                                                        viewGroup.addView(adView)
                                                        refreshNative(adName)
                                                    }
                                                }

                                                override fun onFailure(loadAdError: LoadAdError?) {
                                                    loadNativeAd(
                                                        application,
                                                        id,
                                                        lifecycle,
                                                        adName,
                                                        viewGroup,
                                                        nativeAdLoadCallback,
                                                        layoutId,
                                                        populator,
                                                        adType,
                                                        background,
                                                        textColor1,
                                                        textColor2,
                                                        mediaMaxHeight,
                                                        loadingTextSize,
                                                        contentURL,
                                                        neighbourContentURL,
                                                        showLoadingMessage,
                                                        listOf(adUnit),
                                                        fetchedTimer,
                                                        object: NativeInternalCallback{
                                                            override fun onSuccess(nativeAd: NativeAd?) {
                                                                Log.d("native", "onSuccess: First Fallback Shown" + System.currentTimeMillis()/1000)
                                                                nativeAdLoadCallback?.onAdLoaded()
                                                                if (nativeAd != null) {
                                                                    val adView =
                                                                        View.inflate(application, layoutId, null)
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
                                                                        if (layoutId == R.layout.native_admob_ad_t6){
                                                                            when (background) {
                                                                                is String -> {
                                                                                    adView.findViewById<LinearLayout>(R.id.main_layout).setBackgroundColor(Color.parseColor(background))
                                                                                }
                                                                                is Drawable -> {
                                                                                    adView.findViewById<LinearLayout>(R.id.main_layout).background = background
                                                                                }
                                                                                is Int -> {
                                                                                    adView.findViewById<LinearLayout>(R.id.main_layout).setBackgroundColor(background)
                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                                    if (populator != null) {
                                                                        nativeAd?.let {
                                                                            populator.invoke(it, adView)
                                                                        }
                                                                    } else {
                                                                        nativeAd?.let {
                                                                            try {
                                                                            populateUnifiedNativeAdView(
                                                                                it,
                                                                                adView,
                                                                                adType,
                                                                                textColor1,
                                                                                textColor2,
                                                                                application.fetchColor(adName),
                                                                                mediaMaxHeight
                                                                            )
                                                                        }
                                                                            catch (e:Exception){

                                                                            }
                                                                        }
                                                                    }
                                                                    viewGroup.removeAllViews()
                                                                    viewGroup.addView(adView)
                                                                    refreshNative(adName)
                                                                }
                                                            }

                                                            override fun onFailure(loadAdError: LoadAdError?) {
                                                                nativeAdLoadCallback?.onAdFailed(loadAdError)
                                                            }

                                                        },
                                                        isAdmanager = isAdmanager,
                                                    )
                                                }

                                            },
                                            isAdmanager = isAdmanager,
                                        )
                                    }
                                    else{
                                        loadNativeAd(
                                            application,
                                            id,
                                            lifecycle,
                                            adName,
                                            viewGroup,
                                            nativeAdLoadCallback,
                                            layoutId,
                                            populator,
                                            adType,
                                            background,
                                            textColor1,
                                            textColor2,
                                            mediaMaxHeight,
                                            loadingTextSize,
                                            contentURL,
                                            neighbourContentURL,
                                            showLoadingMessage,
                                            listOf(adUnit),
                                            fetchedTimer,
                                            object: NativeInternalCallback{
                                                override fun onSuccess(nativeAd: NativeAd?) {
                                                    Log.d("native", "onSuccess: First else Fallback Shown" + System.currentTimeMillis()/1000)
                                                    nativeAdLoadCallback?.onAdLoaded()
                                                    if (nativeAd != null) {
                                                        val adView =
                                                            View.inflate(application, layoutId, null)
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
                                                            if (layoutId == R.layout.native_admob_ad_t6){
                                                                when (background) {
                                                                    is String -> {
                                                                        adView.findViewById<LinearLayout>(R.id.main_layout).setBackgroundColor(Color.parseColor(background))
                                                                    }
                                                                    is Drawable -> {
                                                                        adView.findViewById<LinearLayout>(R.id.main_layout).background = background
                                                                    }
                                                                    is Int -> {
                                                                        adView.findViewById<LinearLayout>(R.id.main_layout).setBackgroundColor(background)
                                                                    }
                                                                }
                                                            }
                                                        }
                                                        if (populator != null) {
                                                            nativeAd?.let {
                                                                populator.invoke(it, adView)
                                                            }
                                                        } else {
                                                            nativeAd?.let {
                                                                try {
                                                                populateUnifiedNativeAdView(
                                                                    it,
                                                                    adView,
                                                                    adType,
                                                                    textColor1,
                                                                    textColor2,
                                                                    application.fetchColor(adName),
                                                                    mediaMaxHeight
                                                                )
                                                            }
                                                                catch (e:Exception){

                                                                }
                                                            }
                                                        }
                                                        viewGroup.removeAllViews()
                                                        viewGroup.addView(adView)
                                                        refreshNative(adName)
                                                    }
                                                }

                                                override fun onFailure(loadAdError: LoadAdError?) {
                                                    nativeAdLoadCallback?.onAdFailed(loadAdError)
                                                }

                                            },
                                            isAdmanager = isAdmanager,
                                        )
                                    }
                                }

                            },
                            isAdmanager = isAdmanager,
                        )
                    }
                    else if (secondaryIds.size >0){
                        loadNativeAd(
                            application,
                            id,
                            lifecycle,
                            adName,
                            viewGroup,
                            nativeAdLoadCallback,
                            layoutId,
                            populator,
                            adType,
                            background,
                            textColor1,
                            textColor2,
                            mediaMaxHeight,
                            loadingTextSize,
                            contentURL,
                            neighbourContentURL,
                            showLoadingMessage,
                            secondaryIds,
                            fetchedTimer,
                            object: NativeInternalCallback{
                                override fun onSuccess(nativeAd: NativeAd?) {
                                    Log.d("native", "onSuccess: Second Secondary Shown" + System.currentTimeMillis()/1000)
                                    nativeAdLoadCallback?.onAdLoaded()
                                    if (nativeAd != null) {
                                        val adView =
                                            View.inflate(application, layoutId, null)
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
                                            if (layoutId == R.layout.native_admob_ad_t6){
                                                when (background) {
                                                    is String -> {
                                                        adView.findViewById<LinearLayout>(R.id.main_layout).setBackgroundColor(Color.parseColor(background))
                                                    }
                                                    is Drawable -> {
                                                        adView.findViewById<LinearLayout>(R.id.main_layout).background = background
                                                    }
                                                    is Int -> {
                                                        adView.findViewById<LinearLayout>(R.id.main_layout).setBackgroundColor(background)
                                                    }
                                                }
                                            }
                                        }
                                        if (populator != null) {
                                            nativeAd?.let {
                                                populator.invoke(it, adView)
                                            }
                                        } else {
                                            nativeAd?.let {
                                                try {
                                                populateUnifiedNativeAdView(
                                                    it,
                                                    adView,
                                                    adType,
                                                    textColor1,
                                                    textColor2,
                                                    application.fetchColor(adName),
                                                    mediaMaxHeight
                                                )
                                            }
                                                catch (e:Exception){

                                                }
                                            }
                                        }
                                        viewGroup.removeAllViews()
                                        viewGroup.addView(adView)
                                        refreshNative(adName)
                                    }
                                }

                                override fun onFailure(loadAdError: LoadAdError?) {
                                    loadNativeAd(
                                        application,
                                        id,
                                        lifecycle,
                                        adName,
                                        viewGroup,
                                        nativeAdLoadCallback,
                                        layoutId,
                                        populator,
                                        adType,
                                        background,
                                        textColor1,
                                        textColor2,
                                        mediaMaxHeight,
                                        loadingTextSize,
                                        contentURL,
                                        neighbourContentURL,
                                        showLoadingMessage,
                                        listOf(adUnit),
                                        fetchedTimer,
                                        object: NativeInternalCallback{
                                            override fun onSuccess(nativeAd: NativeAd?) {
                                                Log.d("native", "onSuccess: Second Fallback Shown" + System.currentTimeMillis()/1000)
                                                nativeAdLoadCallback?.onAdLoaded()
                                                if (nativeAd != null) {
                                                    val adView =
                                                        View.inflate(application, layoutId, null)
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
                                                        if (layoutId == R.layout.native_admob_ad_t6){
                                                            when (background) {
                                                                is String -> {
                                                                    adView.findViewById<LinearLayout>(R.id.main_layout).setBackgroundColor(Color.parseColor(background))
                                                                }
                                                                is Drawable -> {
                                                                    adView.findViewById<LinearLayout>(R.id.main_layout).background = background
                                                                }
                                                                is Int -> {
                                                                    adView.findViewById<LinearLayout>(R.id.main_layout).setBackgroundColor(background)
                                                                }
                                                            }
                                                        }
                                                    }
                                                    if (populator != null) {
                                                        nativeAd?.let {
                                                            populator.invoke(it, adView)
                                                        }
                                                    } else {
                                                        nativeAd?.let {
                                                            try {
                                                            populateUnifiedNativeAdView(
                                                                it,
                                                                adView,
                                                                adType,
                                                                textColor1,
                                                                textColor2,
                                                                application.fetchColor(adName),
                                                                mediaMaxHeight
                                                            )
                                                        }
                                                            catch (e:Exception){

                                                            }
                                                        }
                                                    }
                                                    viewGroup.removeAllViews()
                                                    viewGroup.addView(adView)
                                                    refreshNative(adName)
                                                }
                                            }

                                            override fun onFailure(loadAdError: LoadAdError?) {
                                                nativeAdLoadCallback?.onAdFailed(loadAdError)
                                            }

                                        },
                                        isAdmanager = isAdmanager,
                                    )
                                }

                            },
                            isAdmanager = isAdmanager,
                        )
                    }
                    else{
                        loadNativeAd(
                            application,
                            id,
                            lifecycle,
                            adName,
                            viewGroup,
                            nativeAdLoadCallback,
                            layoutId,
                            populator,
                            adType,
                            background,
                            textColor1,
                            textColor2,
                            mediaMaxHeight,
                            loadingTextSize,
                            contentURL,
                            neighbourContentURL,
                            showLoadingMessage,
                            listOf(adUnit),
                            fetchedTimer,
                            object: NativeInternalCallback{
                                override fun onSuccess(nativeAd: NativeAd?) {
                                    Log.d("native", "onSuccess: else Fallback Shown" + System.currentTimeMillis()/1000)
                                    nativeAdLoadCallback?.onAdLoaded()
                                    if (nativeAd != null) {
                                        val adView =
                                            View.inflate(application, layoutId, null)
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
                                            if (layoutId == R.layout.native_admob_ad_t6){
                                                when (background) {
                                                    is String -> {
                                                        adView.findViewById<LinearLayout>(R.id.main_layout).setBackgroundColor(Color.parseColor(background))
                                                    }
                                                    is Drawable -> {
                                                        adView.findViewById<LinearLayout>(R.id.main_layout).background = background
                                                    }
                                                    is Int -> {
                                                        adView.findViewById<LinearLayout>(R.id.main_layout).setBackgroundColor(background)
                                                    }
                                                }
                                            }
                                        }
                                        if (populator != null) {
                                            nativeAd?.let {
                                                populator.invoke(it, adView)
                                            }
                                        } else {
                                            nativeAd?.let {
                                                try {
                                                populateUnifiedNativeAdView(
                                                    it,
                                                    adView,
                                                    adType,
                                                    textColor1,
                                                    textColor2,
                                                    application.fetchColor(adName),
                                                    mediaMaxHeight
                                                )
                                            }
                                                catch (e:Exception){

                                                }
                                            }
                                        }
                                        viewGroup.removeAllViews()
                                        viewGroup.addView(adView)
                                        refreshNative(adName)
                                    }
                                }

                                override fun onFailure(loadAdError: LoadAdError?) {
                                    nativeAdLoadCallback?.onAdFailed(loadAdError)
                                }

                            },
                            isAdmanager = isAdmanager,
                        )
                    }
                }
                else {
                    if (primaryIds.size > 0){
                        loadNativeAdManager(
                            application,
                            id,
                            lifecycle,
                            adName,
                            viewGroup,
                            nativeAdLoadCallback,
                            layoutId,
                            populator,
                            adType,
                            background,
                            textColor1,
                            textColor2,
                            mediaMaxHeight,
                            loadingTextSize,
                            contentURL,
                            neighbourContentURL,
                            showLoadingMessage,
                            primaryIds,
                            fetchedTimer,
                            object: NativeInternalCallback{

                                override fun onSuccess(nativeAd: NativeAd?) {
                                    Log.d("native_admanager", "onSuccess: Primary Shown" + System.currentTimeMillis()/1000)
                                    nativeAdLoadCallback?.onAdLoaded()
                                    if (nativeAd != null) {
                                        val adView =
                                            View.inflate(application, layoutId, null)
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
                                            if (layoutId == R.layout.native_admob_ad_t6){
                                                when (background) {
                                                    is String -> {
                                                        adView.findViewById<LinearLayout>(R.id.main_layout).setBackgroundColor(Color.parseColor(background))
                                                    }
                                                    is Drawable -> {
                                                        adView.findViewById<LinearLayout>(R.id.main_layout).background = background
                                                    }
                                                    is Int -> {
                                                        adView.findViewById<LinearLayout>(R.id.main_layout).setBackgroundColor(background)
                                                    }
                                                }
                                            }
                                        }
                                        if (populator != null) {
                                            nativeAd?.let {
                                                populator.invoke(it, adView)
                                            }
                                        } else {
                                            nativeAd?.let {
                                                try {
                                                populateUnifiedNativeAdView(
                                                    it,
                                                    adView,
                                                    adType,
                                                    textColor1,
                                                    textColor2,
                                                    application.fetchColor(adName),
                                                    mediaMaxHeight
                                                )
                                            }
                                                catch (e:Exception){

                                                }
                                            }
                                        }
                                        viewGroup.removeAllViews()
                                        viewGroup.addView(adView)
                                        refreshNative(adName)
                                    }
                                }

                                override fun onFailure(loadAdError: LoadAdError?) {
                                    if (secondaryIds.size >0){
                                        loadNativeAdManager(
                                            application,
                                            id,
                                            lifecycle,
                                            adName,
                                            viewGroup,
                                            nativeAdLoadCallback,
                                            layoutId,
                                            populator,
                                            adType,
                                            background,
                                            textColor1,
                                            textColor2,
                                            mediaMaxHeight,
                                            loadingTextSize,
                                            contentURL,
                                            neighbourContentURL,
                                            showLoadingMessage,
                                            secondaryIds,
                                            fetchedTimer,
                                            object: NativeInternalCallback{
                                                override fun onSuccess(nativeAd: NativeAd?) {
                                                    Log.d("native_admanager", "onSuccess: First Secondary Shown" + System.currentTimeMillis()/1000)
                                                    nativeAdLoadCallback?.onAdLoaded()
                                                    if (nativeAd != null) {
                                                        val adView =
                                                            View.inflate(application, layoutId, null)
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
                                                            if (layoutId == R.layout.native_admob_ad_t6){
                                                                when (background) {
                                                                    is String -> {
                                                                        adView.findViewById<LinearLayout>(R.id.main_layout).setBackgroundColor(Color.parseColor(background))
                                                                    }
                                                                    is Drawable -> {
                                                                        adView.findViewById<LinearLayout>(R.id.main_layout).background = background
                                                                    }
                                                                    is Int -> {
                                                                        adView.findViewById<LinearLayout>(R.id.main_layout).setBackgroundColor(background)
                                                                    }
                                                                }
                                                            }
                                                        }
                                                        if (populator != null) {
                                                            nativeAd?.let {
                                                                populator.invoke(it, adView)
                                                            }
                                                        } else {
                                                            nativeAd?.let {
                                                                try {
                                                                populateUnifiedNativeAdView(
                                                                    it,
                                                                    adView,
                                                                    adType,
                                                                    textColor1,
                                                                    textColor2,
                                                                    application.fetchColor(adName),
                                                                    mediaMaxHeight
                                                                )
                                                            }
                                                                catch (e:Exception){

                                                                }
                                                            }
                                                        }
                                                        viewGroup.removeAllViews()
                                                        viewGroup.addView(adView)
                                                        refreshNative(adName)
                                                    }
                                                }

                                                override fun onFailure(loadAdError: LoadAdError?) {
                                                    loadNativeAdManager(
                                                        application,
                                                        id,
                                                        lifecycle,
                                                        adName,
                                                        viewGroup,
                                                        nativeAdLoadCallback,
                                                        layoutId,
                                                        populator,
                                                        adType,
                                                        background,
                                                        textColor1,
                                                        textColor2,
                                                        mediaMaxHeight,
                                                        loadingTextSize,
                                                        contentURL,
                                                        neighbourContentURL,
                                                        showLoadingMessage,
                                                        listOf(adUnit),
                                                        fetchedTimer,
                                                        object: NativeInternalCallback{
                                                            override fun onSuccess(nativeAd: NativeAd?) {
                                                                Log.d("native_admanager", "onSuccess: First Fallback Shown" + System.currentTimeMillis()/1000)
                                                                nativeAdLoadCallback?.onAdLoaded()
                                                                if (nativeAd != null) {
                                                                    val adView =
                                                                        View.inflate(application, layoutId, null)
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
                                                                        if (layoutId == R.layout.native_admob_ad_t6){
                                                                            when (background) {
                                                                                is String -> {
                                                                                    adView.findViewById<LinearLayout>(R.id.main_layout).setBackgroundColor(Color.parseColor(background))
                                                                                }
                                                                                is Drawable -> {
                                                                                    adView.findViewById<LinearLayout>(R.id.main_layout).background = background
                                                                                }
                                                                                is Int -> {
                                                                                    adView.findViewById<LinearLayout>(R.id.main_layout).setBackgroundColor(background)
                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                                    if (populator != null) {
                                                                        nativeAd?.let {
                                                                            populator.invoke(it, adView)
                                                                        }
                                                                    } else {
                                                                        nativeAd?.let {
                                                                            try {
                                                                            populateUnifiedNativeAdView(
                                                                                it,
                                                                                adView,
                                                                                adType,
                                                                                textColor1,
                                                                                textColor2,
                                                                                application.fetchColor(adName),
                                                                                mediaMaxHeight
                                                                            )
                                                                        }
                                                                            catch (e:Exception){

                                                                            }
                                                                        }
                                                                    }
                                                                    viewGroup.removeAllViews()
                                                                    viewGroup.addView(adView)
                                                                    refreshNative(adName)
                                                                }
                                                            }

                                                            override fun onFailure(loadAdError: LoadAdError?) {
                                                                nativeAdLoadCallback?.onAdFailed(loadAdError)
                                                            }

                                                        },
                                                        isAdmanager = isAdmanager,
                                                    )
                                                }

                                            },
                                            isAdmanager = isAdmanager,
                                        )
                                    }
                                    else{
                                        loadNativeAdManager(
                                            application,
                                            id,
                                            lifecycle,
                                            adName,
                                            viewGroup,
                                            nativeAdLoadCallback,
                                            layoutId,
                                            populator,
                                            adType,
                                            background,
                                            textColor1,
                                            textColor2,
                                            mediaMaxHeight,
                                            loadingTextSize,
                                            contentURL,
                                            neighbourContentURL,
                                            showLoadingMessage,
                                            listOf(adUnit),
                                            fetchedTimer,
                                            object: NativeInternalCallback{
                                                override fun onSuccess(nativeAd: NativeAd?) {
                                                    Log.d("native_admanager", "onSuccess: First else Fallback Shown" + System.currentTimeMillis()/1000)
                                                    nativeAdLoadCallback?.onAdLoaded()
                                                    if (nativeAd != null) {
                                                        val adView =
                                                            View.inflate(application, layoutId, null)
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
                                                            if (layoutId == R.layout.native_admob_ad_t6){
                                                                when (background) {
                                                                    is String -> {
                                                                        adView.findViewById<LinearLayout>(R.id.main_layout).setBackgroundColor(Color.parseColor(background))
                                                                    }
                                                                    is Drawable -> {
                                                                        adView.findViewById<LinearLayout>(R.id.main_layout).background = background
                                                                    }
                                                                    is Int -> {
                                                                        adView.findViewById<LinearLayout>(R.id.main_layout).setBackgroundColor(background)
                                                                    }
                                                                }
                                                            }
                                                        }
                                                        if (populator != null) {
                                                            nativeAd?.let {
                                                                populator.invoke(it, adView)
                                                            }
                                                        } else {
                                                            nativeAd?.let {
                                                                try {
                                                                populateUnifiedNativeAdView(
                                                                    it,
                                                                    adView,
                                                                    adType,
                                                                    textColor1,
                                                                    textColor2,
                                                                    application.fetchColor(adName),
                                                                    mediaMaxHeight
                                                                )
                                                            }
                                                                catch (e:Exception){

                                                                }
                                                            }
                                                        }
                                                        viewGroup.removeAllViews()
                                                        viewGroup.addView(adView)
                                                        refreshNative(adName)
                                                    }
                                                }

                                                override fun onFailure(loadAdError: LoadAdError?) {
                                                    nativeAdLoadCallback?.onAdFailed(loadAdError)
                                                }

                                            },
                                            isAdmanager = isAdmanager,
                                        )
                                    }
                                }

                            },
                            isAdmanager = isAdmanager,
                        )
                    }
                    else if (secondaryIds.size >0){
                        loadNativeAdManager(
                            application,
                            id,
                            lifecycle,
                            adName,
                            viewGroup,
                            nativeAdLoadCallback,
                            layoutId,
                            populator,
                            adType,
                            background,
                            textColor1,
                            textColor2,
                            mediaMaxHeight,
                            loadingTextSize,
                            contentURL,
                            neighbourContentURL,
                            showLoadingMessage,
                            secondaryIds,
                            fetchedTimer,
                            object: NativeInternalCallback{
                                override fun onSuccess(nativeAd: NativeAd?) {
                                    Log.d("native_admanager", "onSuccess: Second Secondary Shown" + System.currentTimeMillis()/1000)
                                    nativeAdLoadCallback?.onAdLoaded()
                                    if (nativeAd != null) {
                                        val adView =
                                            View.inflate(application, layoutId, null)
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
                                            if (layoutId == R.layout.native_admob_ad_t6){
                                                when (background) {
                                                    is String -> {
                                                        adView.findViewById<LinearLayout>(R.id.main_layout).setBackgroundColor(Color.parseColor(background))
                                                    }
                                                    is Drawable -> {
                                                        adView.findViewById<LinearLayout>(R.id.main_layout).background = background
                                                    }
                                                    is Int -> {
                                                        adView.findViewById<LinearLayout>(R.id.main_layout).setBackgroundColor(background)
                                                    }
                                                }
                                            }
                                        }
                                        if (populator != null) {
                                            nativeAd?.let {
                                                populator.invoke(it, adView)
                                            }
                                        } else {
                                            nativeAd?.let {
                                                try {
                                                populateUnifiedNativeAdView(
                                                    it,
                                                    adView,
                                                    adType,
                                                    textColor1,
                                                    textColor2,
                                                        application.fetchColor(adName),
                                                    mediaMaxHeight
                                                )
                                            }
                                                catch (e:Exception){

                                                }
                                            }
                                        }
                                        viewGroup.removeAllViews()
                                        viewGroup.addView(adView)
                                        refreshNative(adName)
                                    }
                                }

                                override fun onFailure(loadAdError: LoadAdError?) {
                                    loadNativeAdManager(
                                        application,
                                        id,
                                        lifecycle,
                                        adName,
                                        viewGroup,
                                        nativeAdLoadCallback,
                                        layoutId,
                                        populator,
                                        adType,
                                        background,
                                        textColor1,
                                        textColor2,
                                        mediaMaxHeight,
                                        loadingTextSize,
                                        contentURL,
                                        neighbourContentURL,
                                        showLoadingMessage,
                                        listOf(adUnit),
                                        fetchedTimer,
                                        object: NativeInternalCallback{
                                            override fun onSuccess(nativeAd: NativeAd?) {
                                                Log.d("native_admanager", "onSuccess: Second Fallback Shown" + System.currentTimeMillis()/1000)
                                                nativeAdLoadCallback?.onAdLoaded()
                                                if (nativeAd != null) {
                                                    val adView =
                                                        View.inflate(application, layoutId, null)
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
                                                        if (layoutId == R.layout.native_admob_ad_t6){
                                                            when (background) {
                                                                is String -> {
                                                                    adView.findViewById<LinearLayout>(R.id.main_layout).setBackgroundColor(Color.parseColor(background))
                                                                }
                                                                is Drawable -> {
                                                                    adView.findViewById<LinearLayout>(R.id.main_layout).background = background
                                                                }
                                                                is Int -> {
                                                                    adView.findViewById<LinearLayout>(R.id.main_layout).setBackgroundColor(background)
                                                                }
                                                            }
                                                        }
                                                    }
                                                    if (populator != null) {
                                                        nativeAd?.let {
                                                            populator.invoke(it, adView)
                                                        }
                                                    } else {
                                                        nativeAd?.let {
                                                            try {
                                                            populateUnifiedNativeAdView(
                                                                it,
                                                                adView,
                                                                adType,
                                                                textColor1,
                                                                textColor2,
                                                                    application.fetchColor(adName),
                                                                mediaMaxHeight
                                                            )
                                                        }
                                                            catch (e:Exception){

                                                            }
                                                        }
                                                    }
                                                    viewGroup.removeAllViews()
                                                    viewGroup.addView(adView)
                                                    refreshNative(adName)
                                                }
                                            }

                                            override fun onFailure(loadAdError: LoadAdError?) {
                                                nativeAdLoadCallback?.onAdFailed(loadAdError)
                                            }

                                        },
                                        isAdmanager = isAdmanager,
                                    )
                                }

                            },
                            isAdmanager = isAdmanager,
                        )
                    }
                    else{
                        loadNativeAdManager(
                            application,
                            id,
                            lifecycle,
                            adName,
                            viewGroup,
                            nativeAdLoadCallback,
                            layoutId,
                            populator,
                            adType,
                            background,
                            textColor1,
                            textColor2,
                            mediaMaxHeight,
                            loadingTextSize,
                            contentURL,
                            neighbourContentURL,
                            showLoadingMessage,
                            listOf(adUnit),
                            fetchedTimer,
                            object: NativeInternalCallback{
                                override fun onSuccess(nativeAd: NativeAd?) {
                                    Log.d("native_admanager", "onSuccess: else Fallback Shown" + System.currentTimeMillis()/1000)
                                    nativeAdLoadCallback?.onAdLoaded()
                                    if (nativeAd != null) {
                                        val adView =
                                            View.inflate(application, layoutId, null)
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
                                            if (layoutId == R.layout.native_admob_ad_t6){
                                                when (background) {
                                                    is String -> {
                                                        adView.findViewById<LinearLayout>(R.id.main_layout).setBackgroundColor(Color.parseColor(background))
                                                    }
                                                    is Drawable -> {
                                                        adView.findViewById<LinearLayout>(R.id.main_layout).background = background
                                                    }
                                                    is Int -> {
                                                        adView.findViewById<LinearLayout>(R.id.main_layout).setBackgroundColor(background)
                                                    }
                                                }
                                            }
                                        }
                                        if (populator != null) {
                                            nativeAd?.let {
                                                populator.invoke(it, adView)
                                            }
                                        } else {
                                            nativeAd?.let {
                                                try {
                                                populateUnifiedNativeAdView(
                                                    it,
                                                    adView,
                                                    adType,
                                                    textColor1,
                                                    textColor2,
                                                        application.fetchColor(adName),
                                                    mediaMaxHeight
                                                )
                                            }
                                                catch (e:Exception){

                                                }
                                            }
                                        }
                                        viewGroup.removeAllViews()
                                        viewGroup.addView(adView)
                                        refreshNative(adName)
                                    }
                                }

                                override fun onFailure(loadAdError: LoadAdError?) {
                                    nativeAdLoadCallback?.onAdFailed(loadAdError)
                                }

                            },
                            isAdmanager = isAdmanager,
                        )
                    }
                }

            } else {
                AdUtilConstants.nativeAdLifeCycleHashMap.remove(id)
            }
        } else {
            viewGroup.visibility = GONE
        }
    }

    internal fun loadNativeAd(
        application: Context,
        id: Long = System.currentTimeMillis(),
        lifecycle: Lifecycle,
        adName: String,
        viewGroup: ViewGroup,
        nativeAdLoadCallback: NativeAdLoadCallback?,
        @LayoutRes layoutId: Int = R.layout.native_admob_ad_t1,
        populator: ((nativeAd: NativeAd, adView: NativeAdView) -> Unit)? = null,
        adType: String = "1",
        background: Any?,
        textColor1: Int?,
        textColor2: Int?,
        mediaMaxHeight: Int = 300,
        loadingTextSize: Int,
        contentURL: String? = null,
        neighbourContentURL: List<String>? = null,
        showLoadingMessage: Boolean,
        primaryIds: List<String>,
        fetchedTimer: Int,
        nativeInternalCallback: NativeInternalCallback,
        isAdmanager: Boolean
    ){
        var nativeAd: NativeAd? = null
        var loadAdError: LoadAdError? = null
        object : CountDownTimer(fetchedTimer.toLong(), 500) {
            override fun onTick(millisUntilFinished: Long) {
                if (nativeAd != null) {
                    nativeInternalCallback.onSuccess(nativeAd)
                    this.cancel()
                }
            }

            override fun onFinish() {
                if (nativeAd != null) {
                    nativeInternalCallback.onSuccess(nativeAd)
                }
                else
                    nativeInternalCallback.onFailure(loadAdError)
            }
        }.start()
        var loadedId = ""
        for (adUnit in primaryIds){
            lifecycle.addObserver(object : LifecycleObserver {
                @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
                fun onDestroy() {
                    AdUtilConstants.nativeAdLifeCycleHashMap.remove(id)
                }
            })

            val adLoader: AdLoader = AdLoader.Builder(application, adUnit)
                .forNativeAd { ad: NativeAd ->
                    if (nativeAd == null && ad != null) {
                        nativeAd = ad
                        loadedId = adUnit
                    }
                }
                .withAdListener(object : AdListener() {

                    override fun onAdClicked() {
                        super.onAdClicked()
                        nativeAdLoadCallback?.onAdClicked()
                    }

                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        if (nativeAd == null)
                            nativeAd = null
                        viewGroup.removeAllViews()
                        loadAdError = adError
                        Log.d(TAG, "onAdFailedToLoad: "+adName+" : "+adUnit+" : "+adError.message)
//                        nativeAdLoadCallback?.onAdFailed(adError)
                    }

                    override fun onAdLoaded() {
                        super.onAdLoaded()
                        if (nativeAd !=null){
                            if (AdUtilConstants.nativeAdLifeCycleHashMap[id] == null) {
                                AdUtilConstants.nativeAdLifeCycleHashMap[id] = NativeAdItem(
                                    application,
                                    id,
                                    lifecycle,
                                    if (loadedId.equals("")) adUnit else loadedId,
                                    adName,
                                    viewGroup,
                                    nativeAdLoadCallback,
                                    layoutId,
                                    populator,
                                    adType,
                                    background,
                                    textColor1,
                                    textColor2,
                                    mediaMaxHeight,
                                    loadingTextSize,
                                    contentURL = contentURL,
                                    neighbourContentURL = neighbourContentURL,
                                    showLoadingMessage = showLoadingMessage,
                                    isAdManager = isAdmanager
                                )
                            }
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
            val builder = AdRequest.Builder().addNetworkExtrasBundle(
                AdMobAdapter::class.java,
                getConsentEnabledBundle()
            )
            contentURL?.let { builder.setContentUrl(it) }
            neighbourContentURL?.let { builder.setNeighboringContentUrls(it) }
            adLoader.loadAd(
                builder.build()
            )
        }

    }

    internal fun loadNativeAdManager(
        application: Context,
        id: Long = System.currentTimeMillis(),
        lifecycle: Lifecycle,
        adName: String,
        viewGroup: ViewGroup,
        nativeAdLoadCallback: NativeAdLoadCallback?,
        @LayoutRes layoutId: Int = R.layout.native_admob_ad_t1,
        populator: ((nativeAd: NativeAd, adView: NativeAdView) -> Unit)? = null,
        adType: String = "1",
        background: Any?,
        textColor1: Int?,
        textColor2: Int?,
        mediaMaxHeight: Int = 300,
        loadingTextSize: Int,
        contentURL: String? = null,
        neighbourContentURL: List<String>? = null,
        showLoadingMessage: Boolean,
        primaryIds: List<String>,
        fetchedTimer: Int,
        nativeInternalCallback: NativeInternalCallback,
        isAdmanager: Boolean
    ){
        var nativeAd: NativeAd? = null
        var loadAdError: LoadAdError? = null
        object : CountDownTimer(fetchedTimer.toLong(), 500) {
            override fun onTick(millisUntilFinished: Long) {
                if (nativeAd != null) {
                    nativeInternalCallback.onSuccess(nativeAd)
                    this.cancel()
                }
            }

            override fun onFinish() {
                if (nativeAd != null) {
                    nativeInternalCallback.onSuccess(nativeAd)
                }
                else
                    nativeInternalCallback.onFailure(loadAdError)
            }
        }.start()
        var loadedId = ""
        for (adUnit in primaryIds){
            lifecycle.addObserver(object : LifecycleObserver {
                @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
                fun onDestroy() {
                    AdUtilConstants.nativeAdLifeCycleHashMap.remove(id)
                }
            })

            val adLoader: AdLoader = AdLoader.Builder(application, adUnit)
                .forNativeAd { ad: NativeAd ->
                    if (nativeAd == null && ad != null) {
                        nativeAd = ad
                        loadedId = adUnit
                    }
                }
                .withAdListener(object : AdListener() {

                    override fun onAdClicked() {
                        super.onAdClicked()
                        nativeAdLoadCallback?.onAdClicked()
                    }

                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        if (nativeAd == null)
                            nativeAd = null
                        loadAdError = adError
                        Log.d(TAG, "onAdFailedToLoad: "+adName+" : "+adUnit+" : "+adError.message)

//                        nativeAdLoadCallback?.onAdFailed(adError)
                    }

                    override fun onAdLoaded() {
                        super.onAdLoaded()
                        if (nativeAd !=null){
                            if (AdUtilConstants.nativeAdLifeCycleHashMap[id] == null) {
                                AdUtilConstants.nativeAdLifeCycleHashMap[id] = NativeAdItem(
                                    application,
                                    id,
                                    lifecycle,
                                    if (loadedId.equals("")) adUnit else loadedId,
                                    adName,
                                    viewGroup,
                                    nativeAdLoadCallback,
                                    layoutId,
                                    populator,
                                    adType,
                                    background,
                                    textColor1,
                                    textColor2,
                                    mediaMaxHeight,
                                    loadingTextSize,
                                    contentURL = contentURL,
                                    neighbourContentURL = neighbourContentURL,
                                    showLoadingMessage = showLoadingMessage,
                                    isAdManager = isAdmanager
                                )
                            }
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
            val builder = AdManagerAdRequest.Builder()
            contentURL?.let { builder.setContentUrl(it) }
            neighbourContentURL?.let { builder.setNeighboringContentUrls(it) }
            adLoader.loadAd(
                builder.build()
            )
        }

    }

    internal fun loadNativeAdRefresh(
        application: Context,
        id: Long = System.currentTimeMillis(),
        lifecycle: Lifecycle,
        adUnit: String,
        adName: String,
        viewGroup: ViewGroup,
        nativeAdLoadCallback: NativeAdLoadCallback?,
        @LayoutRes layoutId: Int = R.layout.native_admob_ad_t1,
        populator: ((nativeAd: NativeAd, adView: NativeAdView) -> Unit)? = null,
        adType: String = "1",
        background: Any?,
        textColor1: Int?,
        textColor2: Int?,
        mediaMaxHeight: Int = 300,
        loadingTextSize: Int,
        contentURL: String? = null,
        neighbourContentURL: List<String>? = null,
        showLoadingMessage: Boolean,
        isAdmanager: Boolean,
    ) {
        if (application != null){
            if (adUnit != "STOP" && AppPref.getBoolean(application,AppPref.showAppAds) && application.fetchAdStatusFromAdId(adName)) {
                viewGroup.visibility = VISIBLE
                if (application != null) {
                    val inflate = View.inflate(application, R.layout.shimmer, null)
                    when (adType) {
                        "6" -> {
                            inflate.findViewById<LinearLayout>(R.id.shim_default).visibility = View.VISIBLE
                        }/*DEFAULT_AD*/
                        "3" -> {
                            inflate.findViewById<LinearLayout>(R.id.shim_small).visibility = View.VISIBLE
                        }
                        /*SMALL*/
                        "4" -> {
                            inflate.findViewById<LinearLayout>(R.id.shim_medium).visibility = View.VISIBLE
                        }
                        /*MEDIUM*/
                        "1" -> {
                            inflate.findViewById<LinearLayout>(R.id.shim_bigv1).visibility = View.VISIBLE
                        }
                        /*BIGV1*/
                        "5" -> {
                            inflate.findViewById<LinearLayout>(R.id.shim_bigv2).visibility = View.VISIBLE
                        }
                        /*BIGV2*/
                        "2" -> {
                            inflate.findViewById<LinearLayout>(R.id.shim_bigv3).visibility = View.VISIBLE
                        }
                        /*BIGV3*/
                        "7" -> {
                            inflate.findViewById<LinearLayout>(R.id.shim_gridad).visibility = View.VISIBLE
                        }
                        /*GRID_AD*/
                        else -> {
                            inflate.findViewById<LinearLayout>(R.id.shim_default).visibility = View.VISIBLE
                        }

                    }
                    viewGroup.removeAllViews()
                    if (showLoadingMessage) {
                        viewGroup.addView(inflate)
                    }
                    if (adUnit.isBlank()) return
                    if (AdUtilConstants.nativeAdLifeCycleHashMap[id] == null) {
                        AdUtilConstants.nativeAdLifeCycleHashMap[id] = NativeAdItem(
                            application,
                            id,
                            lifecycle,
                            adUnit,
                            adName,
                            viewGroup,
                            nativeAdLoadCallback,
                            layoutId,
                            populator,
                            adType,
                            background,
                            textColor1,
                            textColor2,
                            mediaMaxHeight,
                            loadingTextSize,
                            contentURL = contentURL,
                            neighbourContentURL = neighbourContentURL,
                            showLoadingMessage = showLoadingMessage,
                            isAdManager = isAdmanager
                        )
                    }
                    lifecycle.addObserver(object : LifecycleObserver {
                        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
                        fun onDestroy() {
                            AdUtilConstants.nativeAdLifeCycleHashMap.remove(id)
                        }
                    })
                    var nativeAd: NativeAd? = null
                    val adLoader: AdLoader = AdLoader.Builder(application, adUnit)
                        .forNativeAd { ad: NativeAd ->
                            nativeAd = ad
                        }
                        .withAdListener(object : AdListener() {

                            override fun onAdClicked() {
                                super.onAdClicked()
                                nativeAdLoadCallback?.onAdClicked()
                            }

                            override fun onAdFailedToLoad(adError: LoadAdError) {
                                nativeAdLoadCallback?.onAdFailed(adError)
                            }

                            override fun onAdLoaded() {
                                super.onAdLoaded()
                                nativeAdLoadCallback?.onAdLoaded()
                                if (nativeAd != null) {
                                    val adView =
                                        View.inflate(application, layoutId, null)
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
                                        if (layoutId == R.layout.native_admob_ad_t6){
                                            when (background) {
                                                is String -> {
                                                    adView.findViewById<LinearLayout>(R.id.main_layout).setBackgroundColor(Color.parseColor(background))
                                                }
                                                is Drawable -> {
                                                    adView.findViewById<LinearLayout>(R.id.main_layout).background = background
                                                }
                                                is Int -> {
                                                    adView.findViewById<LinearLayout>(R.id.main_layout).setBackgroundColor(background)
                                                }
                                            }
                                        }
                                    }
                                    if (populator != null) {
                                        nativeAd?.let {
                                            populator.invoke(it, adView)
                                        }
                                    } else {
                                        nativeAd?.let {
                                            try {
                                                populateUnifiedNativeAdView(
                                                    it,
                                                    adView,
                                                    adType,
                                                    textColor1,
                                                    textColor2,
                                                    application.fetchColor(adName),
                                                    mediaMaxHeight
                                                )
                                            }
                                            catch (e:Exception){

                                            }
                                        }
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
                    if (!isAdmanager){
                        val builder = AdRequest.Builder().addNetworkExtrasBundle(
                            AdMobAdapter::class.java,
                            getConsentEnabledBundle()
                        )
                        contentURL?.let { builder.setContentUrl(it) }
                        neighbourContentURL?.let { builder.setNeighboringContentUrls(it) }
                        adLoader.loadAd(
                            builder.build()
                        )
                        Log.d("native_refresher", "loadNativeAdRefresh: admob   "+adName+"  "+System.currentTimeMillis()/1000)
                    }
                    else {
                        val builder = AdManagerAdRequest.Builder()
                        contentURL?.let { builder.setContentUrl(it) }
                        neighbourContentURL?.let { builder.setNeighboringContentUrls(it) }
                        adLoader.loadAd(
                            builder.build()
                        )
                        Log.d("native_refresher", "loadNativeAdRefresh: admanager   "+adName+"  "+System.currentTimeMillis()/1000)
                    }


                } else {
                    AdUtilConstants.nativeAdLifeCycleHashMap.remove(id)
                }
            } else {
                viewGroup.visibility = GONE
            }
        }
        else{
            nativeAdLoadCallback?.onContextFailed()
            viewGroup.visibility = GONE
        }

    }


    fun disableBannerRefresh() {
        bannerRefresh = REFRESH_STATE.REFRESH_OFF
    }

    fun enableBannerRefresh() {
        bannerRefresh = REFRESH_STATE.REFRESH_ON
    }

    fun disableNativeRefresh() {
        nativeRefresh = REFRESH_STATE.REFRESH_OFF
    }

    fun enableNativeRefresh() {
        nativeRefresh = REFRESH_STATE.REFRESH_ON
    }

    fun removeNativeAdFromService(
        viewGroup: ViewGroup,
    ): Boolean {
        val id = viewGroup.id.toLong()
        return if (AdUtilConstants.nativeAdLifeCycleServiceHashMap.containsKey(id)) {
            viewGroup.removeAllViews()
            AdUtilConstants.nativeAdLifeCycleServiceHashMap.remove(id)
            true
        } else {
            false
        }
    }

    /**
     * Call loadNativeAdFromService if activity is not available
     **/
    fun loadNativeAdFromSrvs(
        layoutInflater: LayoutInflater,
        context: Context,
        lifecycle: Lifecycle,
        adUnit: String,
        adName: String,
        viewGroup: ViewGroup,
        nativeAdLoadCallback: NativeAdLoadCallback?,
        adType: String = "1",
        mediaMaxHeight: Int = 300,
        loadingTextSize: Int = 24,
        background: Any?,
        textColor1: Int?,
        textColor2: Int?,
        id: Long = viewGroup.id.toLong(),
        populator: ((nativeAd: NativeAd, adView: NativeAdView) -> Unit)? = null,
        preloadAds: Boolean = false,
        autoRefresh: Boolean = false,
        contentURL: String? = null,
        neighbourContentURL: List<String>? = null,
        showLoadingMessage: Boolean = true,
        isAdmanager:Boolean = false,
        loadTimeOut:Int
    ){
        if (isInitialized){
            if (context != null){
                if (AppPref.getBoolean(context,AppPref.showAppAds) && context.fetchAdStatusFromAdId(adName)) {
                    loadNativeAdFromService(
                        layoutInflater,
                        context,
                        lifecycle,
                        adUnit,
                        adName,
                        viewGroup,
                        nativeAdLoadCallback,
                        background = background,
                        textColor1 = textColor1,
                        textColor2 = textColor2,
                        mediaMaxHeight = mediaMaxHeight,
                        loadingTextSize = loadingTextSize,
                        id = id,
                        populator = populator,
                        adType = adType,
                        preloadAds = preloadAds,
                        autoRefresh = preloadAds,
                        contentURL = contentURL,
                        neighbourContentURL = neighbourContentURL,
                        showLoadingMessage = showLoadingMessage,
                        isAdmanager = isAdmanager,
                        loadTimeOut = loadTimeOut
                    )
                }
                else {
                    viewGroup.visibility = GONE
                }
            }
            else {
                nativeAdLoadCallback?.onContextFailed()
                viewGroup.visibility = GONE
            }

        }
        else {
            viewGroup.visibility = GONE
            nativeAdLoadCallback?.onAdFailed(LoadAdError(404, "Ads SDK not (initialized / properly initial). Try initializing again.", "",null,null))
        }

    }

    internal fun loadNativeAdFromServiceRefresh(
        layoutInflater: LayoutInflater,
        context: Context,
        lifecycle: Lifecycle,
        adUnit: String,
        adName: String,
        viewGroup: ViewGroup,
        nativeAdLoadCallback: NativeAdLoadCallback?,
        adType: String = "1",
        mediaMaxHeight: Int = 300,
        loadingTextSize: Int = 24,
        background: Any?,
        textColor1: Int?,
        textColor2: Int?,
        id: Long = viewGroup.id.toLong(),
        populator: ((nativeAd: NativeAd, adView: NativeAdView) -> Unit)? = null,
        preloadAds: Boolean = false,
        autoRefresh: Boolean = false,
        contentURL: String? = null,
        neighbourContentURL: List<String>? = null,
        showLoadingMessage: Boolean = true,
        isAdmanager: Boolean
    ) {
        if (isInitialized){
            if (context != null){
                if (adUnit != "STOP" && AppPref.getBoolean(context,AppPref.showAppAds) && context.fetchAdStatusFromAdId(adName)) {
                    var newAdSize = context.fetchAdSize(adName,adType)
                    @LayoutRes val layoutId = when (newAdSize) {
                        "6" -> { R.layout.native_admob_ad_t6
                        }/*DEFAULT_AD*/
                        "3" -> R.layout.native_admob_ad_t3/*SMALL*/
                        "4" -> R.layout.native_admob_ad_t4/*MEDIUM*/
                        "1" -> R.layout.native_admob_ad_t1/*BIGV1*/
                        "5" -> R.layout.native_admob_ad_t5/*BIGV2*/
                        "2" -> R.layout.native_admob_ad_t2/*BIGV3*/
                        "7" -> R.layout.native_admob_ad_t7
                        else -> R.layout.native_admob_ad_t1
                    }
                    viewGroup.visibility = VISIBLE
                    val inflate = View.inflate(context, R.layout.shimmer, null)
                    when (newAdSize) {
                        "6" -> {
                            inflate.findViewById<LinearLayout>(R.id.shim_default).visibility = View.VISIBLE
                        }/*DEFAULT_AD*/
                        "3" -> {
                            inflate.findViewById<LinearLayout>(R.id.shim_small).visibility = View.VISIBLE
                        }
                        /*SMALL*/
                        "4" -> {
                            inflate.findViewById<LinearLayout>(R.id.shim_medium).visibility = View.VISIBLE
                        }
                        /*MEDIUM*/
                        "1" -> {
                            inflate.findViewById<LinearLayout>(R.id.shim_bigv1).visibility = View.VISIBLE
                        }
                        /*BIGV1*/
                        "5" -> {
                            inflate.findViewById<LinearLayout>(R.id.shim_bigv2).visibility = View.VISIBLE
                        }
                        /*BIGV2*/
                        "2" -> {
                            inflate.findViewById<LinearLayout>(R.id.shim_bigv3).visibility = View.VISIBLE
                        }
                        /*BIGV3*/
                        "7" -> {
                            inflate.findViewById<LinearLayout>(R.id.shim_gridad).visibility = View.VISIBLE
                        }
                        /*GRID_AD*/
                        else -> {
                            inflate.findViewById<LinearLayout>(R.id.shim_default).visibility = View.VISIBLE
                        }

                    }
                    viewGroup.removeAllViews()
                    if (showLoadingMessage)
                        viewGroup.addView(inflate)
                    if (adUnit.isBlank()) return
                    if (AdUtilConstants.nativeAdLifeCycleServiceHashMap[id] == null) {
                        AdUtilConstants.nativeAdLifeCycleServiceHashMap[id] = NativeAdItemService(
                            layoutInflater,
                            context,
                            lifecycle,
                            id,
                            adUnit,
                            adName,
                            viewGroup,
                            nativeAdLoadCallback,
                            populator,
                            newAdSize,
                            background,
                            textColor1,
                            textColor2,
                            mediaMaxHeight,
                            loadingTextSize,
                            preloadAds,
                            autoRefresh,
                            contentURL,
                            neighbourContentURL,
                            showLoadingMessage = showLoadingMessage,
                            isAdmanager = isAdmanager
                        )
                    }
                    var nativeAd: NativeAd? = null
                    val adLoader: AdLoader = AdLoader.Builder(context, adUnit)
                        .forNativeAd { ad: NativeAd ->
                            nativeAd = ad
                        }
                        .withAdListener(object : AdListener() {

                            override fun onAdClicked() {
                                super.onAdClicked()
                                nativeAdLoadCallback?.onAdClicked()
                            }

                            override fun onAdFailedToLoad(adError: LoadAdError) {
                                nativeAdLoadCallback?.onAdFailed(adError)
                            }

                            override fun onAdLoaded() {
                                super.onAdLoaded()
                                nativeAdLoadCallback?.onAdLoaded()
                                if (nativeAd != null) {
                                    val adView = layoutInflater.inflate(layoutId, null)
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
                                        if (layoutId == R.layout.native_admob_ad_t6){
                                            when (background) {
                                                is String -> {
                                                    adView.findViewById<LinearLayout>(R.id.main_layout).setBackgroundColor(Color.parseColor(background))
                                                }
                                                is Drawable -> {
                                                    adView.findViewById<LinearLayout>(R.id.main_layout).background = background
                                                }
                                                is Int -> {
                                                    adView.findViewById<LinearLayout>(R.id.main_layout).setBackgroundColor(background)
                                                }
                                            }
                                        }
                                    }
                                    if (populator != null) {
                                        populator.invoke(nativeAd!!, adView)
                                    } else {
                                        populateUnifiedNativeAdView(
                                            nativeAd!!,
                                            adView,
                                            newAdSize,
                                            textColor1,
                                            textColor2,
                                            context.fetchColor(adName),
                                            mediaMaxHeight
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
                        ).build()
                    if (preloadNativeAdList != null) {
                        val preloadNativeAds = preloadNativeAdList!![adName]
                        val ad = preloadNativeAds?.ad
                        if (ad != null) {
                            viewGroup.removeAllViews()
                            viewGroup.addView(ad)
                            preloadNativeAds.ad = null
                            if (preloadAds) {
                                preloadAds(layoutInflater, context)
                            }
                            Log.d("refreshNativeService", "loadNativeAdFromServiceRefresh: ad exist")
                        } else {
                            if (preloadAds) {
                                preloadAds(layoutInflater, context)
                            }
                            Log.d("refreshNativeService", "loadNativeAdFromServiceRefresh: ad not exist")
                            loadAd(
                                adLoader,
                                contentURL,
                                neighbourContentURL,
                                isAdmanager
                            )
                            /*The Extra Parameters are just for logging*/
                        }
                    } else {
                        Log.d("refreshNativeService", "loadNativeAdFromServiceRefresh: ad exist else")
                        if (preloadAds) {
                            preloadAds(layoutInflater, context)
                        }
                        loadAd(adLoader, contentURL, neighbourContentURL,isAdmanager)
                    }
                } else {
                    viewGroup.visibility = GONE
                }
            }
            else {
                nativeAdLoadCallback?.onContextFailed()
                viewGroup.visibility = GONE
            }
        }
        else {
            viewGroup.visibility = GONE
            nativeAdLoadCallback?.onAdFailed(LoadAdError(404, "Ads SDK not (initialized / properly initial). Try initializing again.", "",null,null))
        }
    }

    internal fun loadNativeAdFromService(
        layoutInflater: LayoutInflater,
        context: Context,
        lifecycle: Lifecycle,
        adUnit: String,
        adName: String,
        viewGroup: ViewGroup,
        nativeAdLoadCallback: NativeAdLoadCallback?,
        adType: String = "1",
        mediaMaxHeight: Int = 300,
        loadingTextSize: Int = 24,
        background: Any?,
        textColor1: Int?,
        textColor2: Int?,
        id: Long = viewGroup.id.toLong(),
        populator: ((nativeAd: NativeAd, adView: NativeAdView) -> Unit)? = null,
        preloadAds: Boolean = false,
        autoRefresh: Boolean = false,
        contentURL: String? = null,
        neighbourContentURL: List<String>? = null,
        showLoadingMessage: Boolean = true,
        isAdmanager:Boolean = false,
        loadTimeOut:Int
    ) {
        if (adUnit != "STOP" && AppPref.getBoolean(context,AppPref.showAppAds) && context.fetchAdStatusFromAdId(adName)) {
            var newAdSize = context.fetchAdSize(adName,adType)
            @LayoutRes val layoutId = when (newAdSize) {
                "1" -> R.layout.native_admob_ad_t1/*MEDIUM*/
                "2" -> R.layout.native_admob_ad_t2/*SEMIMEDIUM*/
                "3" -> R.layout.native_admob_ad_t3/*SMALLEST*/
                "4" -> R.layout.native_admob_ad_t4/*SMALLER*/
                "5" -> R.layout.native_admob_ad_t5/*BIG*/
                "6" -> R.layout.native_admob_ad_t6/*DEFAULT NATIVE SMALL*/
                "7" -> R.layout.native_admob_ad_t7
                else -> R.layout.native_admob_ad_t1
            }
            viewGroup.visibility = VISIBLE

            var fetchedTimer:Int = context.fetchAdLoadTimeout(adName)
            if (fetchedTimer == 0){
                fetchedTimer = loadTimeOut
            }
            var primaryIds = context.fetchPrimaryById(adName)
            var secondaryIds = context.fetchSecondaryById(adName)

            val inflate = View.inflate(context, R.layout.shimmer, null)
            when (newAdSize) {
                "6" -> {
                    inflate.findViewById<LinearLayout>(R.id.shim_default).visibility = View.VISIBLE
                }/*DEFAULT_AD*/
                "3" -> {
                    inflate.findViewById<LinearLayout>(R.id.shim_small).visibility = View.VISIBLE
                }
                /*SMALL*/
                "4" -> {
                    inflate.findViewById<LinearLayout>(R.id.shim_medium).visibility = View.VISIBLE
                }
                /*MEDIUM*/
                "1" -> {
                    inflate.findViewById<LinearLayout>(R.id.shim_bigv1).visibility = View.VISIBLE
                }
                /*BIGV1*/
                "5" -> {
                    inflate.findViewById<LinearLayout>(R.id.shim_bigv2).visibility = View.VISIBLE
                }
                /*BIGV2*/
                "2" -> {
                    inflate.findViewById<LinearLayout>(R.id.shim_bigv3).visibility = View.VISIBLE
                }
                /*BIGV3*/
                "7" -> {
                    inflate.findViewById<LinearLayout>(R.id.shim_gridad).visibility = View.VISIBLE
                }
                /*GRID_AD*/
                else -> {
                    inflate.findViewById<LinearLayout>(R.id.shim_default).visibility = View.VISIBLE
                }

            }

            viewGroup.removeAllViews()
            if (showLoadingMessage)
                viewGroup.addView(inflate)
            if (adUnit.isBlank()) return
            if (preloadNativeAdList != null) {
                val preloadNativeAds = preloadNativeAdList!![adName]
                val ad = preloadNativeAds?.ad
                if (ad != null) {
                    viewGroup.removeAllViews()
                    viewGroup.addView(ad)
                    preloadNativeAds.ad = null
                    if (preloadAds) {
                        preloadAds(layoutInflater, context)
                    }
                    lifecycle.addObserver(object : LifecycleObserver {
                        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
                        fun onDestroy() {
                            AdUtilConstants.nativeAdLifeCycleServiceHashMap.remove(id)
                        }
                    })
                    if (AdUtilConstants.nativeAdLifeCycleServiceHashMap[id] == null ) {
                        AdUtilConstants.nativeAdLifeCycleServiceHashMap[id] = NativeAdItemService(
                            layoutInflater,
                            context,
                            lifecycle,
                            id,
                            adUnit,
                            adName,
                            viewGroup,
                            nativeAdLoadCallback,
                            populator,
                            newAdSize,
                            background,
                            textColor1,
                            textColor2,
                            mediaMaxHeight,
                            loadingTextSize,
                            preloadAds,
                            autoRefresh,
                            contentURL,
                            neighbourContentURL,
                            showLoadingMessage = showLoadingMessage,
                            isAdmanager = isAdmanager
                        )
                        refreshNativeService(adName)
                    }

                } else {
                    if (preloadAds) {
                        preloadAds(layoutInflater, context)
                    }

                    if (primaryIds.size>0){
                        loadNativeAdFromService(
                            layoutInflater,
                            context,
                            lifecycle,
                            adName,
                            viewGroup,
                            nativeAdLoadCallback,
                            background = background,
                            textColor1 = textColor1,
                            textColor2 = textColor2,
                            mediaMaxHeight = mediaMaxHeight,
                            loadingTextSize = loadingTextSize,
                            id = id,
                            populator = populator,
                            adType = newAdSize,
                            preloadAds = preloadAds,
                            autoRefresh = preloadAds,
                            contentURL = contentURL,
                            neighbourContentURL = neighbourContentURL,
                            layoutId,
                            fetchedTimer,
                            primaryIds,
                            object :NativeInternalCallback{
                                override fun onSuccess(nativeAd: NativeAd?) {
                                    nativeAdLoadCallback?.onAdLoaded()
                                    val adView = layoutInflater.inflate(layoutId, null)
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
                                        if (layoutId == R.layout.native_admob_ad_t6) {
                                            when (background) {
                                                is String -> {
                                                    adView.findViewById<LinearLayout>(R.id.main_layout)
                                                        .setBackgroundColor(Color.parseColor(background))
                                                }
                                                is Drawable -> {
                                                    adView.findViewById<LinearLayout>(R.id.main_layout).background =
                                                        background
                                                }
                                                is Int -> {
                                                    adView.findViewById<LinearLayout>(R.id.main_layout)
                                                        .setBackgroundColor(background)
                                                }
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
                                            context.fetchColor(adName),
                                            mediaMaxHeight
                                        )
                                    }
                                    viewGroup.removeAllViews()
                                    viewGroup.addView(adView)
                                    refreshNativeService(adName)
                                }

                                override fun onFailure(loadAdError: LoadAdError?) {
                                    if (secondaryIds.size>0){
                                        loadNativeAdFromService(
                                            layoutInflater,
                                            context,
                                            lifecycle,
                                            adName,
                                            viewGroup,
                                            nativeAdLoadCallback,
                                            background = background,
                                            textColor1 = textColor1,
                                            textColor2 = textColor2,
                                            mediaMaxHeight = mediaMaxHeight,
                                            loadingTextSize = loadingTextSize,
                                            id = id,
                                            populator = populator,
                                            adType = newAdSize,
                                            preloadAds = preloadAds,
                                            autoRefresh = preloadAds,
                                            contentURL = contentURL,
                                            neighbourContentURL = neighbourContentURL,
                                            layoutId,
                                            fetchedTimer,
                                            secondaryIds,
                                            object :NativeInternalCallback{
                                                override fun onSuccess(nativeAd: NativeAd?) {
                                                    nativeAdLoadCallback?.onAdLoaded()
                                                    val adView = layoutInflater.inflate(layoutId, null)
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
                                                        if (layoutId == R.layout.native_admob_ad_t6) {
                                                            when (background) {
                                                                is String -> {
                                                                    adView.findViewById<LinearLayout>(R.id.main_layout)
                                                                        .setBackgroundColor(Color.parseColor(background))
                                                                }
                                                                is Drawable -> {
                                                                    adView.findViewById<LinearLayout>(R.id.main_layout).background =
                                                                        background
                                                                }
                                                                is Int -> {
                                                                    adView.findViewById<LinearLayout>(R.id.main_layout)
                                                                        .setBackgroundColor(background)
                                                                }
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
                                                            context.fetchColor(adName),
                                                            mediaMaxHeight
                                                        )
                                                    }
                                                    viewGroup.removeAllViews()
                                                    viewGroup.addView(adView)
                                                    refreshNativeService(adName)
                                                }

                                                override fun onFailure(loadAdError: LoadAdError?) {
                                                    loadNativeAdFromService(
                                                        layoutInflater,
                                                        context,
                                                        lifecycle,
                                                        adName,
                                                        viewGroup,
                                                        nativeAdLoadCallback,
                                                        background = background,
                                                        textColor1 = textColor1,
                                                        textColor2 = textColor2,
                                                        mediaMaxHeight = mediaMaxHeight,
                                                        loadingTextSize = loadingTextSize,
                                                        id = id,
                                                        populator = populator,
                                                        adType = newAdSize,
                                                        preloadAds = preloadAds,
                                                        autoRefresh = preloadAds,
                                                        contentURL = contentURL,
                                                        neighbourContentURL = neighbourContentURL,
                                                        layoutId,
                                                        fetchedTimer,
                                                        listOf(adUnit),
                                                        object :NativeInternalCallback{
                                                            override fun onSuccess(nativeAd: NativeAd?) {
                                                                nativeAdLoadCallback?.onAdLoaded()
                                                                val adView = layoutInflater.inflate(layoutId, null)
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
                                                                    if (layoutId == R.layout.native_admob_ad_t6) {
                                                                        when (background) {
                                                                            is String -> {
                                                                                adView.findViewById<LinearLayout>(R.id.main_layout)
                                                                                    .setBackgroundColor(Color.parseColor(background))
                                                                            }
                                                                            is Drawable -> {
                                                                                adView.findViewById<LinearLayout>(R.id.main_layout).background =
                                                                                    background
                                                                            }
                                                                            is Int -> {
                                                                                adView.findViewById<LinearLayout>(R.id.main_layout)
                                                                                    .setBackgroundColor(background)
                                                                            }
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
                                                                        context.fetchColor(adName),
                                                                        mediaMaxHeight
                                                                    )
                                                                }
                                                                viewGroup.removeAllViews()
                                                                viewGroup.addView(adView)
                                                                refreshNativeService(adName)
                                                            }

                                                            override fun onFailure(loadAdError: LoadAdError?) {
                                                                nativeAdLoadCallback?.onAdFailed(loadAdError)
                                                            }
                                                        },
                                                        showLoadingMessage,
                                                        isAdmanager
                                                    )
                                                }
                                            },
                                            showLoadingMessage,
                                            isAdmanager
                                        )
                                    }
                                    else{
                                        loadNativeAdFromService(
                                            layoutInflater,
                                            context,
                                            lifecycle,
                                            adName,
                                            viewGroup,
                                            nativeAdLoadCallback,
                                            background = background,
                                            textColor1 = textColor1,
                                            textColor2 = textColor2,
                                            mediaMaxHeight = mediaMaxHeight,
                                            loadingTextSize = loadingTextSize,
                                            id = id,
                                            populator = populator,
                                            adType = newAdSize,
                                            preloadAds = preloadAds,
                                            autoRefresh = preloadAds,
                                            contentURL = contentURL,
                                            neighbourContentURL = neighbourContentURL,
                                            layoutId,
                                            fetchedTimer,
                                            listOf(adUnit),
                                            object :NativeInternalCallback{
                                                override fun onSuccess(nativeAd: NativeAd?) {
                                                    nativeAdLoadCallback?.onAdLoaded()
                                                    val adView = layoutInflater.inflate(layoutId, null)
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
                                                        if (layoutId == R.layout.native_admob_ad_t6) {
                                                            when (background) {
                                                                is String -> {
                                                                    adView.findViewById<LinearLayout>(R.id.main_layout)
                                                                        .setBackgroundColor(Color.parseColor(background))
                                                                }
                                                                is Drawable -> {
                                                                    adView.findViewById<LinearLayout>(R.id.main_layout).background =
                                                                        background
                                                                }
                                                                is Int -> {
                                                                    adView.findViewById<LinearLayout>(R.id.main_layout)
                                                                        .setBackgroundColor(background)
                                                                }
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
                                                            context.fetchColor(adName),
                                                            mediaMaxHeight
                                                        )
                                                    }
                                                    viewGroup.removeAllViews()
                                                    viewGroup.addView(adView)
                                                    refreshNativeService(adName)
                                                }

                                                override fun onFailure(loadAdError: LoadAdError?) {
                                                    nativeAdLoadCallback?.onAdFailed(loadAdError)
                                                }
                                            },
                                            showLoadingMessage,
                                            isAdmanager
                                        )
                                    }
                                }
                            },
                            showLoadingMessage,
                            isAdmanager
                        )
                    }
                    else if (secondaryIds.size>0){
                        loadNativeAdFromService(
                            layoutInflater,
                            context,
                            lifecycle,
                            adName,
                            viewGroup,
                            nativeAdLoadCallback,
                            background = background,
                            textColor1 = textColor1,
                            textColor2 = textColor2,
                            mediaMaxHeight = mediaMaxHeight,
                            loadingTextSize = loadingTextSize,
                            id = id,
                            populator = populator,
                            adType = newAdSize,
                            preloadAds = preloadAds,
                            autoRefresh = preloadAds,
                            contentURL = contentURL,
                            neighbourContentURL = neighbourContentURL,
                            layoutId,
                            fetchedTimer,
                            secondaryIds,
                            object :NativeInternalCallback{
                                override fun onSuccess(nativeAd: NativeAd?) {
                                    nativeAdLoadCallback?.onAdLoaded()
                                    val adView = layoutInflater.inflate(layoutId, null)
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
                                        if (layoutId == R.layout.native_admob_ad_t6) {
                                            when (background) {
                                                is String -> {
                                                    adView.findViewById<LinearLayout>(R.id.main_layout)
                                                        .setBackgroundColor(Color.parseColor(background))
                                                }
                                                is Drawable -> {
                                                    adView.findViewById<LinearLayout>(R.id.main_layout).background =
                                                        background
                                                }
                                                is Int -> {
                                                    adView.findViewById<LinearLayout>(R.id.main_layout)
                                                        .setBackgroundColor(background)
                                                }
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
                                            context.fetchColor(adName),
                                            mediaMaxHeight
                                        )
                                    }
                                    viewGroup.removeAllViews()
                                    viewGroup.addView(adView)
                                    refreshNativeService(adName)
                                }

                                override fun onFailure(loadAdError: LoadAdError?) {
                                    loadNativeAdFromService(
                                        layoutInflater,
                                        context,
                                        lifecycle,
                                        adName,
                                        viewGroup,
                                        nativeAdLoadCallback,
                                        background = background,
                                        textColor1 = textColor1,
                                        textColor2 = textColor2,
                                        mediaMaxHeight = mediaMaxHeight,
                                        loadingTextSize = loadingTextSize,
                                        id = id,
                                        populator = populator,
                                        adType = newAdSize,
                                        preloadAds = preloadAds,
                                        autoRefresh = preloadAds,
                                        contentURL = contentURL,
                                        neighbourContentURL = neighbourContentURL,
                                        layoutId,
                                        fetchedTimer,
                                        listOf(adUnit),
                                        object :NativeInternalCallback{
                                            override fun onSuccess(nativeAd: NativeAd?) {
                                                nativeAdLoadCallback?.onAdLoaded()
                                                val adView = layoutInflater.inflate(layoutId, null)
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
                                                    if (layoutId == R.layout.native_admob_ad_t6) {
                                                        when (background) {
                                                            is String -> {
                                                                adView.findViewById<LinearLayout>(R.id.main_layout)
                                                                    .setBackgroundColor(Color.parseColor(background))
                                                            }
                                                            is Drawable -> {
                                                                adView.findViewById<LinearLayout>(R.id.main_layout).background =
                                                                    background
                                                            }
                                                            is Int -> {
                                                                adView.findViewById<LinearLayout>(R.id.main_layout)
                                                                    .setBackgroundColor(background)
                                                            }
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
                                                        context.fetchColor(adName),
                                                        mediaMaxHeight
                                                    )
                                                }
                                                viewGroup.removeAllViews()
                                                viewGroup.addView(adView)
                                                refreshNativeService(adName)
                                            }

                                            override fun onFailure(loadAdError: LoadAdError?) {
                                                nativeAdLoadCallback?.onAdFailed(loadAdError)
                                            }
                                        },
                                        showLoadingMessage,
                                        isAdmanager
                                    )
                                }
                            },
                            showLoadingMessage,
                            isAdmanager
                        )
                    }
                    else{
                        loadNativeAdFromService(
                            layoutInflater,
                            context,
                            lifecycle,
                            adName,
                            viewGroup,
                            nativeAdLoadCallback,
                            background = background,
                            textColor1 = textColor1,
                            textColor2 = textColor2,
                            mediaMaxHeight = mediaMaxHeight,
                            loadingTextSize = loadingTextSize,
                            id = id,
                            populator = populator,
                            adType = newAdSize,
                            preloadAds = preloadAds,
                            autoRefresh = preloadAds,
                            contentURL = contentURL,
                            neighbourContentURL = neighbourContentURL,
                            layoutId,
                            fetchedTimer,
                            listOf(adUnit),
                            object :NativeInternalCallback{
                                override fun onSuccess(nativeAd: NativeAd?) {
                                    nativeAdLoadCallback?.onAdLoaded()
                                    val adView = layoutInflater.inflate(layoutId, null)
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
                                        if (layoutId == R.layout.native_admob_ad_t6) {
                                            when (background) {
                                                is String -> {
                                                    adView.findViewById<LinearLayout>(R.id.main_layout)
                                                        .setBackgroundColor(Color.parseColor(background))
                                                }
                                                is Drawable -> {
                                                    adView.findViewById<LinearLayout>(R.id.main_layout).background =
                                                        background
                                                }
                                                is Int -> {
                                                    adView.findViewById<LinearLayout>(R.id.main_layout)
                                                        .setBackgroundColor(background)
                                                }
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
                                            context.fetchColor(adName),
                                            mediaMaxHeight
                                        )
                                    }
                                    viewGroup.removeAllViews()
                                    viewGroup.addView(adView)
                                    refreshNativeService(adName)
                                }

                                override fun onFailure(loadAdError: LoadAdError?) {
                                    nativeAdLoadCallback?.onAdFailed(loadAdError)
                                }
                            },
                            showLoadingMessage,
                            isAdmanager
                        )
                    }
                    /*The Extra Parameters are just for logging*/
                }
            } else {
                if (preloadAds) {
                    preloadAds(layoutInflater, context)
                }
                if (primaryIds.size>0){
                    loadNativeAdFromService(
                        layoutInflater,
                        context,
                        lifecycle,
                        adName,
                        viewGroup,
                        nativeAdLoadCallback,
                        background = background,
                        textColor1 = textColor1,
                        textColor2 = textColor2,
                        mediaMaxHeight = mediaMaxHeight,
                        loadingTextSize = loadingTextSize,
                        id = id,
                        populator = populator,
                        adType = newAdSize,
                        preloadAds = preloadAds,
                        autoRefresh = preloadAds,
                        contentURL = contentURL,
                        neighbourContentURL = neighbourContentURL,
                        layoutId,
                        fetchedTimer,
                        primaryIds,
                        object :NativeInternalCallback{
                            override fun onSuccess(nativeAd: NativeAd?) {
                                nativeAdLoadCallback?.onAdLoaded()
                                val adView = layoutInflater.inflate(layoutId, null)
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
                                    if (layoutId == R.layout.native_admob_ad_t6) {
                                        when (background) {
                                            is String -> {
                                                adView.findViewById<LinearLayout>(R.id.main_layout)
                                                    .setBackgroundColor(Color.parseColor(background))
                                            }
                                            is Drawable -> {
                                                adView.findViewById<LinearLayout>(R.id.main_layout).background =
                                                    background
                                            }
                                            is Int -> {
                                                adView.findViewById<LinearLayout>(R.id.main_layout)
                                                    .setBackgroundColor(background)
                                            }
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
                                        context.fetchColor(adName),
                                        mediaMaxHeight
                                    )
                                }
                                viewGroup.removeAllViews()
                                viewGroup.addView(adView)
                                refreshNativeService(adName)
                            }

                            override fun onFailure(loadAdError: LoadAdError?) {
                                if (secondaryIds.size>0){
                                    loadNativeAdFromService(
                                        layoutInflater,
                                        context,
                                        lifecycle,
                                        adName,
                                        viewGroup,
                                        nativeAdLoadCallback,
                                        background = background,
                                        textColor1 = textColor1,
                                        textColor2 = textColor2,
                                        mediaMaxHeight = mediaMaxHeight,
                                        loadingTextSize = loadingTextSize,
                                        id = id,
                                        populator = populator,
                                        adType = newAdSize,
                                        preloadAds = preloadAds,
                                        autoRefresh = preloadAds,
                                        contentURL = contentURL,
                                        neighbourContentURL = neighbourContentURL,
                                        layoutId,
                                        fetchedTimer,
                                        secondaryIds,
                                        object :NativeInternalCallback{
                                            override fun onSuccess(nativeAd: NativeAd?) {
                                                nativeAdLoadCallback?.onAdLoaded()
                                                val adView = layoutInflater.inflate(layoutId, null)
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
                                                    if (layoutId == R.layout.native_admob_ad_t6) {
                                                        when (background) {
                                                            is String -> {
                                                                adView.findViewById<LinearLayout>(R.id.main_layout)
                                                                    .setBackgroundColor(Color.parseColor(background))
                                                            }
                                                            is Drawable -> {
                                                                adView.findViewById<LinearLayout>(R.id.main_layout).background =
                                                                    background
                                                            }
                                                            is Int -> {
                                                                adView.findViewById<LinearLayout>(R.id.main_layout)
                                                                    .setBackgroundColor(background)
                                                            }
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
                                                        context.fetchColor(adName),
                                                        mediaMaxHeight
                                                    )
                                                }
                                                viewGroup.removeAllViews()
                                                viewGroup.addView(adView)
                                                refreshNativeService(adName)
                                            }

                                            override fun onFailure(loadAdError: LoadAdError?) {
                                                loadNativeAdFromService(
                                                    layoutInflater,
                                                    context,
                                                    lifecycle,
                                                    adName,
                                                    viewGroup,
                                                    nativeAdLoadCallback,
                                                    background = background,
                                                    textColor1 = textColor1,
                                                    textColor2 = textColor2,
                                                    mediaMaxHeight = mediaMaxHeight,
                                                    loadingTextSize = loadingTextSize,
                                                    id = id,
                                                    populator = populator,
                                                    adType = newAdSize,
                                                    preloadAds = preloadAds,
                                                    autoRefresh = preloadAds,
                                                    contentURL = contentURL,
                                                    neighbourContentURL = neighbourContentURL,
                                                    layoutId,
                                                    fetchedTimer,
                                                    listOf(adUnit),
                                                    object :NativeInternalCallback{
                                                        override fun onSuccess(nativeAd: NativeAd?) {
                                                            nativeAdLoadCallback?.onAdLoaded()
                                                            val adView = layoutInflater.inflate(layoutId, null)
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
                                                                if (layoutId == R.layout.native_admob_ad_t6) {
                                                                    when (background) {
                                                                        is String -> {
                                                                            adView.findViewById<LinearLayout>(R.id.main_layout)
                                                                                .setBackgroundColor(Color.parseColor(background))
                                                                        }
                                                                        is Drawable -> {
                                                                            adView.findViewById<LinearLayout>(R.id.main_layout).background =
                                                                                background
                                                                        }
                                                                        is Int -> {
                                                                            adView.findViewById<LinearLayout>(R.id.main_layout)
                                                                                .setBackgroundColor(background)
                                                                        }
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
                                                                    context.fetchColor(adName),
                                                                    mediaMaxHeight
                                                                )
                                                            }
                                                            viewGroup.removeAllViews()
                                                            viewGroup.addView(adView)
                                                            refreshNativeService(adName)
                                                        }

                                                        override fun onFailure(loadAdError: LoadAdError?) {
                                                            nativeAdLoadCallback?.onAdFailed(loadAdError)
                                                        }
                                                    },
                                                    showLoadingMessage,
                                                    isAdmanager
                                                )
                                            }
                                        },
                                        showLoadingMessage,
                                        isAdmanager
                                    )
                                }
                                else{
                                    loadNativeAdFromService(
                                        layoutInflater,
                                        context,
                                        lifecycle,
                                        adName,
                                        viewGroup,
                                        nativeAdLoadCallback,
                                        background = background,
                                        textColor1 = textColor1,
                                        textColor2 = textColor2,
                                        mediaMaxHeight = mediaMaxHeight,
                                        loadingTextSize = loadingTextSize,
                                        id = id,
                                        populator = populator,
                                        adType = newAdSize,
                                        preloadAds = preloadAds,
                                        autoRefresh = preloadAds,
                                        contentURL = contentURL,
                                        neighbourContentURL = neighbourContentURL,
                                        layoutId,
                                        fetchedTimer,
                                        listOf(adUnit),
                                        object :NativeInternalCallback{
                                            override fun onSuccess(nativeAd: NativeAd?) {
                                                nativeAdLoadCallback?.onAdLoaded()
                                                val adView = layoutInflater.inflate(layoutId, null)
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
                                                    if (layoutId == R.layout.native_admob_ad_t6) {
                                                        when (background) {
                                                            is String -> {
                                                                adView.findViewById<LinearLayout>(R.id.main_layout)
                                                                    .setBackgroundColor(Color.parseColor(background))
                                                            }
                                                            is Drawable -> {
                                                                adView.findViewById<LinearLayout>(R.id.main_layout).background =
                                                                    background
                                                            }
                                                            is Int -> {
                                                                adView.findViewById<LinearLayout>(R.id.main_layout)
                                                                    .setBackgroundColor(background)
                                                            }
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
                                                        context.fetchColor(adName),
                                                        mediaMaxHeight
                                                    )
                                                }
                                                viewGroup.removeAllViews()
                                                viewGroup.addView(adView)
                                                refreshNativeService(adName)
                                            }

                                            override fun onFailure(loadAdError: LoadAdError?) {
                                                nativeAdLoadCallback?.onAdFailed(loadAdError)
                                            }
                                        },
                                        showLoadingMessage,
                                        isAdmanager
                                    )
                                }
                            }
                        },
                        showLoadingMessage,
                        isAdmanager
                    )
                }
                else if (secondaryIds.size>0){
                    loadNativeAdFromService(
                        layoutInflater,
                        context,
                        lifecycle,
                        adName,
                        viewGroup,
                        nativeAdLoadCallback,
                        background = background,
                        textColor1 = textColor1,
                        textColor2 = textColor2,
                        mediaMaxHeight = mediaMaxHeight,
                        loadingTextSize = loadingTextSize,
                        id = id,
                        populator = populator,
                        adType = newAdSize,
                        preloadAds = preloadAds,
                        autoRefresh = preloadAds,
                        contentURL = contentURL,
                        neighbourContentURL = neighbourContentURL,
                        layoutId,
                        fetchedTimer,
                        secondaryIds,
                        object :NativeInternalCallback{
                            override fun onSuccess(nativeAd: NativeAd?) {
                                nativeAdLoadCallback?.onAdLoaded()
                                val adView = layoutInflater.inflate(layoutId, null)
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
                                    if (layoutId == R.layout.native_admob_ad_t6) {
                                        when (background) {
                                            is String -> {
                                                adView.findViewById<LinearLayout>(R.id.main_layout)
                                                    .setBackgroundColor(Color.parseColor(background))
                                            }
                                            is Drawable -> {
                                                adView.findViewById<LinearLayout>(R.id.main_layout).background =
                                                    background
                                            }
                                            is Int -> {
                                                adView.findViewById<LinearLayout>(R.id.main_layout)
                                                    .setBackgroundColor(background)
                                            }
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
                                        context.fetchColor(adName),
                                        mediaMaxHeight
                                    )
                                }
                                viewGroup.removeAllViews()
                                viewGroup.addView(adView)
                                refreshNativeService(adName)
                            }

                            override fun onFailure(loadAdError: LoadAdError?) {
                                loadNativeAdFromService(
                                    layoutInflater,
                                    context,
                                    lifecycle,
                                    adName,
                                    viewGroup,
                                    nativeAdLoadCallback,
                                    background = background,
                                    textColor1 = textColor1,
                                    textColor2 = textColor2,
                                    mediaMaxHeight = mediaMaxHeight,
                                    loadingTextSize = loadingTextSize,
                                    id = id,
                                    populator = populator,
                                    adType = newAdSize,
                                    preloadAds = preloadAds,
                                    autoRefresh = preloadAds,
                                    contentURL = contentURL,
                                    neighbourContentURL = neighbourContentURL,
                                    layoutId,
                                    fetchedTimer,
                                    listOf(adUnit),
                                    object :NativeInternalCallback{
                                        override fun onSuccess(nativeAd: NativeAd?) {
                                            nativeAdLoadCallback?.onAdLoaded()
                                            val adView = layoutInflater.inflate(layoutId, null)
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
                                                if (layoutId == R.layout.native_admob_ad_t6) {
                                                    when (background) {
                                                        is String -> {
                                                            adView.findViewById<LinearLayout>(R.id.main_layout)
                                                                .setBackgroundColor(Color.parseColor(background))
                                                        }
                                                        is Drawable -> {
                                                            adView.findViewById<LinearLayout>(R.id.main_layout).background =
                                                                background
                                                        }
                                                        is Int -> {
                                                            adView.findViewById<LinearLayout>(R.id.main_layout)
                                                                .setBackgroundColor(background)
                                                        }
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
                                                    context.fetchColor(adName),
                                                    mediaMaxHeight
                                                )
                                            }
                                            viewGroup.removeAllViews()
                                            viewGroup.addView(adView)
                                            refreshNativeService(adName)
                                        }

                                        override fun onFailure(loadAdError: LoadAdError?) {
                                            nativeAdLoadCallback?.onAdFailed(loadAdError)
                                        }
                                    },
                                    showLoadingMessage,
                                    isAdmanager
                                )
                            }
                        },
                        showLoadingMessage,
                        isAdmanager
                    )
                }
                else{
                    loadNativeAdFromService(
                        layoutInflater,
                        context,
                        lifecycle,
                        adName,
                        viewGroup,
                        nativeAdLoadCallback,
                        background = background,
                        textColor1 = textColor1,
                        textColor2 = textColor2,
                        mediaMaxHeight = mediaMaxHeight,
                        loadingTextSize = loadingTextSize,
                        id = id,
                        populator = populator,
                        adType = newAdSize,
                        preloadAds = preloadAds,
                        autoRefresh = preloadAds,
                        contentURL = contentURL,
                        neighbourContentURL = neighbourContentURL,
                        layoutId,
                        fetchedTimer,
                        listOf(adUnit),
                        object :NativeInternalCallback{
                            override fun onSuccess(nativeAd: NativeAd?) {
                                nativeAdLoadCallback?.onAdLoaded()
                                val adView = layoutInflater.inflate(layoutId, null)
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
                                    if (layoutId == R.layout.native_admob_ad_t6) {
                                        when (background) {
                                            is String -> {
                                                adView.findViewById<LinearLayout>(R.id.main_layout)
                                                    .setBackgroundColor(Color.parseColor(background))
                                            }
                                            is Drawable -> {
                                                adView.findViewById<LinearLayout>(R.id.main_layout).background =
                                                    background
                                            }
                                            is Int -> {
                                                adView.findViewById<LinearLayout>(R.id.main_layout)
                                                    .setBackgroundColor(background)
                                            }
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
                                        context.fetchColor(adName),
                                        mediaMaxHeight
                                    )
                                }
                                viewGroup.removeAllViews()
                                viewGroup.addView(adView)
                                refreshNativeService(adName)
                            }

                            override fun onFailure(loadAdError: LoadAdError?) {
                                nativeAdLoadCallback?.onAdFailed(loadAdError)
                            }
                        },
                        showLoadingMessage,
                        isAdmanager
                    )
                }
            }
        } else {
            viewGroup.visibility = GONE
        }
    }

    internal fun loadNativeAdFromService(
        layoutInflater: LayoutInflater,
        context: Context,
        lifecycle: Lifecycle,
        adName: String,
        viewGroup: ViewGroup,
        nativeAdLoadCallback: NativeAdLoadCallback?,
        background: Any?,
        textColor1: Int?,
        textColor2: Int?,
        mediaMaxHeight: Int,
        loadingTextSize: Int,
        id: Long,
        populator: ((nativeAd: NativeAd, adView: NativeAdView) -> Unit)?,
        adType: String,
        preloadAds: Boolean,
        autoRefresh: Boolean,
        contentURL: String?,
        neighbourContentURL: List<String>?,
        layoutId: Int,
        fetchedTimer: Int,
        primaryIds: List<String>,
        nativeInternalCallback: NativeInternalCallback,
        showLoadingMessage: Boolean,
        isAdmanager: Boolean
    ) {
        var nativeAd: NativeAd? = null
        var loadAdError: LoadAdError? = null
        object : CountDownTimer(fetchedTimer.toLong(), 500) {
            override fun onTick(millisUntilFinished: Long) {
                if (nativeAd != null) {
                    nativeInternalCallback.onSuccess(nativeAd)
                    this.cancel()
                }
            }

            override fun onFinish() {
                if (nativeAd != null) {
                    nativeInternalCallback.onSuccess(nativeAd)
                }
                else
                    nativeInternalCallback.onFailure(loadAdError)
            }
        }.start()
        var loadedUnit = ""
        for (adUnit in primaryIds){

            val adLoader: AdLoader = AdLoader.Builder(context, adUnit)
                .forNativeAd { ad: NativeAd ->
                    if (nativeAd == null) {
                        nativeAd = ad
                        if (loadedUnit.equals(""))
                            loadedUnit = adUnit
                    }

                }
                .withAdListener(object : AdListener() {

                    override fun onAdClicked() {
                        super.onAdClicked()
                        nativeAdLoadCallback?.onAdClicked()
                    }

                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        if (nativeAd == null)
                            nativeAd = null
                        viewGroup.removeAllViews()
                        loadAdError = adError
                        Log.d(TAG, "onAdFailedToLoad: "+adName+" : "+adUnit+" : "+adError.message)

                    }

                    override fun onAdLoaded() {
                        super.onAdLoaded()
                        if (AdUtilConstants.nativeAdLifeCycleServiceHashMap[id] == null && nativeAd != null) {
                            AdUtilConstants.nativeAdLifeCycleServiceHashMap[id] = NativeAdItemService(
                                layoutInflater,
                                context,
                                lifecycle,
                                id,
                                if (!loadedUnit.equals("")) loadedUnit else adUnit,
                                adName,
                                viewGroup,
                                nativeAdLoadCallback,
                                populator,
                                adType,
                                background,
                                textColor1,
                                textColor2,
                                mediaMaxHeight,
                                loadingTextSize,
                                preloadAds,
                                autoRefresh,
                                contentURL,
                                neighbourContentURL,
                                showLoadingMessage = showLoadingMessage,
                                isAdmanager = isAdmanager
                            )
                        }
                    }
                })
                .withNativeAdOptions(
                    NativeAdOptions.Builder()
                        .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                        .setRequestCustomMuteThisAd(true)
                        .build()
                ).build()

            loadAd(
                adLoader,
                contentURL,
                neighbourContentURL,
                isAdmanager
            )
        }
    }



    internal fun preLoadNativeAd(
        layoutInflater: LayoutInflater,
        context: Context,
        adUnit: String,
        adName: String,
        adType: String = "1",
        mediaMaxHeight: Int = 300,
        loadingTextSize: Int,
        background: Any?,
        textColor1: Int?,
        textColor2: Int?,
        populator: ((nativeAd: NativeAd, adView: NativeAdView) -> Unit)? = null,
        contentURL: String? = null,
        neighbourContentURL: List<String>? = null,
        isAdmanager: Boolean,
        loadTimeOut:Int
    ) {
        Log.d("preLoadNativeAd: ", adUnit+"-"+adName)
        if (context != null){
            if (adUnit != "STOP" && AppPref.getBoolean(context,AppPref.showAppAds) && context.fetchAdStatusFromAdId(adName)) {
                var mediaMaxHeight1 = mediaMaxHeight
                var newAdSize = context.fetchAdSize(adName,adType)
                @LayoutRes val layoutId = when (newAdSize) {
                    "6" -> {
                        mediaMaxHeight1 = 200
                        R.layout.native_admob_ad_t6
                    }/*DEFAULT_AD*/
                    "3" -> R.layout.native_admob_ad_t3/*SMALL*/
                    "4" -> R.layout.native_admob_ad_t4/*MEDIUM*/
                    "1" -> R.layout.native_admob_ad_t1/*BIGV1*/
                    "5" -> R.layout.native_admob_ad_t5/*BIGV2*/
                    "2" -> R.layout.native_admob_ad_t2/*BIGV3*/
                    "7" -> R.layout.native_admob_ad_t7/*GRID_AD*/
                    else -> R.layout.native_admob_ad_t1
                }
                if (adUnit.isBlank()) return

                var fetchedTimer:Int = context.fetchAdLoadTimeout(adName)
                if (fetchedTimer == 0){
                    fetchedTimer = loadTimeOut
                }
                var primaryIds = context.fetchPrimaryById(adName)
                var secondaryIds = context.fetchSecondaryById(adName)


                val preloadNativeAds = preloadNativeAdList?.get(adName)

                val inflate = View.inflate(context, R.layout.shimmer, null)
                when (newAdSize) {
                    "6" -> {
                        inflate.findViewById<LinearLayout>(R.id.shim_default).visibility = View.VISIBLE
                    }/*DEFAULT_AD*/
                    "3" -> {
                        inflate.findViewById<LinearLayout>(R.id.shim_small).visibility = View.VISIBLE
                    }
                    /*SMALL*/
                    "4" -> {
                        inflate.findViewById<LinearLayout>(R.id.shim_medium).visibility = View.VISIBLE
                    }
                    /*MEDIUM*/
                    "1" -> {
                        inflate.findViewById<LinearLayout>(R.id.shim_bigv1).visibility = View.VISIBLE
                    }
                    /*BIGV1*/
                    "5" -> {
                        inflate.findViewById<LinearLayout>(R.id.shim_bigv2).visibility = View.VISIBLE
                    }
                    /*BIGV2*/
                    "2" -> {
                        inflate.findViewById<LinearLayout>(R.id.shim_bigv3).visibility = View.VISIBLE
                    }
                    /*BIGV3*/
                    "7" -> {
                        inflate.findViewById<LinearLayout>(R.id.shim_gridad).visibility = View.VISIBLE
                    }
                    /*GRID_AD*/
                    else -> {
                        inflate.findViewById<LinearLayout>(R.id.shim_default).visibility = View.VISIBLE
                    }

                }

                Log.d("preload_native", "onStart" + System.currentTimeMillis()/1000)
                if (primaryIds.size>0){
                    preLoadNativeAd(
                        adName,
                        context,
                        contentURL = contentURL,
                        neighbourContentURL = neighbourContentURL,
                        primaryIds,
                        fetchedTimer,
                        object : NativeInternalCallback {
                            override fun onSuccess(nativeAd: NativeAd?) {
                                Log.d("preload_native", "onSuccess: First Shown" + System.currentTimeMillis()/1000)
                                if (nativeAd != null) {
                                    val adView = layoutInflater.inflate(layoutId, null)
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
                                        if (layoutId == R.layout.native_admob_ad_t6){
                                            when (background) {
                                                is String -> {
                                                    adView.findViewById<LinearLayout>(R.id.main_layout).setBackgroundColor(Color.parseColor(background))
                                                }
                                                is Drawable -> {
                                                    adView.findViewById<LinearLayout>(R.id.main_layout).background = background
                                                }
                                                is Int -> {
                                                    adView.findViewById<LinearLayout>(R.id.main_layout).setBackgroundColor(background)
                                                }
                                            }
                                        }
                                    }
                                    if (populator != null) {
                                        populator.invoke(nativeAd!!, adView)
                                    } else {
                                        populateUnifiedNativeAdView(
                                            nativeAd!!,
                                            adView,
                                            newAdSize,
                                            textColor1,
                                            textColor2,
                                            context.fetchColor(adName),
                                            mediaMaxHeight1
                                        )
                                        Log.d("preloadColor", "onSuccess: "+ context.fetchColor(adName))
                                    }
                                    if (preloadNativeAds != null) {
                                        preloadNativeAds.ad = adView
                                    }
                                }
                            }

                            override fun onFailure(loadAdError: LoadAdError?) {
                                if (secondaryIds.size > 0){
                                    preLoadNativeAd(
                                        adName,
                                        context,
                                        contentURL = contentURL,
                                        neighbourContentURL = neighbourContentURL,
                                        secondaryIds,
                                        fetchedTimer,
                                        object : NativeInternalCallback {
                                            override fun onSuccess(nativeAd: NativeAd?) {
                                                Log.d("preload_native", "onSuccess: First Secondary Shown" + System.currentTimeMillis()/1000)
                                                if (nativeAd != null) {
                                                    val adView = layoutInflater.inflate(layoutId, null)
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
                                                        if (layoutId == R.layout.native_admob_ad_t6){
                                                            when (background) {
                                                                is String -> {
                                                                    adView.findViewById<LinearLayout>(R.id.main_layout).setBackgroundColor(Color.parseColor(background))
                                                                }
                                                                is Drawable -> {
                                                                    adView.findViewById<LinearLayout>(R.id.main_layout).background = background
                                                                }
                                                                is Int -> {
                                                                    adView.findViewById<LinearLayout>(R.id.main_layout).setBackgroundColor(background)
                                                                }
                                                            }
                                                        }
                                                    }
                                                    if (populator != null) {
                                                        populator.invoke(nativeAd!!, adView)
                                                    } else {
                                                        populateUnifiedNativeAdView(
                                                            nativeAd!!,
                                                            adView,
                                                            newAdSize,
                                                            textColor1,
                                                            textColor2,
                                                            context.fetchColor(adName),
                                                            mediaMaxHeight1
                                                        )
                                                        Log.d("preloadColor", "onSuccess: "+ context.fetchColor(adName))
                                                    }
                                                    if (preloadNativeAds != null) {
                                                        preloadNativeAds.ad = adView
                                                    }
                                                }
                                            }

                                            override fun onFailure(loadAdError: LoadAdError?) {
                                                preLoadNativeAd(
                                                    adName,
                                                    context,
                                                    contentURL = contentURL,
                                                    neighbourContentURL = neighbourContentURL,
                                                    listOf(adUnit),
                                                    fetchedTimer,
                                                    object : NativeInternalCallback {
                                                        override fun onSuccess(nativeAd: NativeAd?) {
                                                            Log.d("preload_native", "onSuccess: First else Fallback Shown" + System.currentTimeMillis()/1000)
                                                            if (nativeAd != null) {
                                                                val adView = layoutInflater.inflate(layoutId, null)
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
                                                                    if (layoutId == R.layout.native_admob_ad_t6){
                                                                        when (background) {
                                                                            is String -> {
                                                                                adView.findViewById<LinearLayout>(R.id.main_layout).setBackgroundColor(Color.parseColor(background))
                                                                            }
                                                                            is Drawable -> {
                                                                                adView.findViewById<LinearLayout>(R.id.main_layout).background = background
                                                                            }
                                                                            is Int -> {
                                                                                adView.findViewById<LinearLayout>(R.id.main_layout).setBackgroundColor(background)
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                                if (populator != null) {
                                                                    populator.invoke(nativeAd!!, adView)
                                                                } else {
                                                                    populateUnifiedNativeAdView(
                                                                        nativeAd!!,
                                                                        adView,
                                                                        newAdSize,
                                                                        textColor1,
                                                                        textColor2,
                                                                        context.fetchColor(adName),
                                                                        mediaMaxHeight1
                                                                    )
                                                                    Log.d("preloadColor", "onSuccess: "+ context.fetchColor(adName))
                                                                }
                                                                if (preloadNativeAds != null) {
                                                                    preloadNativeAds.ad = adView
                                                                }
                                                            }
                                                        }

                                                        override fun onFailure(loadAdError: LoadAdError?) {

                                                        }
                                                    },
                                                    isAdmanager = isAdmanager
                                                )
                                            }
                                        },
                                        isAdmanager = isAdmanager
                                    )
                                }
                                else{
                                    preLoadNativeAd(
                                        adName,
                                        context,
                                        contentURL = contentURL,
                                        neighbourContentURL = neighbourContentURL,
                                        listOf(adUnit),
                                        fetchedTimer,
                                        object : NativeInternalCallback {
                                            override fun onSuccess(nativeAd: NativeAd?) {
                                                Log.d("preload_native", "onSuccess: primary else Fallback Shown" + System.currentTimeMillis()/1000)
                                                if (nativeAd != null) {
                                                    val adView = layoutInflater.inflate(layoutId, null)
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
                                                        if (layoutId == R.layout.native_admob_ad_t6){
                                                            when (background) {
                                                                is String -> {
                                                                    adView.findViewById<LinearLayout>(R.id.main_layout).setBackgroundColor(Color.parseColor(background))
                                                                }
                                                                is Drawable -> {
                                                                    adView.findViewById<LinearLayout>(R.id.main_layout).background = background
                                                                }
                                                                is Int -> {
                                                                    adView.findViewById<LinearLayout>(R.id.main_layout).setBackgroundColor(background)
                                                                }
                                                            }
                                                        }
                                                    }
                                                    if (populator != null) {
                                                        populator.invoke(nativeAd!!, adView)
                                                    } else {
                                                        populateUnifiedNativeAdView(
                                                            nativeAd!!,
                                                            adView,
                                                            newAdSize,
                                                            textColor1,
                                                            textColor2,
                                                            context.fetchColor(adName),
                                                            mediaMaxHeight1
                                                        )
                                                        Log.d("preloadColor", "onSuccess: "+ context.fetchColor(adName))
                                                    }
                                                    if (preloadNativeAds != null) {
                                                        preloadNativeAds.ad = adView
                                                    }
                                                }
                                            }

                                            override fun onFailure(loadAdError: LoadAdError?) {

                                            }
                                        },
                                        isAdmanager = isAdmanager
                                    )
                                }
                            }
                        },
                        isAdmanager = isAdmanager
                    )
                }
                else if (secondaryIds.size > 0){
                    preLoadNativeAd(
                        adName,
                        context,
                        contentURL = contentURL,
                        neighbourContentURL = neighbourContentURL,
                        secondaryIds,
                        fetchedTimer,
                        object : NativeInternalCallback {
                            override fun onSuccess(nativeAd: NativeAd?) {
                                Log.d("preload_native", "onSuccess: Second Shown" + System.currentTimeMillis()/1000)
                                if (nativeAd != null) {
                                    val adView = layoutInflater.inflate(layoutId, null)
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
                                        if (layoutId == R.layout.native_admob_ad_t6){
                                            when (background) {
                                                is String -> {
                                                    adView.findViewById<LinearLayout>(R.id.main_layout).setBackgroundColor(Color.parseColor(background))
                                                }
                                                is Drawable -> {
                                                    adView.findViewById<LinearLayout>(R.id.main_layout).background = background
                                                }
                                                is Int -> {
                                                    adView.findViewById<LinearLayout>(R.id.main_layout).setBackgroundColor(background)
                                                }
                                            }
                                        }
                                    }
                                    if (populator != null) {
                                        populator.invoke(nativeAd!!, adView)
                                    } else {
                                        populateUnifiedNativeAdView(
                                            nativeAd!!,
                                            adView,
                                            newAdSize,
                                            textColor1,
                                            textColor2,
                                            context.fetchColor(adName),
                                            mediaMaxHeight1
                                        )
                                        Log.d("preloadColor", "onSuccess: "+ context.fetchColor(adName))
                                    }
                                    if (preloadNativeAds != null) {
                                        preloadNativeAds.ad = adView
                                    }
                                }
                            }

                            override fun onFailure(loadAdError: LoadAdError?) {
                                preLoadNativeAd(
                                    adName,
                                    context,
                                    contentURL = contentURL,
                                    neighbourContentURL = neighbourContentURL,
                                    listOf(adUnit),
                                    fetchedTimer,
                                    object : NativeInternalCallback {
                                        override fun onSuccess(nativeAd: NativeAd?) {
                                            Log.d("preload_native", "onSuccess: Second Fallback Shown" + System.currentTimeMillis()/1000)
                                            if (nativeAd != null) {
                                                val adView = layoutInflater.inflate(layoutId, null)
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
                                                    if (layoutId == R.layout.native_admob_ad_t6){
                                                        when (background) {
                                                            is String -> {
                                                                adView.findViewById<LinearLayout>(R.id.main_layout).setBackgroundColor(Color.parseColor(background))
                                                            }
                                                            is Drawable -> {
                                                                adView.findViewById<LinearLayout>(R.id.main_layout).background = background
                                                            }
                                                            is Int -> {
                                                                adView.findViewById<LinearLayout>(R.id.main_layout).setBackgroundColor(background)
                                                            }
                                                        }
                                                    }
                                                }
                                                if (populator != null) {
                                                    populator.invoke(nativeAd!!, adView)
                                                } else {
                                                    populateUnifiedNativeAdView(
                                                        nativeAd!!,
                                                        adView,
                                                        newAdSize,
                                                        textColor1,
                                                        textColor2,
                                                        context.fetchColor(adName),
                                                        mediaMaxHeight1
                                                    )
                                                    Log.d("preloadColor", "onSuccess: "+ context.fetchColor(adName))
                                                }
                                                if (preloadNativeAds != null) {
                                                    preloadNativeAds.ad = adView
                                                }
                                            }
                                        }

                                        override fun onFailure(loadAdError: LoadAdError?) {

                                        }
                                    },
                                    isAdmanager = isAdmanager
                                )
                            }
                        },
                        isAdmanager = isAdmanager
                    )
                }
                else{
                    preLoadNativeAd(
                        adName,
                        context,
                        contentURL = contentURL,
                        neighbourContentURL = neighbourContentURL,
                        listOf(adUnit),
                        fetchedTimer,
                        object : NativeInternalCallback {
                            override fun onSuccess(nativeAd: NativeAd?) {
                                Log.d("preload_native", "onSuccess: else Fallback Shown" + System.currentTimeMillis()/1000)
                                if (nativeAd != null) {
                                    val adView = layoutInflater.inflate(layoutId, null)
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
                                        if (layoutId == R.layout.native_admob_ad_t6){
                                            when (background) {
                                                is String -> {
                                                    adView.findViewById<LinearLayout>(R.id.main_layout).setBackgroundColor(Color.parseColor(background))
                                                }
                                                is Drawable -> {
                                                    adView.findViewById<LinearLayout>(R.id.main_layout).background = background
                                                }
                                                is Int -> {
                                                    adView.findViewById<LinearLayout>(R.id.main_layout).setBackgroundColor(background)
                                                }
                                            }
                                        }
                                    }
                                    if (populator != null) {
                                        populator.invoke(nativeAd!!, adView)
                                    } else {
                                        populateUnifiedNativeAdView(
                                            nativeAd!!,
                                            adView,
                                            newAdSize,
                                            textColor1,
                                            textColor2,
                                            context.fetchColor(adName),
                                            mediaMaxHeight1
                                        )
                                        Log.d("preloadColor", "onSuccess: "+ context.fetchColor(adName))
                                    }
                                    if (preloadNativeAds != null) {
                                        preloadNativeAds.ad = adView
                                    }
                                }
                            }

                            override fun onFailure(loadAdError: LoadAdError?) {

                            }
                        },
                        isAdmanager = isAdmanager
                    )
                }
            }
        }
        else {
            Log.d("preLoadNativeAd:contextNull", adUnit+"-"+adName)
        }
    }

    internal fun preLoadNativeAd(
        adName: String,
        context: Context,
        contentURL: String? = null,
        neighbourContentURL: List<String>? = null,
        primaryIds: List<String>,
        fetchedTimer: Int,
        nativeInternalCallback: NativeInternalCallback,
        isAdmanager: Boolean
    ) {
        var nativeAd: NativeAd? = null
        var loadAdError: LoadAdError? = null
        object : CountDownTimer(fetchedTimer.toLong(), 500) {
            override fun onTick(millisUntilFinished: Long) {
                if (nativeAd != null) {
                    nativeInternalCallback.onSuccess(nativeAd)
                    this.cancel()
                }
            }

            override fun onFinish() {
                if (nativeAd != null) {
                    nativeInternalCallback.onSuccess(nativeAd)
                }
                else
                    nativeInternalCallback.onFailure(loadAdError)
            }
        }.start()
        var loadedId = ""
        for (adUnit in primaryIds){
            val adLoader: AdLoader? = AdLoader.Builder(context, adUnit)
                .forNativeAd { ad: NativeAd ->
                    if (nativeAd == null && ad != null) {
                        nativeAd = ad
                        loadedId = adUnit
                    }
                }
                .withAdListener(object : AdListener() {

                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        loadAdError = adError
                        Log.d(TAG, "onAdFailedToLoad: "+adName+" : "+adUnit+" : "+adError.message)

                    }

                    override fun onAdLoaded() {
                        super.onAdLoaded()
                    }
                })
                .withNativeAdOptions(
                    NativeAdOptions.Builder()
                        .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                        .setRequestCustomMuteThisAd(true)
                        .build()
                )
                .build()
            loadAd(adLoader, contentURL, neighbourContentURL,isAdmanager)
        }

    }

    internal fun loadAd(
        adLoader: AdLoader?,
        contentURL: String?,
        neighbourContentURL: List<String>?,
        isAdmanager: Boolean
    ) {
        if (!isAdmanager){
            val builder = AdRequest.Builder().addNetworkExtrasBundle(
                AdMobAdapter::class.java,
                getConsentEnabledBundle()
            )
            contentURL?.let { builder.setContentUrl(it) }
            neighbourContentURL?.let { builder.setNeighboringContentUrls(it) }
            adLoader?.loadAd(
                builder.build()
            )
        }
        else {
            val builder = AdManagerAdRequest.Builder()
            contentURL?.let { builder.setContentUrl(it) }
            neighbourContentURL?.let { builder.setNeighboringContentUrls(it) }
            adLoader?.loadAd(
                builder.build()
            )
            Log.d("loadAd", "loadAd: admanager  "+ System.currentTimeMillis()/1000)
        }

    }

    fun populateUnifiedNativeAdView(
        nativeAd: NativeAd,
        adView: NativeAdView?,
        adType: String,
        textColor1: Int?,
        textColor2: Int?,
        buttonColor: String?,
        mediaMaxHeight: Int = 300,
    ) {
        val iconView = adView?.findViewById(R.id.icon) as ImageView
        Log.e("$TAG: nativead", "ad body : " + nativeAd.body)
        val icon = nativeAd.icon
        adView.iconView = iconView
        val iconView1 = adView.iconView
        if (iconView1 != null) {
            if (icon == null) {
                if (adType == ADType.DEFAULT_AD) {
                    val iconHeight = mediaMaxHeight
                    iconView1.layoutParams = LinearLayout.LayoutParams(1, iconHeight)
                }
            } else {
                if (adType == ADType.DEFAULT_AD) {
                    val iconHeight = mediaMaxHeight
                    iconView1.layoutParams = LinearLayout.LayoutParams(iconHeight, iconHeight)
                }
                (iconView1 as ImageView).setImageDrawable(icon.drawable)
                iconView1.visibility = VISIBLE
            }
        }

        val mediaView = adView.findViewById<MediaView>(R.id.ad_media)
        adView.mediaView = mediaView
        mediaView.setImageScaleType(ImageView.ScaleType.FIT_CENTER)
        mediaView.setOnHierarchyChangeListener(object : ViewGroup.OnHierarchyChangeListener {
            override fun onChildViewAdded(parent: View, child: View) {
                val maxHeightPixels = mediaMaxHeight
                if (child is ImageView) { //Images
                    child.adjustViewBounds = true
                    val layoutParams1 = child.layoutParams
                    layoutParams1.width = MATCH_PARENT
                    layoutParams1.height = mediaMaxHeight
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
           try {
               adView.storeView?.visibility = VISIBLE
               val textView1 = adView.storeView as TextView
               textView1.text = nativeAd.store
               if (textColor2 != null) {
                   textView1.setTextColor(textColor2)
               }
           }catch (e:java.lang.Exception){
               adView.storeView?.visibility = View.GONE
           }
        } else {
            adView.storeView?.visibility = View.GONE
        }


        val cta = adView.findViewById(R.id.call_to_action) as TextView
        cta.backgroundTintList = (ColorStateList.valueOf(Color.parseColor(buttonColor)))
        Log.d(TAG, adType+" :populateUnifiedNativeAdView: "+buttonColor)

        adView.callToActionView = cta
        adView.callToActionView?.visibility = VISIBLE
        (adView.callToActionView as TextView).text = nativeAd.callToAction
        adView.setNativeAd(nativeAd)
        if (nativeAd.adChoicesInfo != null && adView.adChoicesView != null) {
            try {
                val choicesView = AdChoicesView(adView.adChoicesView!!.context)
                adView.adChoicesView = choicesView
            } catch (e: Exception) {

            }
        }


    }

    fun showAvailableInterstitialAd(activity: Activity,interstitialAd: InterstitialAd?=null,adManagerInterstitialAd: AdManagerInterstitialAd? = null){
        if (interstitialAd!= null)
            interstitialAd?.show(activity)
        else if (adManagerInterstitialAd != null)
            adManagerInterstitialAd?.show(activity)
    }

    var create: AlertDialog? = null
    var builder: AlertDialog.Builder? = null
    fun showRewardedIntersAd(
        application: Activity,
        adId: String,
        adName: String,
        interstitialCallback: InterstitialCallback,
        isAdmanager:Boolean = false,
        loadTimeOut:Int
    ) {
        if (isInitialized){
            if (application != null){
                if (adId != "STOP" && AppPref.getBoolean(application,AppPref.showAppAds) && application.fetchAdStatusFromAdId(adName)) {
                    var fetchedTimer:Int = application.fetchAdLoadTimeout(adName)
                    if (fetchedTimer == 0){
                        fetchedTimer = loadTimeOut
                    }
                    var primaryIds = application.fetchPrimaryById(adName)
                    var secondaryIds = application.fetchSecondaryById(adName)

                    showAdLoaderLayout(application)

                    Log.d("rewardedInterstitial","OnStart:" + System.currentTimeMillis()/1000)
                    if (!isAdmanager) {
                        if (primaryIds.size > 0) {
                            showRewardedIntersAd(
                                adName,
                                application,
                                fetchedTimer.toLong(),
                                primaryIds,
                                interstitialCallback,
                                object : RewardInterstitialInternalCallback {
                                    override fun onSuccess(ad: RewardedInterstitialAd?) {
                                        Log.d(
                                            "rewardedInterstitial",
                                            "onSuccess: Primary Shown" + System.currentTimeMillis() / 1000
                                        )
                                        if (ad != null) {
                                            dismissAdLoaderLayout(application)
                                            ad!!.show(application, {

                                            })
                                        }
                                    }

                                    override fun onFailed(loadAdError: LoadAdError?) {
                                        if (secondaryIds.size > 0) {
                                            showRewardedIntersAd(
                                                adName,
                                                application,
                                                fetchedTimer.toLong(),
                                                secondaryIds,
                                                interstitialCallback,
                                                object : RewardInterstitialInternalCallback {
                                                    override fun onSuccess(ad: RewardedInterstitialAd?) {
                                                        Log.d(
                                                            "rewardedInterstitial",
                                                            "onSuccess: First Secondary Shown" + System.currentTimeMillis() / 1000
                                                        )
                                                        if (ad != null) {
                                                            dismissAdLoaderLayout(application)
                                                            ad!!.show(application, {

                                                            })
                                                        }
                                                    }

                                                    override fun onFailed(loadAdError: LoadAdError?) {
                                                        showRewardedIntersAd(
                                                            adName,
                                                            application,
                                                            fetchedTimer.toLong(),
                                                            listOf(adId),
                                                            interstitialCallback,
                                                            object : RewardInterstitialInternalCallback {
                                                                override fun onSuccess(ad: RewardedInterstitialAd?) {
                                                                    Log.d(
                                                                        "rewardedInterstitial",
                                                                        "onSuccess: First Fallback Shown" + System.currentTimeMillis() / 1000
                                                                    )
                                                                    if (ad != null) {
                                                                        dismissAdLoaderLayout(application)
                                                                        ad!!.show(application, {

                                                                        })
                                                                    }
                                                                }

                                                                override fun onFailed(loadAdError: LoadAdError?) {
                                                                    interstitialCallback.moveNext()
                                                                }
                                                            }
                                                        )
                                                    }
                                                }
                                            )
                                        } else {
                                            showRewardedIntersAd(
                                                adName,
                                                application,
                                                fetchedTimer.toLong(),
                                                listOf(adId),
                                                interstitialCallback,
                                                object : RewardInterstitialInternalCallback {
                                                    override fun onSuccess(ad: RewardedInterstitialAd?) {
                                                        Log.d(
                                                            "rewardedInterstitial",
                                                            "onSuccess: First else Fallback Shown" + System.currentTimeMillis() / 1000
                                                        )
                                                        if (ad != null) {
                                                            dismissAdLoaderLayout(application)
                                                            ad!!.show(application, {

                                                            })
                                                        }
                                                    }

                                                    override fun onFailed(loadAdError: LoadAdError?) {
                                                        interstitialCallback.moveNext()
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            )
                        } else if (secondaryIds.size > 0) {
                            showRewardedIntersAd(
                                adName,
                                application,
                                fetchedTimer.toLong(),
                                secondaryIds,
                                interstitialCallback,
                                object : RewardInterstitialInternalCallback {
                                    override fun onSuccess(ad: RewardedInterstitialAd?) {
                                        Log.d(
                                            "rewardedInterstitial",
                                            "onSuccess: Second Secondary Shown" + System.currentTimeMillis() / 1000
                                        )
                                        if (ad != null) {
                                            dismissAdLoaderLayout(application)
                                            ad!!.show(application, {

                                            })
                                        }
                                    }

                                    override fun onFailed(loadAdError: LoadAdError?) {
                                        showRewardedIntersAd(
                                            adName,
                                            application,
                                            fetchedTimer.toLong(),
                                            listOf(adId),
                                            interstitialCallback,
                                            object : RewardInterstitialInternalCallback {
                                                override fun onSuccess(ad: RewardedInterstitialAd?) {
                                                    Log.d(
                                                        "rewardedInterstitial",
                                                        "onSuccess: Second Fallback Shown" + System.currentTimeMillis() / 1000
                                                    )
                                                    if (ad != null) {
                                                        dismissAdLoaderLayout(application)
                                                        ad!!.show(application, {

                                                        })
                                                    }
                                                }

                                                override fun onFailed(loadAdError: LoadAdError?) {
                                                    interstitialCallback.moveNext()
                                                }
                                            }
                                        )
                                    }
                                }
                            )
                        } else {
                            showRewardedIntersAd(
                                adName,
                                application,
                                fetchedTimer.toLong(),
                                listOf(adId),
                                interstitialCallback,
                                object : RewardInterstitialInternalCallback {
                                    override fun onSuccess(ad: RewardedInterstitialAd?) {
                                        Log.d(
                                            "rewardedInterstitial",
                                            "onSuccess: else Fallback Shown" + System.currentTimeMillis() / 1000
                                        )
                                        if (ad != null) {
                                            dismissAdLoaderLayout(application)
                                            ad!!.show(application, {

                                            })
                                        }
                                    }

                                    override fun onFailed(loadAdError: LoadAdError?) {
                                        interstitialCallback.moveNext()
                                    }
                                }
                            )
                        }
                    }
                    else {
                        if (primaryIds.size >0){
                            showRewardedIntersAdManager(
                                adName,
                                application,
                                fetchedTimer.toLong(),
                                primaryIds,
                                interstitialCallback,
                                object :RewardInterstitialInternalCallback{
                                    override fun onSuccess(ad: RewardedInterstitialAd?) {
                                        Log.d("rewardedInterstitial", "onSuccess: Primary Shown" + System.currentTimeMillis()/1000)
                                        if (ad != null){
                                            dismissAdLoaderLayout(application)
                                            ad!!.show(application,{

                                            })
                                        }
                                    }

                                    override fun onFailed(loadAdError: LoadAdError?) {
                                        if (secondaryIds.size > 0){
                                            showRewardedIntersAdManager(
                                                adName,
                                                application,
                                                fetchedTimer.toLong(),
                                                secondaryIds,
                                                interstitialCallback,
                                                object :RewardInterstitialInternalCallback{
                                                    override fun onSuccess(ad: RewardedInterstitialAd?) {
                                                        Log.d("rewardedInterstitial", "onSuccess: First Secondary Shown" + System.currentTimeMillis()/1000)
                                                        if (ad != null){
                                                            dismissAdLoaderLayout(application)
                                                            ad!!.show(application,{

                                                            })
                                                        }
                                                    }

                                                    override fun onFailed(loadAdError: LoadAdError?) {
                                                        showRewardedIntersAdManager(
                                                            adName,
                                                            application,
                                                            fetchedTimer.toLong(),
                                                            listOf(adId),
                                                            interstitialCallback,
                                                            object :RewardInterstitialInternalCallback{
                                                                override fun onSuccess(ad: RewardedInterstitialAd?) {
                                                                    Log.d("rewardedInterstitial", "onSuccess: First Fallback Shown" + System.currentTimeMillis()/1000)
                                                                    if (ad != null){
                                                                        dismissAdLoaderLayout(application)
                                                                        ad!!.show(application,{

                                                                        })
                                                                    }
                                                                }

                                                                override fun onFailed(loadAdError: LoadAdError?) {
                                                                    interstitialCallback.moveNext()
                                                                }
                                                            }
                                                        )
                                                    }
                                                }
                                            )
                                        }
                                        else{
                                            showRewardedIntersAdManager(
                                                adName,
                                                application,
                                                fetchedTimer.toLong(),
                                                listOf(adId),
                                                interstitialCallback,
                                                object :RewardInterstitialInternalCallback{
                                                    override fun onSuccess(ad: RewardedInterstitialAd?) {
                                                        Log.d("rewardedInterstitial", "onSuccess: First else Fallback Shown" + System.currentTimeMillis()/1000)
                                                        if (ad != null){
                                                            dismissAdLoaderLayout(application)
                                                            ad!!.show(application,{

                                                            })
                                                        }
                                                    }

                                                    override fun onFailed(loadAdError: LoadAdError?) {
                                                        interstitialCallback.moveNext()
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            )
                        }
                        else if (secondaryIds.size > 0){
                            showRewardedIntersAdManager(
                                adName,
                                application,
                                fetchedTimer.toLong(),
                                secondaryIds,
                                interstitialCallback,
                                object :RewardInterstitialInternalCallback{
                                    override fun onSuccess(ad: RewardedInterstitialAd?) {
                                        Log.d("rewardedInterstitial", "onSuccess: Second Secondary Shown" + System.currentTimeMillis()/1000)
                                        if (ad != null){
                                            dismissAdLoaderLayout(application)
                                            ad!!.show(application,{

                                            })
                                        }
                                    }

                                    override fun onFailed(loadAdError: LoadAdError?) {
                                        showRewardedIntersAdManager(
                                            adName,
                                            application,
                                            fetchedTimer.toLong(),
                                            listOf(adId),
                                            interstitialCallback,
                                            object :RewardInterstitialInternalCallback{
                                                override fun onSuccess(ad: RewardedInterstitialAd?) {
                                                    Log.d("rewardedInterstitial", "onSuccess: Second Fallback Shown" + System.currentTimeMillis()/1000)
                                                    if (ad != null){
                                                        dismissAdLoaderLayout(application)
                                                        ad!!.show(application,{

                                                        })
                                                    }
                                                }

                                                override fun onFailed(loadAdError: LoadAdError?) {
                                                    interstitialCallback.moveNext()
                                                }
                                            }
                                        )
                                    }
                                }
                            )
                        }
                        else{
                            showRewardedIntersAdManager(
                                adName,
                                application,
                                fetchedTimer.toLong(),
                                listOf(adId),
                                interstitialCallback,
                                object :RewardInterstitialInternalCallback{
                                    override fun onSuccess(ad: RewardedInterstitialAd?) {
                                        Log.d("rewardedInterstitial", "onSuccess: else Fallback Shown" + System.currentTimeMillis()/1000)
                                        if (ad != null){
                                            dismissAdLoaderLayout(application)
                                            ad!!.show(application,{

                                            })
                                        }
                                    }

                                    override fun onFailed(loadAdError: LoadAdError?) {
                                        interstitialCallback.moveNext()
                                    }
                                }
                            )
                        }
                    }
                } else {
                    interstitialCallback.moveNext(AdError(404, "AD BLOCKED", ""))
                }
            }
            else {
                interstitialCallback.onContextFailed()
            }

        }
        else {
            interstitialCallback.moveNext(AdError(404, "Ads SDK not (initialized / properly initial). Try initializing again.", ""))
        }

    }

    internal  fun showRewardedIntersAd(
        adName:String,
        activity: Activity,
        timer: Long,
        primaryIds: List<String>,
        interstitialCallback: InterstitialCallback,
        rewardInternalCallback: RewardInterstitialInternalCallback
    ){
        var isShown = false
        var ads: RewardedInterstitialAd? = null
        var loadAdError: LoadAdError? = null
        object : CountDownTimer(timer, 500) {
            override fun onTick(p0: Long) {
                if (ads != null){
                    rewardInternalCallback.onSuccess(ads)
                    this.cancel()
                }
            }

            override fun onFinish() {
                if (ads != null){
                    rewardInternalCallback.onSuccess(ads)
                }
                else{
                    rewardInternalCallback.onFailed(loadAdError)
                }
            }
        }.start()

        for (adId in primaryIds){
            RewardedInterstitialAd.load(activity, adId,
                AdRequest.Builder().build(), object : RewardedInterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedInterstitialAd) {
                        if (!isShown) {
                            ads = ad
                            isShown = true
                            Log.d(TAG, "Ad was loaded.")
                            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                                override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                                    super.onAdFailedToShowFullScreenContent(p0)
                                        interstitialCallback.moveNext(p0)
                                }

                                override fun onAdDismissedFullScreenContent() {
                                    super.onAdDismissedFullScreenContent()
                                        interstitialCallback.moveNext()
                                }
                            }
                        }
                    }

                    override fun onAdFailedToLoad(adError: LoadAdError) {
//                        dismissAdLoaderLayout(activity)
                        if (ads == null)
                            ads = null
                        loadAdError = adError
                        Log.d(TAG, "onAdFailedToLoad: "+adName+" : "+adId+" : "+adError.message)
//                        if (!isShown) {
//                            interstitialCallback.moveNext(adError)
//                            isShown = true
//                        }
                    }
                })
        }
    }

    internal  fun showRewardedIntersAdManager(
        adName: String,
        activity: Context,
        timer: Long,
        primaryIds: List<String>,
        interstitialCallback: InterstitialCallback,
        rewardInternalCallback: RewardInterstitialInternalCallback
    ){
        var isShown = false
        var ads: RewardedInterstitialAd? = null
        var loadAdError: LoadAdError? = null
        object : CountDownTimer(timer, 500) {
            override fun onTick(p0: Long) {
                if (ads != null){
                    rewardInternalCallback.onSuccess(ads)
                    this.cancel()
                }
            }

            override fun onFinish() {
                if (ads != null){
                    rewardInternalCallback.onSuccess(ads)
                }
                else{
                    rewardInternalCallback.onFailed(loadAdError)
                }
            }
        }.start()

        for (adId in primaryIds){
            RewardedInterstitialAd.load(activity, adId,
                AdManagerAdRequest.Builder().build(), object : RewardedInterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedInterstitialAd) {
                        if (!isShown) {
                            ads = ad
                            isShown = true
                            Log.d(TAG, "Ad was loaded.")
                            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                                override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                                    super.onAdFailedToShowFullScreenContent(p0)
                                    interstitialCallback.moveNext(p0)
                                }

                                override fun onAdDismissedFullScreenContent() {
                                    super.onAdDismissedFullScreenContent()
                                    interstitialCallback.moveNext()
                                }
                            }
                        }
                    }

                    override fun onAdFailedToLoad(adError: LoadAdError) {
//                        dismissAdLoaderLayout(activity)
                        if (ads == null)
                            ads = null
                        loadAdError = adError
                        Log.d(TAG, "onAdFailedToLoad: "+adName+" : "+adId+" : "+adError.message)
//                        if (!isShown) {
//                            interstitialCallback.moveNext(adError)
//                            isShown = true
//                        }
                    }
                })
        }
    }

    internal fun showAdLoaderLayout(activity: Activity) {
        dismissAdLoaderLayout(activity)//Added This So that The alertDialog Variable is never used twice which will lead to no closing of the dialog
        builder = AlertDialog.Builder(activity, R.style.DialogTheme)
        builder?.setView(
            LayoutInflater.from(activity).inflate(R.layout.ad_loading_layout_inters, null)
        )
        create = builder?.create()
        if (!activity.isFinishing) {
            create?.show()
        }
    }


    val preloadedRewardedAdList: HashMap<String, RewardedAd?> = hashMapOf()
    val AdRewardedList: HashMap<String, Boolean> = hashMapOf()

    fun preLoadRewardedAd(
        application: Context?,
        adUnit: String,
        adName: String
    ) {
        EmptyAdList(adUnit)
        if (application != null && AppPref.getBoolean(application,AppPref.showAppAds) && application.fetchAdStatusFromAdId(adName)) {
            val adRequest = AdRequest.Builder()
                .addNetworkExtrasBundle(AdMobAdapter::class.java, getConsentEnabledBundle())
                .build()
            RewardedAd.load(
                application,
                adUnit,
                adRequest,
                object : RewardedAdLoadCallback() {
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        EmptyAdList(adUnit)
                    }

                    override fun onAdLoaded(rewardedAd: RewardedAd) {
                        preloadedRewardedAdList[adUnit] = rewardedAd
                        AdRewardedList[adUnit] = false
                    }
                },
            )
        }
    }

    private fun EmptyAdList(adUnit: String) {
        preloadedRewardedAdList[adUnit] = null
        AdRewardedList[adUnit] = false
    }

    fun showRewardedAdsAfterWait(
        activity: Activity?,
        timeToWait: Long,
        adId: String,
        adName: String,
        callback: RewardedCallback
    ) {
        if (isInitialized){
            if (activity != null && adId != "STOP") {
                showAdLoaderLayout(activity)
                if (preloadedRewardedAdList.containsKey(adId)) {
                    var rewardedAd: RewardedAd? = null
                    var ctd: CountDownTimer? = null
                    ctd = object : CountDownTimer(timeToWait, 500) {
                        override fun onTick(millisUntilFinished: Long) {
                            rewardedAd = preloadedRewardedAdList[adId]
                            if (rewardedAd != null) {
                                ctd?.cancel()
                                ctd = null
                                rewardedAd?.fullScreenContentCallback =
                                    object : FullScreenContentCallback() {
                                        override fun onAdDismissedFullScreenContent() {
                                            super.onAdDismissedFullScreenContent()
                                            callback.moveNext(AdRewardedList[adId] ?: false)
                                            dismissAdLoaderLayout(activity)
                                        }

                                        override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                                            super.onAdFailedToShowFullScreenContent(p0)
                                            callback.moveNext(p0)
                                            dismissAdLoaderLayout(activity)
                                        }

                                        override fun onAdShowedFullScreenContent() {
                                            super.onAdShowedFullScreenContent()
                                            preloadedRewardedAdList.remove(adId)
                                            AdRewardedList[adId] = false
                                            dismissAdLoaderLayout(activity)
                                        }
                                    }
                                Handler(Looper.getMainLooper()).postDelayed({
                                    rewardedAd?.show(
                                        activity
                                    ) { AdRewardedList[adId] = true }
                                }, 1500)
                            }
                        }

                        override fun onFinish() {
                            EmptyAdList(adId)
                            preloadedRewardedAdList.remove(adId)
                            AdRewardedList[adId] = false
                            dismissAdLoaderLayout(activity)
                            callback.adNotLoaded()
                        }
                    }
                    ctd?.start()
                } else {
                    preLoadRewardedAd(activity, adId,adName)
                    showRewardedAdsAfterWait(
                        activity,
                        timeToWait,
                        adId,
                        adName,
                        callback
                    )
                }
            } else {
                callback.onContextFailed()
            }
        }
        else {
            callback.moveNext(AdError(404,"Ads SDK not (initialized / properly initial). Try initializing again.",""))
        }

    }

    private fun dismissAdLoaderLayout(activity: Activity) {
        if (!activity.isFinishing) {
            create?.dismiss()
        }
    }


    interface BypassAppOpenAd

}