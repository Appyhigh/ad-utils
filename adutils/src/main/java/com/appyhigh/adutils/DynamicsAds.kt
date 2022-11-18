package com.appyhigh.adutils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.util.Log
import com.appyhigh.adutils.api.AdmobInstance
import com.appyhigh.adutils.models.apimodels.AdMod
import com.appyhigh.adutils.models.apimodels.AppRequest
import com.appyhigh.adutils.models.apimodels.AppsData
import com.appyhigh.adutils.models.apimodels.SingleAppResponse
import com.appyhigh.adutils.utils.AdMobUtil
import com.appyhigh.adutils.utils.RSAKeyGenerator
import com.example.speakinenglish.container.AppPrefs
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.json.JSONObject


class DynamicsAds {
    companion object {
        val updateJSON = JSONObject()
        var adMobNew: List<AdMod> = ArrayList()

        fun getDynamicAdsId(fallBackAdId: String, adName: String): String {
            return try {
                if (!AdMobUtil.fetchPrimaryById(adName).isEmpty()) {
                    val string = AdMobUtil.fetchPrimaryById(adName)?.get(0)
                    string
                }
                else{
                    fallBackAdId
                }
            } catch (e: Exception) {
                fallBackAdId
            }
        }

        //TODO : we can pass the for logging the ad names
        fun listAllAds(applicationContext: Context, logTag: String) {
            //TODO : List all the ads along with key names for checking
            val string = AppPrefs.ads.get()
            if (string != null) {
                adMobNew = AdMobUtil.fetchAllAds()!!
            }
            if (adMobNew.size > 0) {
                adMobNew.forEach {
                    Log.d(logTag, "listAllAds: " + it + " -> " + it.primary_ids)
                }
            } else {
                Log.d(logTag, "No Dynamic Ads Found")
            }
        }

        fun getDynamicAds(
            applicationContext: Context,
            appPackageName: String,
            dynamicAdsFetchThresholdInSecs: Int,
            fetchingCallback: AdSdk.FetchingCallback?
        ) {
            try {
                val lastTime = AppPrefs.lastFetched.get()
                val l = (System.currentTimeMillis() - lastTime) / 1000
                if (l > dynamicAdsFetchThresholdInSecs) {
//                    if (isNetworkConnected(applicationContext)) {
                        val appRequest = AppRequest(appPackageName,"ANDROID")
                        AdmobInstance.ApiBuilder(applicationContext)
                            .getAppInfo(appRequest)
                            ?.doOnSuccess { response ->
                                if (response.status.equals("success")){
                                    val item = response.app
                                    var added = 0
                                    if (item?.packageId.equals(appPackageName)){
                                        added++
                                        val alldata = Gson().toJson(item,object : TypeToken<AppsData>() {}.type)
                                        AppPrefs.appdata.set(alldata)
                                        AppPrefs.showAppAds.set(item?.showAppAds)
                                        val string = Gson().toJson(item?.adMob,object : TypeToken<List<AdMod?>?>() {}.type)
                                        AppPrefs.ads.set(string)
                                        AppPrefs.commit(applicationContext)
                                        Log.d("Dynamicads", "getDynamicAds: "+AdMobUtil.fetchAdById("util_interstitial"))
                                        fetchingCallback?.OnSuccess()
                                    }
                                    if (added == 0) {
                                        AppPrefs.ads.set("")
                                        AppPrefs.commit(applicationContext)
                                    }
                                }
                            }
                            ?.subscribeOn(Schedulers.io())
                            ?.observeOn(AndroidSchedulers.mainThread())
                            ?.subscribe({
                            }, {
                                fetchingCallback?.OnFailure()
                                Log.d("DunamicAds", "getDynamicAds: "+it.localizedMessage)
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