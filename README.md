# ad-utils

## What

An ad util library to facilitate easy and standardized implementation of latest Admob SDK

## Initialization

In your  `build.gradle`:

```groovy

allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}

dependencies {
    implementation 'com.github.Appyhigh:ad-utils:1.1.3'
}
...
buildFeatures {
    dataBinding true
    viewBinding true
}
```

Add these configurations to you AndroidManifest.xml

```xml

<meta-data android:name="com.google.android.gms.ads.APPLICATION_ID"
    android:value="ca-app-pub-XXXXXXXXXXXXXXXX~XXXXXXXXXX" /> 
```

Initialize Sdk without App Open Ad

```kotlin
AdSdk.initialize(applicationContext as MyApp)
```

Initialize Sdk with App Open Ad

```kotlin

/**
 * Call initialize with you Application class object
 *
 * @param app -> Pass your application context here
 * @param appOpenAdUnit -> Pass an app open ad unit id if you wish to ad an app open ad
 * @param appOpenAdCallback -> This is the nullable listener for app open ad callbacks
 * @param bannerRefreshTimer -> Pass 0L to stop refresh or pass your required refresh interval in milliseconds. (Default Value is 45 seconds)
 * @param nativeRefreshTimer -> Pass 0L to stop refresh or pass your required refresh interval in milliseconds. (Default Value is 45 seconds)
 * @param loadSplashAppOpenAd : Boolean = false-> Load the Splash App Open ad if set to true default is false
 * @param showFGToBGAdOnlyOnce : Boolean = false-> show BG to FG ads only once or multiple times
 */

AdSdk.initialize(
    activity = null,
    app = applicationContext as MyApp,
    appOpenAdUnit = "ca-app-pub-3940256099942544/3419835294",
    appOpenAdCallback = null,
    bannerRefreshTimer = 0L,
    nativeRefreshTimer = 0L,
    loadSplashAppOpenAd = true,
    showFGToBGAdOnlyOnce = false
)
```

## Show App Open Ad

```kotlin
//Use this to Start App Open Ad Loading

AppOpenManager.loadSplashAppOpenAd(application, adID)

//This is an example load method
var waitTimeinSec = 5
private fun loadAppOpenAd() {
    AppOpenManager.showAdIfAvailable(this,
        object : AppOpenManager.Companion.appOpenCallBack {
            override fun adDismissed() {
                Log.d("appOpenCallBack", "adDismissed: ")
            }

            override fun adLoaded(appOpenAd: AppOpenAd) {
                Log.d("appOpenCallBack", "adLoaded: 11 ")
                //TODO: Show the Ad Here
            }

            override fun adError(message: String?) {

            }

            override fun adShown() {

            }

            override fun adClicked() {

            }

            override fun adNotLoaded(reason: String?) {
                //TODO : Add the Timer logic here
                //This is the example
                Log.d("appOpenCallBack", "adNotLoaded: $reason")
                if (waitTimeinSec == 0) {
                    this.adError(null)
                } else {
                    Thread {
                        //Infinite wait
                        Thread.sleep(1000)
                        loadAppOpenAd()
                    }.start()
                }
            }
        })
}
```

## Banner Ad

```kotlin
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

AdSdk.loadBannerAd(
    lifecycle,
    binding.llRoot,
    bannerAdUnit,
    AdSize.BANNER,
    bannerAdLoadCallback
)

private val bannerAdLoadCallback = object : BannerAdLoadCallback {
    override fun onAdLoaded() {
        Log.d("BannerAdLoadCallback", "onAdLoaded")
    }

    override fun onAdFailedToLoad(adError: LoadAdError) {
        Log.d("BannerAdLoadCallback", "onAdFailedToLoad")
    }

    override fun onAdOpened() {
        Log.d("BannerAdLoadCallback", "onAdOpened")
    }

    override fun onAdClicked() {
        Log.d("BannerAdLoadCallback", "onAdClicked")
    }

    override fun onAdClosed() {
        Log.d("BannerAdLoadCallback", "onAdClosed")
    }

}

```

## Interstitial Ad

```kotlin
/**
 * Call loadInterstitialAd with following params to load an interstitial ad
 *
 * @param adUnit -> Pass the adUnit id in this parameter
 * @param interstitialAdUtilLoadCallback -> nullable callback to register interstitial ad load events
 *
 * IMPORTANT: You wont be able to show ad if you pass a null callback
 */

AdSdk.loadInterstitialAd(
    "ca-app-pub-3940256099942544/1033173712",
    mInterstitialAdUtilCallback
)

//Callback Registration

private val mInterstitialAdUtilCallback = object : InterstitialAdUtilLoadCallback {
    override fun onAdFailedToLoad(adError: LoadAdError, ad: InterstitialAd?) {
        interstitialAd = ad
    }

    override fun onAdLoaded(ad: InterstitialAd?) {
        interstitialAd = ad
    }

    override fun onAdDismissedFullScreenContent() {
        /**
         * Comment this if you want the ad to load just once
         * Uncomment this to load ad again once shown
         */
        loadInterstitialAd()
    }

    override fun onAdFailedToShowFullScreenContent(adError: AdError?) {}

    override fun onAdShowedFullScreenContent() {
        interstitialAd = null
    }
}

//Show Ad

interstitialAd?.show(this)

```

### To Show Splash Ad Use this Method

    AdSdk.loadSplashAd(
                "ca-app-pub-3940256099942544/1033173712",
                this,
                object : SplashInterstitialCallback {
                    override fun moveNext() {
                        startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                    }
                })

> adUnit: String, activity: Activity?, callback: SplashInterstitialCallback, timer: Long = 5000L

These are the parameters

    fun moveNext()

This is the callback of the splash it simply means to move to the next activity or do whatever you
wanted to do on splash fail or user saw the ad or if the timer runs out

## Rewarded Ad

```kotlin
/**
 * Call loadRewardedAd with following params to load an rewarded ad
 *
 * @param adUnit -> Pass the adUnit id in this parameter
 * @param rewardedAdUtilCallback -> nullable callback to register rewarded ad load events
 *
 * IMPORTANT: You wont be able to show ad if you pass a null callback
 */

AdSdk.loadRewardedAd(
    "ca-app-pub-3940256099942544/5224354917",
    mRewardedAdUtilCallback
)

//Callback Registration

private val mRewardedAdUtilCallback = object : RewardedAdUtilLoadCallback {
    override fun onAdFailedToLoad(adError: LoadAdError, ad: RewardedAd?) {
        rewardedAd = ad
    }

    override fun onAdLoaded(ad: RewardedAd?) {
        rewardedAd = ad
    }

    override fun onAdDismissedFullScreenContent() {
        /**
         * Comment this if you want the ad to load just once
         * Uncomment this to load ad again once shown
         */
        loadRewardedAd()
    }

    override fun onAdFailedToShowFullScreenContent(adError: AdError?) {}

    override fun onAdShowedFullScreenContent() {
        rewardedAd = null
    }
}

//Show Ad

rewardedAd?.show(this)

```

# Load a Native Ad

## Enable firebase remote config for the app.

```kotlin

In your build.gradle(app) add :

##IMPORTANT:
#Create a firebase account for this project .
#Add the google - services.json to your project .

plugins {
    ...
    id 'com.google.gms.google-services'
}

dependencies {
    //Firebase Remote Config
    implementation platform ('com.google.firebase:firebase-bom:29.0.2')
    implementation 'com.google.firebase:firebase-config-ktx'
    implementation 'com.google.firebase:firebase-analytics-ktx'
}



In your build.gradle(project) add :

buildscript {
    dependencies {
        classpath 'com.google.gms:google-services:4.3.10'
        ...
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
 * @param RemoteConfigUtils.getNativeAdTypeId() -> returns a value from remote config and according to the value, layout is loaded.
 */
```

# In the MainApplication add this line

in the firebase remote config of your application project ad this native_ad_layout_type

if you want to set layout via remote config

```kotlin
/**
 *Go to the Firebase of your Project and set this variable in Remore Config
 * native_ad_layout_type - Type String - Value - 1 to 5, no need for quotes
 */

RemoteConfigUtils.init()



/**
 * Call loadNativeAd with following params to load a Native Ad
 *
 *
 * @param activty -> Non-Null activity parameter
 * @param lifecycle -> Lifecycle of activity in which ad will be loaded
 * @param adUnit -> Pass the adUnit id in this parameter
 * @param viewGroup -> Pass the parent ViewGroup to add a native ad in that layout
 * @param nativeAdLoadCallback -> nullable callback to register native ad load events
 * @param adType -> nullable layoutId, if you want a custom layout, pass a custom layout otherwise its load default UI
 * @param populator -> nullable populator, if you want a custom population method, pass a method which takes (NativeAd, NativeAdView) as params
 * @param background -> nullable It is the Background Color or a Drawable you can put either, if it matches nothing then it'll choose default
 * @param textColor1 -> nullable It is the primary color in Int that is Color Resource
 * @param textColor1 -> nullable It is the Secondary color in Int that is Color Resource
 * @param maxHeight -> nullable It is the height of the ad Media in Int
 *
 *
 * The adType can be either string size from "1" to "5"
 * or one of these below
 *
 * class ADType {
 * companion object {
 * val SMALLEST = "3"
 * val SMALLER = "4"
 * val SEMIMEDIUM = "2"
 * val MEDIUM = "1"
 * val BIG = "5"
 * }
 * }
 *
 *e.g. AdType.SMALL
 *
 *
 */

/**
 * Examples
 */

AdSdk.loadNativeAd(
    this,
    lifecycle,
    "ca-app-pub-3940256099942544/2247696110",
    binding.adView,
    nativeAdCallBack,
    AdSdk.ADType.DEFAULT_NATIVE_SMALL, null, null, null, maxHeight = 200
)

AdSdk.loadNativeAd(
    activity,
    lifecycle,
    "ca-app-pub-3940256099942544/2247696110",
    binding.llRoot,
    nativeAdCallBack,
    RemoteConfigUtils.getNativeAdTypeId()
)


private val nativeAdCallBack = object : NativeAdLoadCallback {
    override fun onAdLoaded() {
        Log.d("NativeAdLoadCallback", "onAdLoaded")
    }

    override fun onAdFailed(adError: LoadAdError) {
        Log.d("NativeAdLoadCallback", "onAdFailed")
    }

    override fun onAdClicked() {
        Log.d("NativeAdLoadCallback", "onAdClicked")
    }
}

fun populateNativeAdView(nativeAd: NativeAd, adView: NativeAdView) {
    val ratingBar = adView.findViewById(R.id.stars) as View
    adView.starRatingView = ratingBar
    val adHeadline = adView.findViewById(R.id.headline) as TextView
    adView.headlineView = adHeadline
    val adBody = adView.findViewById(R.id.body) as TextView
    adView.bodyView = adBody
    adView.setNativeAd(nativeAd)
}
```

