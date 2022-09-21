package com.appyhigh.adutils

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.util.Log
import com.appyhigh.adutils.utils.RSAKeyGenerator
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit


class DynamicsAds {
    companion object {
        val updateJSON = JSONObject()
        lateinit var ADMODELPREF: SharedPreferences
        var adMobNew: JSONObject = JSONObject()

        fun getDynamicAdsId(fallBackAdId: String, adName: String): String {
            return try {
                val string = adMobNew.getString(adName)
                string
            } catch (e: Exception) {
                fallBackAdId
            }
        }


        //TODO : we can pass the for logging the ad names
        fun listAllAds(applicationContext: Context, logTag: String) {
            //TODO : List all the ads along with key names for checking
            ADMODELPREF = applicationContext.getSharedPreferences("ADMODEL", 0)
            val string = ADMODELPREF.getString("ads", null)
            if (string != null) {
                adMobNew = JSONObject(string)
            }
            if (adMobNew.length() > 0) {
                adMobNew.keys().iterator().forEach {
                    Log.d(logTag, "listAllAds: " + it + " -> " + adMobNew.get(it))
                }
            } else {
                Log.d(logTag, "No Dynamic Ads Found")
            }
        }

        fun getDynamicAds(
            applicationContext: Context,
            appPackageName: String,
            dynamicAdsFetchThresholdInSecs: Int
        ) {
            try {
                val lastTime = ADMODELPREF.getLong("lastFetched", 0)
                val l = (System.currentTimeMillis() - lastTime) / 1000
                if (l > dynamicAdsFetchThresholdInSecs) {
                    if (isNetworkConnected(applicationContext)) {
                        val fetchToken = fetchToken(applicationContext)
                        val client = OkHttpClient().newBuilder()
                            .readTimeout(2, TimeUnit.MINUTES)
                            .connectTimeout(2, TimeUnit.MINUTES)
                            .writeTimeout(2, TimeUnit.MINUTES)
                            .build()
                        val mediaType: MediaType? = ("text/plain").toMediaTypeOrNull()
                        if (mediaType != null) {
                            val mediaType = "text/plain".toMediaTypeOrNull()
                            val request: Request = Request.Builder()
                                .url("https://admob-automation.apyhi.com/api/app")
                                .method("GET", null)
                                .addHeader(
                                    "Authorization",
                                    "Bearer $fetchToken"
                                ).addHeader("Content-Type", "application/json")
                                .build()
                            Thread {
                                try {
                                    val response: Response = client.newCall(request).execute()
                                    if (response.isSuccessful) {
                                        try {
                                            val string = response.body?.string()
                                            if (string != null) {
                                                val jsonObject = JSONObject(string.toString())
                                                val apps = JSONArray(jsonObject.getString("apps"))
                                                var added = 0
                                                for (i in 0 until apps.length()) {
                                                    val appList = JSONObject(apps[i].toString())
                                                    //TODO : Get the List of Apps and only select teh app for the selected packageid
                                                    if (appList.getString("packageId")
                                                            .equals(appPackageName)
                                                    ) {
                                                        added++
                                                        val adMob =
                                                            appList.get("adMob").toString()
                                                        updateJSON.put(
                                                            "package_name",
                                                            applicationContext.packageName
                                                        )
                                                        updateJSON.put(
                                                            "critical_version",
                                                            appList.getString("criticalVersion")
                                                        )
                                                        updateJSON.put(
                                                            "current_version",
                                                            appList.getString("latestVersion")
                                                        )
                                                        adMobNew = JSONObject(adMob)
                                                        //TODO : Store it in Shared Pref for future uses
                                                        ADMODELPREF.edit()
                                                            .putString("ads", adMobNew.toString())
                                                            .apply()
                                                        ADMODELPREF.edit().putLong(
                                                            "lastFetched",
                                                            System.currentTimeMillis()
                                                        ).apply()
                                                        break
                                                    }
                                                }
                                                if (added == 0) {
                                                    //TODO : Clear Shared Pref if no ads are found
                                                    ADMODELPREF.edit().remove("ads").apply()
                                                }
                                            }
                                        } catch (e: Exception) {

                                        }
                                    }
                                } catch (e: Exception) {

                                }
                            }.start()
                        }
                    }
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
            return token
        }
    }


}