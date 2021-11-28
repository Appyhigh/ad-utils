package com.appyhigh.adutils

import android.app.Application
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.appcompat.widget.LinearLayoutCompat
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

class AdSdk {
    companion object {
        private var application: Application? = null
        private var TAG = "AdSdk"
    }

    /**
     * Call initialize with you Application class object
     *
     * @param app -> Pass your application context here
     * @param appOpenAdUnit -> Pass an app open ad unit id if you wish to ad an app open ad
     * @param appOpenAdCallback -> This is the nullable listener for app open ad callbacks
     */

    fun initialize(
        app: Application,
        appOpenAdUnit: String = "",
        appOpenAdCallback: AppOpenAdCallback?=null
    ) {
        application = app
        application?.let { myApp ->
            MobileAds.initialize(myApp) {
                if (appOpenAdUnit.isNotEmpty()) {
                    val appOpenManager = AppOpenManager(myApp, appOpenAdUnit,appOpenAdCallback)
                    appOpenAdCallback?.onInitSuccess(appOpenManager)
                }
            }
        }
    }

    /**
     * Call loadBannerAd with following parameters to load a banner ad
     *
     *
     * @param llRoot -> This is the view (LinearLayoutCompat) you need to supply in which your ad unit will be loaded
     * @param adSize -> Pass the adUnit id in this parameter
     * @param adSize -> Pass the AdSize for banner that you want to load eg: AdSize.BANNER
     * @param bannerAdLoadCallback -> it is a nullable callback to register ad load events, pass null if you don't need callbacks
     *
     */

    fun loadBannerAd(
        llRoot: LinearLayoutCompat,
        adUnit: String,
        adSize: AdSize,
        bannerAdLoadCallback: BannerAdLoadCallback?
    ) {
        if (application != null) {
            val adRequest = AdRequest.Builder().build()
            val mAdView = AdView(llRoot.context)
            mAdView.adSize = adSize
            mAdView.adUnitId = adUnit
            mAdView.loadAd(adRequest)
            llRoot.addView(mAdView)

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
        } else {
            throw Exception("Please make sure that you have initialized the AdSdk using AdSdk.initialize!!!")
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
        } else {
            throw Exception("Please make sure that you have initialized the AdSdk using AdSdk.initialize!!!")
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
        } else {
            throw Exception("Please make sure that you have initialized the AdSdk using AdSdk.initialize!!!")
        }
    }

    /**
     * Call loadNativeAd with following params to load an interstitial ad
     *
     * @param adUnit -> Pass the adUnit id in this parameter
     * @param llRoot -> Pass the parent LinearLayoutCompat to add a native ad in that layout
     * @param layoutId -> nullable layoutId, if you want a custom layout, pass a custom layout otherwise its load default UI
     * @param nativeAdLoadCallback -> nullable callback to register native ad load events
     */

    fun loadNativeAd(
        adUnit: String,
        llRoot: LinearLayoutCompat,
        nativeAdLoadCallback: NativeAdLoadCallback?,
        layoutId: Int = R.layout.ad_item
    ) {
        if (application != null) {
            var nativeAd: NativeAd? = null
            val adLoader: AdLoader? = AdLoader.Builder(application!!, adUnit)
                .forNativeAd { ad: NativeAd ->
                    nativeAd = ad
                }
                .withAdListener(object : AdListener() {
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        Log.e("Ad Load Failed", adError.toString())
                        nativeAdLoadCallback?.onAdFailed()
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
                            populateUnifiedNativeAdView(nativeAd!!, adView)
                            llRoot.removeAllViews()
                            llRoot.addView(adView)
                        }
                    }
                })
                .withNativeAdOptions(
                    NativeAdOptions.Builder()
                        .build()
                )
                .build()
            adLoader?.loadAd(AdRequest.Builder().build())
        } else {
            throw Exception("Please make sure that you have initialized the AdSdk using AdSdk.initialize!!!")
        }
    }

    fun populateUnifiedNativeAdView(
        nativeAd: NativeAd,
        adView: NativeAdView?
    ) {
        val iconView = adView?.findViewById(R.id.icon) as ImageView
        Log.e("nativead", "ad body : " + nativeAd.body)

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