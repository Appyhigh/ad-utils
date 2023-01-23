package com.appyhigh.adutils

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.util.Log
import com.appyhigh.adutils.AdSdk.preloadAds
import com.appyhigh.adutils.api.AdmobInstance
import com.appyhigh.adutils.models.PreloadNativeAds
import com.appyhigh.adutils.models.apimodels.AdMod
import com.appyhigh.adutils.models.apimodels.AppRequest
import com.appyhigh.adutils.models.apimodels.AppsData
import com.appyhigh.adutils.models.apimodels.SingleAppResponse
import com.appyhigh.adutils.utils.AdMobUtil
import com.appyhigh.adutils.utils.AdMobUtil.fetchAdById
import com.appyhigh.adutils.utils.RSAKeyGenerator
import com.appyhigh.adutils.utils.container.AppPref
import com.example.speakinenglish.container.AppPrefs
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.json.JSONObject


class DynamicsAds {
    companion object {

        @SuppressLint("CheckResult")
        fun getDynamicAds(
            applicationContext: Context,
            appPackageName: String,
            dynamicAdsFetchThresholdInSecs: Int,
            preloadingNativeAdList: HashMap<String, PreloadNativeAds>?,
            fetchingCallback: AdSdk.FetchingCallback?
        ) {
            try {
                val lastTime = AppPref.getLong(applicationContext,AppPref.lastFetched)
                val l = (System.currentTimeMillis() - lastTime!!) / 1000
                if (l > dynamicAdsFetchThresholdInSecs) {
//                    if (isNetworkConnected(applicationContext)) {
                        val appRequest = AppRequest(appPackageName,"ANDROID")
                        AdmobInstance.ApiBuilder(applicationContext)
                            .getAppInfo(appRequest)
                            .doOnSuccess { response ->
                                if (response.status == "success"){
                                    val item = response.app
                                    var added = 0
                                    if (item?.packageId.equals(appPackageName)){
                                        added++
                                        val alldata = Gson().toJson(item,object : TypeToken<AppsData>() {}.type)
                                        AppPref.put(applicationContext,AppPref.appdata,alldata)
                                        item?.showAppAds?.let {
                                            AppPref.put(applicationContext,AppPref.showAppAds,
                                                it
                                            )
                                        }
                                        val string = Gson().toJson(item?.adMob,object : TypeToken<List<AdMod?>?>() {}.type)
                                        AppPref.put(applicationContext,AppPref.ads,string)
                                        Log.d("Dynamicads", "getDynamicAds: "+applicationContext.fetchAdById("util_interstitial"))
                                        if (preloadingNativeAdList != null) {
                                            preloadAds(
                                                applicationContext as Application,
                                                preloadingNativeAdList
                                            )
                                        }
                                        fetchingCallback?.OnComplete(item)
                                    }
                                    if (added == 0) {
                                        AppPref.put(applicationContext,AppPref.ads,"")
                                    }
                                }
                            }
                            ?.subscribeOn(Schedulers.io())
                            ?.observeOn(AndroidSchedulers.mainThread())
                            ?.subscribe({
                            }, {
                                fetchingCallback?.OnComplete(null)
                                Log.d("Dynamicads", "getDynamicAds: "+it.localizedMessage)
                            })
//                    }
                }
            } catch (e: Exception) {


            }
        }

        private fun isNetworkConnected(context: Context): Boolean {
            val cm: ConnectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork: NetworkInfo? = cm.activeNetworkInfo
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting
        }

        fun fetchToken(context: Context): String {
            var token = ""
            try {
                try {
                    val map = HashMap<String, String>()
                    map["user_id"] = "test_user"
                    map["aud"] = "dapps"
                    map["api"] = "users"
                    token = RSAKeyGenerator.getJwtToken(
                        map,
                        System.currentTimeMillis(),
                        BuildConfig.PRIVATE_KEY_NOTIF
                    ) ?: ""
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return "Bearer "+token
        }
    }


}