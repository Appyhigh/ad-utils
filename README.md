# ad-utils

## What

An ad util library to facilitate easy and standardized implementation of latest Admob SDK

## Initialization

In your  `build.gradle`:

```groovy
allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}
dependencies {
    implementation 'com.github.Appyhigh:ad-utils:x.x.x'
} 

``` 

### To Use ADSDK Version 2.2.0 or above you need to make sure target SDK is 31

Add these configurations to you AndroidManifest.xml

 ```xml 
 <meta-data 
 android:name="com.google.android.gms.ads.APPLICATION_ID"
 android:value="ca-app-pub-XXXXXXXXXXXXXXXX~XXXXXXXXXX" 
 /> 
 ``` 

Ad Initialize Optimisations Use Google Mobile Ads SDK version 21.0.0 or higher.    
https://developers.google.com/admob/android/optimize-initialization#update_your_manifest_file

 ```xml

<manifest>
    <application>
        <meta-data android:name="com.google.android.gms.ads.flag.OPTIMIZE_INITIALIZATION"
            android:value="true" />
    </application>
</manifest> 
```   

Initialize Sdk without App Open Ad

```kotlin 
/** 
Parameters 
dynamicAdsFetchThresholdInSecs -> This is the amount of time to waut in Seconds before fetching new ad id values    
*/  
AdSdk.initialize( applicationContext as MyApp, dynamicAdsFetchThresholdInSecs = 10)    
 ```   

```kotlin
AdSdk.loadNPAForm([Privacy Policy URL], [activity], [Publisher_ID])

fun loadNPAForm(
    privacyPolicyLink: String,
    activity: Activity,
    pubValue: String,
    testDevice: String = "59106BA0F480E2EC4CD8CC7AA2C49B81"
)
//if this is your app ID - ca-app-pub-3940256099942544~3347511713 
//Then this is your PubID pub-3940256099942544 
// if you want to test the consent form pass the testdeviceID as 4th parameter      
``` 

Initialize Sdk with App Open Ad

```kotlin      
/**
Call initialize with you Application class object
@param app -> Pass your application context here
@param bannerRefreshTimer -> Pass 0L to stop refresh or pass your required refresh interval in milliseconds. (Default Value is 45 seconds)
@param nativeRefreshTimer -> Pass 0L to stop refresh or pass your required refresh interval in milliseconds. (Default Value is 45 seconds)
@param testDeviceID -> You can pass the testdeviceid here to get test ads 
*/ 

AdSdk.initialize(
applicationContext as MyApp, 55000L, 60000L, "[Test Device ID]"
)    
 /**
If you want to precache service native ads then only you need to add the preloadingNativeAdList and layoutInflater 
*/ 
initialize( app: Application, 
bannerRefreshTimer: Long = 45000L, 
nativeRefreshTimer: Long = 45000L, 
testDevice: String? = null, 
preloadingNativeAdList: HashMap<String, PreloadNativeAds>? = null, 
layoutInflater: LayoutInflater? = null ) 
``` 
## Dynamic Ads
### List All Dynamic Ads from the Ads Dashboard For this App

```kotlin 
/**
Parameters applicationContext: Context -> Pass the Context logTag: String -> Pass the Tag for logging the list of availbale ad ids will be shown with this log tag
**/
fun listAllAds(applicationContext: Context, logTag: String)    
 ``` 

### Get the Dynamic Ad Id Via the AdName as Set in Dashboard

``` kotlin 
/**
Parameters 
fallBackAdId: String -> This is the ad id which will be returned instead if the expected ad id is not found 
adName: String -> This name will be used as a key from the ad list to fetch the ad id 
*/    
 fun getDynamicAdsId(fallBackAdId: String?, adName: String) //if the ad id is not found it will return the fallBackAdId    
 Example AdSdk.loadNativeAd(    
 lifecycle, DynamicsAds.getDynamicAdsId("ca-app-pub-3940256099942544/2247696110","ADNAME"), binding.llRoot4, nativeAdCallBack, AdSdk.ADType.SMALLEST, null, null, null, mediaMaxHeight = 100,)    
    
 ```

## To show AppOpenAd when app comes from Background to Foreground

![chrome_zXtIBvGj33](https://user-images.githubusercontent.com/71721356/184829218-2ad642ec-56d4-47de-aa47-eac0009958fc.png)

```kotlin 
/**      
Call initialize with you Application class object * * @param appOpenAdUnit -> Pass an app open ad unit id if you wish to ad an app open ad * @param appOpenAdCallback -> This is the nullable listener for app open ad callbacks * @param backgroundThreshold -> Minimum time in millis that app should remain in background before showing AppOpenAd **/ AdSdk.attachAppOpenAdManager("ca-app-pub-3940256099942544/3419835294", null, 30000)  >
 */
 ``` 

### Implement `BypassAppOpenAd` interface in Activities where you do not want to show AppOpenAd when app resumes

### Show AppOpen Ad on SplashScreen

 ```kotlin 
 AdSdk.loadAppOpenAd(this, "ca-app-pub-3940256099942544/3419835294", true,      
 object : AppOpenAdLoadCallback() {      
 override fun onAdFailedToLoad(loadAdError: LoadAdError) { launchHomeActivity() }      
 override fun onAdFailedToShow(adError: AdError) { launchHomeActivity() }      
override fun onAdClosed() { launchHomeActivity() } }) 
```

## Banner Ad

 ```kotlin 
 /** * Call loadBannerAd with following parameters to load a banner ad * * @param lifecycle -> Lifecycle of activity in which ad will be loaded * @param viewGroup -> Pass the parent ViewGroup in which your ad unit will be loaded * @param adUnit -> Pass the adUnit id in this parameter * @param adSize -> Pass the AdSize for banner that you want to load eg: AdSize.BANNER * @param bannerAdLoadCallback -> it is a nullable callback to register ad load events, pass null if you don't need callbacks * */ AdSdk.loadBannerAd(      
 lifecycle, binding.llRoot, bannerAdUnit, AdSize.BANNER, bannerAdLoadCallback)      
 private val bannerAdLoadCallback = object : BannerAdLoadCallback {      
 override fun onAdLoaded() { Log.d("BannerAdLoadCallback", "onAdLoaded") }      
 override fun onAdFailedToLoad(adError: LoadAdError) { Log.d("BannerAdLoadCallback", "onAdFailedToLoad") }      
 override fun onAdOpened() { Log.d("BannerAdLoadCallback", "onAdOpened") }      
override fun onAdClicked() { Log.d("BannerAdLoadCallback", "onAdClicked") } override fun onAdClosed() { Log.d("BannerAdLoadCallback", "onAdClosed") } }      
``` 

## Interstitial Ad

```kotlin 
/** 
Call loadInterstitialAd with following params to load an interstitial ad
@param adUnit -> Pass the adUnit id in this parameter
@param interstitialAdUtilLoadCallback -> nullable callback to register interstitial ad load events 
*/
```

**IMPORTANT: You wont be able to show ad if you pass a null callbackinterstitialAdUtilLoadCallback
callbacks available**

```kotlin 
/***
fun onAdFailedToLoad(
adError: LoadAdError, ad: InterstitialAd?)
fun onAdLoaded(ad: InterstitialAd?)
fun onAdImpression()
fun onAdDismissedFullScreenContent()
fun onAdFailedToShowFullScreenContent(adError: AdError?)
fun onAdShowedFullScreenContent() 
*/ 

AdSdk.loadInterstitialAd(      
 "ca-app-pub-3940256099942544/1033173712", mInterstitialAdUtilCallback)      
 //Callback Registration      
 private val mInterstitialAdUtilCallback = object : InterstitialAdUtilLoadCallback {      
 override fun onAdFailedToLoad(adError: LoadAdError, ad: InterstitialAd?) { interstitialAd = ad }      
 override fun onAdLoaded(ad: InterstitialAd?) { interstitialAd = ad }      
 override fun onAdDismissedFullScreenContent() { /** * Comment this if you want the ad to load just once * Uncomment this to load ad again once shown */ loadInterstitialAd() }      
 override fun onAdFailedToShowFullScreenContent(adError: AdError?) {}      
 override fun onAdShowedFullScreenContent() { interstitialAd = null }}      
 //Show Ad      
 interstitialAd?.show(this)      
``` 

### To Show Splash Ad Use this Method

 ```kotlin 
/***
AdSdk.loadSplashAd( 
"ca-app-pub-3940256099942544/1033173712", 
this, 
object :  SplashInterstitialCallback
 { override fun moveNext() 
 {
 startActivity(Intent(this@SplashActivity,  MainActivity::class.java))
  }
  }),
 adUnit: String, 
 activity: Activity?, 
 callback: SplashInterstitialCallback, 
 timer: Long = 5000L)
  
These are the parameters  
*/
fun moveNext()      
This is the callback of the splash it simply means to move to the next activity or do whatever you      
wanted to do on splash fail or user saw the ad or if the timer runs out  
  
## Rewarded Ad  
  
```kotlin 
/**
Call loadRewardedAd with following params to load an rewarded ad
@param adUnit -> Pass the adUnit id in this parameter
@param rewardedAdUtilCallback -> nullable callback to register rewarded ad load events 
IMPORTANT: You wont be able to show ad if you pass a null callback 
*/ 
AdSdk.loadRewardedAd(      
 "ca-app-pub-3940256099942544/5224354917", mRewardedAdUtilCallback)      
 //Callback Registration      
 private val mRewardedAdUtilCallback = object : RewardedAdUtilLoadCallback {      
 override fun onAdFailedToLoad(adError: LoadAdError, ad: RewardedAd?) { rewardedAd = ad }      
 override fun onAdLoaded(ad: RewardedAd?) { rewardedAd = ad }      
 override fun onAdDismissedFullScreenContent() { 
 /**
 Comment this if you want the ad to load just once * Uncomment this to load ad again once shown 
 */ 
 loadRewardedAd() 
 }      
 override fun onAdFailedToShowFullScreenContent(adError: AdError?) {}      
 override fun onAdShowedFullScreenContent() { rewardedAd = null }}      
 //Show Ad      
 rewardedAd?.show(this)      
``` 

# Load a Native Ad

## Enable firebase remote config for the app.

```kotlin      
 In your build.gradle(app) add :     
 ``` 

**IMPORTANT: Create a firebase account for this project . #Add the google - services.json to your
project .**

```kotlin   
 plugins {      
 ... id 'com.google.gms.google-services'}      
 dependencies {      
 //Firebase Remote Config implementation platform ('com.google.firebase:firebase-bom:29.0.2') implementation 'com.google.firebase:firebase-config-ktx' implementation 'com.google.firebase:firebase-analytics-ktx'}      
      
      
 In your build.gradle(project) add :      
 buildscript {      
 dependencies { classpath 'com.google.gms:google-services:4.3.10' ... }}      
/**
Call loadNativeAd with following params to load a Native Ad
@param lifecycle -> Lifecycle of activity in which ad will be loaded
@param adUnit -> Pass the adUnit id in this parameter
@param viewGroup -> Pass the parent ViewGroup to add a native ad in that layout
@param nativeAdLoadCallback -> nullable callback to register native ad load events
@param RemoteConfigUtils.getNativeAdTypeId() -> returns a value from remote config and according to the value, layout is loaded. 
*/ 
``` 

## Load Native Ad From Service

 ```kotlin 
 AdSdk.loadNativeAdFromService(
 layoutInflater, 
 applicationContext, 
 "ca-app-pub-3940256099942544/2247696110", 
 binding.llRoot4, 
 nativeAdCallBack, 
 AdSdk.ADType.DEFAULT_NATIVE_SMALL, 
 mediaMaxHeight = 150, 
 loadingTextSize = 24, null, null, null,    
 preloadAds = true)
 
 /** Parameters fun loadNativeAdFromService(      
  layoutInflater: LayoutInflater,      
  context: Context,      
  adUnit: String,      
  viewGroup: ViewGroup,      
  nativeAdLoadCallback: NativeAdLoadCallback?,      
  adType: String = "1",      
  mediaMaxHeight: Int = 300,      
  loadingTextSize: Int,      
  background: Any?,      
  textColor1: Int?,      
  textColor2: Int?,      
  id: Long = viewGroup.id.toLong(),      
  populator: ((nativeAd: NativeAd, adView: NativeAdView) -> Unit)? = null,      
preloadAds: Boolean = false, -> preload ads on ads shown once or only show preloaded ads once autoRefresh: Boolean = false, -> Enable or disable auto refresh ) */ 
``` 
## (Beta) Preload Service Native Ads

During Initialization Add This

 ```kotlin 
 val preloadingNativeAdList = hashMapOf<String, PreloadNativeAds>() preloadingNativeAdList.put(
 "ca-app-pub-3940256099942544/2247696110",
	 PreloadNativeAds(
	 "ca-app-pub-3940256099942544/2247696110",
	 AdSdk.ADType.DEFAULT_NATIVE_SMALL,
	 mediaMaxHeight = 150,
	 loadingTextSize = 24 )
 )
 
 AdSdk.initialize(    
 applicationContext as MyApp,  
 bannerRefreshTimer = 5000L, 
 nativeRefreshTimer = 5000L,      
preloadingNativeAdList = preloadingNativeAdList, /*This*/ ) 
```   
It will be automatically cached and refreshed upon use

Call the function below to load it

```kotlin    
AdSdk.loadNativeAdFromService( 
layoutInflater, applicationContext, 
"ca-app-pub-3940256099942544/2247696110", 
binding.llRoot4, 
nativeAdCallBack, 
AdSdk.ADType.DEFAULT_NATIVE_SMALL, 
mediaMaxHeight = 150, 
loadingTextSize = 24, null, null, null, preloadAds = true) 
```   
Set Preload Ads to true if you want to refresh it upon use or it will be only cached once

```kotlin 
preloadAds(layoutInflater: LayoutInflater, context: Context) 
```   
Manually Preload Ads using this Function

```kotlin 
/** 
Parameters fun loadNativeAdFromService( layoutInflater: LayoutInflater, context: Context, adUnit: String, viewGroup: ViewGroup, nativeAdLoadCallback: NativeAdLoadCallback?, adType: String = "1", mediaMaxHeight: Int = 300, loadingTextSize: Int, background: Any?, textColor1: Int?, textColor2: Int?, id: Long = viewGroup.id.toLong(), populator: ((nativeAd: NativeAd, adView: NativeAdView) -> Unit)? = null, preloadAds: Boolean = false, )    
*/ 
``` 

# In the MainApplication add this line

In the firebase remote config of your application project ad this native_ad_layout_type

if you want to set layout via remote config

```kotlin

Go to the Firebase of your Project and set this variable in Remore Config * native_ad_layout_type - Type String -Value - 1 to 5, no need for quotes * / RemoteConfigUtils . init ()
AdSdk.loadNativeAd(
    lifecycle,
    "ca-app-pub-3940256099942544/2247696110",
    binding.llRoot, nativeAdCallBack, RemoteConfigUtils.getNativeAdTypeId()
)  
```

Call loadNativeAd with following params to load a Native Ad

```kotlin
 /***

@param lifecycle -> Lifecycle of activity in which ad will be loaded
@param adUnit -> Pass the adUnit id in this parameter
@param viewGroup -> Pass the parent ViewGroup to add a native ad in that layout
@param nativeAdLoadCallback -> nullable callback to register native ad load events
@param adType -> nullable layoutId, if you want a custom layout, pass a custom layout otherwise its load default UI
@param populator -> nullable populator, if you want a custom population method, pass a method which takes (NativeAd, NativeAdView) as params
@param background -> nullable It is the Background Color or a Drawable you can put either, if it matches nothing then it'll choose default
@param textColor1 -> nullable It is the primary color in Int that is Color Resource
@param textColor1 -> nullable It is the Secondary color in Int that is Color Resource
@param mediaMaxHeight -> nullable It is the height of the ad Media in Int
@param loadingTextSize: Int -> nullable It is the textSize of the loading text default is 48
 */
```

The adType can be either string size from "1" to "5" * or one of these below

![chrome_zXtIBvGj33](https://i.ibb.co/SDjcBgT/ad-Size-Image.jpg)

```kotlin
 companion object {
    val SMALL = "3"
    val MEDIUM = "4"
    val BIGV1 = "1"
    val BIGV2 = "5"
    val BIGV3 = "2"
    val DEFAULT_AD = "6"
}
/***
e.g. AdType.SMALL
 */

AdSdk.loadNativeAd(
    lifecycle,
    "ca-app-pub-3940256099942544/2247696110",
    binding.llRoot,
    nativeAdCallBack,
    R.layout.ad_item,
    this::populateNativeAdView
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

fun populateNativeAdView(
    nativeAd: NativeAd, adView: NativeAdView
) {
    val ratingBar = adView.findViewById(R.id.stars) as View
    adView.starRatingView = ratingBar
    val adHeadline = adView.findViewById(R.id.headline) as TextView
    adView.headlineView = adHeadline
    val adBody = adView.findViewById(R.id.body) as TextView
    adView.bodyView = adBody
    adView.setNativeAd(nativeAd)
}

```