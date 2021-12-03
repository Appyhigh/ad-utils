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
   implementation 'com.github.Appyhigh:ad-utils:1.0.5'
}
```

Add these configurations to you AndroidManifest.xml

```xml
<meta-data
    android:name="com.google.android.gms.ads.APPLICATION_ID"
    android:value="ca-app-pub-XXXXXXXXXXXXXXXX~XXXXXXXXXX" /> 
```

Initialize Sdk without App Open Ad

```kotlin
AdSdk().initialize(applicationContext as MyApp)
```

Initialize Sdk with App Open Ad

```kotlin
AdSdk().initialize(
    applicationContext as MyApp,
    "ca-app-pub-3940256099942544/3419835294",
    appOpenAdCallback
)
```

## Banner Ad

```kotlin
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
     
AdSdk().loadBannerAd(
    binding.llRoot,
    bannerAdUnit,
    AdSize.BANNER,
    bannerAdLoadCallback
)

private val bannerAdLoadCallback = object :BannerAdLoadCallback{
    override fun onAdLoaded() {
        Log.d("BannerAdLoadCallback","onAdLoaded")
    }

    override fun onAdFailedToLoad(adError: LoadAdError) {
        Log.d("BannerAdLoadCallback","onAdFailedToLoad")
    }

    override fun onAdOpened() {
        Log.d("BannerAdLoadCallback","onAdOpened")
    }

    override fun onAdClicked() {
        Log.d("BannerAdLoadCallback","onAdClicked")
    }

    override fun onAdClosed() {
        Log.d("BannerAdLoadCallback","onAdClosed")
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

AdSdk().loadInterstitialAd(
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

AdSdk().loadRewardedAd(
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

## Load a Native Ad

```kotlin
/**
 * Call loadNativeAd with following params to load an interstitial ad
 *
 * @param adUnit -> Pass the adUnit id in this parameter
 * @param llRoot -> Pass the parent LinearLayoutCompat to add a native ad in that layout
 * @param layoutId -> nullable layoutId, if you want a custom layout, pass a custom layout otherwise its load default UI
 * @param nativeAdLoadCallback -> nullable callback to register native ad load events
 */
AdSdk().loadNativeAd(
    "ca-app-pub-3940256099942544/2247696110",
    binding.llRoot,
    nativeAdCallBack,
    R.layout.ad_item_big
)

private val nativeAdCallBack = object :NativeAdLoadCallback{
    override fun onAdLoaded() {
        Log.d("NativeAdLoadCallback","onAdLoaded")
    }

    override fun onAdFailed() {
        Log.d("NativeAdLoadCallback","onAdFailed")
    }

    override fun onAdClicked() {
        Log.d("NativeAdLoadCallback","onAdClicked")
    }
}
```

