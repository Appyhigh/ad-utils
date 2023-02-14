package com.appyhigh.adutils.utils

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.appyhigh.adutils.AdSdk
import com.appyhigh.adutils.models.apimodels.AdMod
import com.appyhigh.adutils.models.apimodels.AppsData
import com.appyhigh.adutils.models.apimodels.SingleAppResponse
import com.appyhigh.adutils.utils.AdMobUtil.fetchAdLoadTimeout
import com.appyhigh.adutils.utils.AdMobUtil.fetchAllAds
import com.appyhigh.adutils.utils.AdMobUtil.fetchColor
import com.appyhigh.adutils.utils.AdMobUtil.fetchCriticalVersion
import com.appyhigh.adutils.utils.AdMobUtil.fetchLatestVersion
import com.appyhigh.adutils.utils.AdMobUtil.fetchRefreshTime
import com.appyhigh.adutils.utils.AdMobUtil.printData
import com.appyhigh.adutils.utils.container.AppPref
import com.example.speakinenglish.container.AppPrefs
import com.google.android.gms.ads.AdSize
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type


@SuppressLint("StaticFieldLeak")
object AdMobUtil {


    fun Context.printData(){
        try {
            if (this != null){
                var ads = AppPref.getString(this,AppPref.appdata)
                Log.d("printData: ",ads)
            }
        }catch (e:Exception){
            e.printStackTrace()
        }
    }

    fun Context.fetchAllAds():List<AdMod>{
        try {
            if (this != null){
                var ads = AppPref.getString(this,AppPref.ads)
                if (!ads.equals("")){
                    try {
                        val type: Type = object : TypeToken<List<AdMod?>?>() {}.type
                        var adsObj:List<AdMod> = Gson().fromJson(ads,type)
                        return adsObj
                    }
                    catch (e:Exception){
                        return emptyList()
                    }
                }
                return emptyList()
            }
            return emptyList()
        }
        catch (e:Exception){
            return emptyList()
        }
    }

    fun Context.fetchLatestVersion():String{
        try {
            if (this != null){
                var ads = AppPref.getString(this,AppPref.appdata)
                if (!ads.equals("")){
                    try {
                        val type: Type = object : TypeToken<AppsData>() {}.type
                        var adsObj:AppsData = Gson().fromJson(ads,type)
                        return adsObj.latestVersion.toString()
                    }
                    catch (e:Exception){
                        return "0"
                    }
                }
                return "0"
            }
            return "0"
        }
        catch (e:Exception){
            return "0"
        }
    }

    fun Context.fetchCriticalVersion():String{
        try {
            if (this != null){
                var ads = AppPref.getString(this,AppPref.appdata)
                if (!ads.equals("")){
                    try {
                        val type: Type = object : TypeToken<AppsData>() {}.type
                        var adsObj:AppsData = Gson().fromJson(ads,type)
                        return adsObj.criticalVersion.toString()
                    }
                    catch (e:Exception){
                        return "0"
                    }
                }
                return "0"
            }
            return "0"
        }
        catch (e:Exception){
            return "0"
        }
    }

    fun Context.fetchColor(key: String):String{
        try {
            if (this != null){
                var ads = AppPref.getString(this,AppPref.ads)
                if (!ads.equals("")){
                    try {
                        val type: Type = object : TypeToken<List<AdMod?>?>() {}.type
                        var adsObj:List<AdMod> = Gson().fromJson(ads,type)
                        return adsObj.filter { it -> it.ad_name.equals(key) }.get(0).color_hex
                    }
                    catch (e:Exception){
                        return "#00B0B9"
                    }
                }
                return "#00B0B9"
            }
            return "#00B0B9"
        }
        catch (e:Exception){
            return "#00B0B9"
        }
    }

    fun Context.fetchRefreshTime(key: String):Long{
        try {
            if (this != null){
                var ads = AppPref.getString(this,AppPref.ads)
                if (!ads.equals("")){
                    try {
                        val type: Type = object : TypeToken<List<AdMod?>?>() {}.type
                        var adsObj:List<AdMod> = Gson().fromJson(ads,type)
                        return adsObj.filter {it.ad_name.equals(key) }.get(0).refresh_rate_ms.toLong()
                    }
                    catch (e:Exception){
                        return 45000.toLong()
                    }
                }
                return 45000.toLong()
            }
            return 45000.toLong()
        }
        catch (e:Exception){
            return 45000.toLong()
        }
    }

    fun Context.fetchAdById(key:String):AdMod?{
        try {
            if (this != null){
                var ads = AppPref.getString(this,AppPref.ads)
                if (!ads.equals("")){
                    try {
                        val type: Type = object : TypeToken<List<AdMod?>?>() {}.type
                        var adsObj:List<AdMod> = Gson().fromJson(ads,type)
                        return adsObj.filter { ad -> ad.ad_name.equals(key) }.get(0)
                    }
                    catch (e:Exception){
                        return null
                    }
                }
                return null
            }
            return null
        }
        catch (e:Exception){
            return null
        }
    }

    fun Context.fetchPrimaryById(key:String):List<String>{
        try {
            if (this != null){
                var ads = AppPref.getString(this,AppPref.ads)
                if (!ads.equals("")){
                    try {
                        val type: Type = object : TypeToken<List<AdMod?>?>() {}.type
                        var adsObj:List<AdMod> = Gson().fromJson(ads,type)
                        return adsObj.filter { ad -> ad.ad_name.equals(key) }.get(0).primary_ids
                    }
                    catch (e:Exception){
                        return emptyList()
                    }
                }
                return emptyList()
            }
            return emptyList()
        }
        catch (e:Exception){
            return emptyList()
        }
    }

    fun Context.fetchSecondaryById(key:String):List<String>{
        try {
            if (this != null){
                var ads = AppPref.getString(this,AppPref.ads)
                if (!ads.equals("")){
                    try {
                        val type: Type = object : TypeToken<List<AdMod?>?>() {}.type
                        var adsObj:List<AdMod> = Gson().fromJson(ads,type)
                        return adsObj.filter { ad -> ad.ad_name.equals(key) }.get(0).secondary_ids
                    }
                    catch (e:Exception){
                        return emptyList()
                    }
                }
                return emptyList()
            }
            return emptyList()
        }
        catch (e:Exception){
            return emptyList()
        }
    }

    fun Context.fetchAdStatusFromAdId(id:String):Boolean{
        try {
            if (this != null){
                var ads = AppPref.getString(this,AppPref.ads)
                if (!ads.equals("") || ads!=null){
                    try {
                        val type: Type = object : TypeToken<List<AdMod?>?>() {}.type
                        var adsObj:List<AdMod> = Gson().fromJson(ads,type)
                        return adsObj.filter { ad -> ad.ad_name.trim().toLowerCase().equals(id.trim().toLowerCase())}.get(0).isActive
                    }
                    catch (e:IndexOutOfBoundsException){
                        return false
                    }
                    catch (e:Exception){
                        return true
                    }
                }
                return true
            }
            return true
        }
        catch (e:Exception){
            return true
        }
    }

    fun Context.fetchAdLoadTimeout(id:String):Int{
        try {
            if (this != null){
                var ads = AppPref.getString(this,AppPref.ads)
                if (!ads.equals("")){
                    try {
                        val type: Type = object : TypeToken<List<AdMod?>?>() {}.type
                        var adsObj:List<AdMod> = Gson().fromJson(ads,type)
                        return adsObj.filter { ad -> ad.ad_name.equals(id)}.get(0).primary_adload_timeout_ms
                    }
                    catch (e:Exception){
                        return 0
                    }
                }
                return 0
            }
            return 0
        }
        catch (e:Exception){
            return 0
        }
    }

    fun Context.fetchAdSize(name:String,adSize:String):String{
        try {
            if (this != null){
                val ad = fetchAdById(name)
                if (ad != null)
                    return when{
                        ad?.size?.trim().equals("small",ignoreCase = true) -> AdSdk.ADType.SMALL
                        ad?.size?.trim().equals("medium",ignoreCase = true) -> AdSdk.ADType.MEDIUM
                        ad?.size?.trim().equals("bigv1",ignoreCase = true) -> AdSdk.ADType.BIGV1
                        ad?.size?.trim().equals("bigv2",ignoreCase = true) -> AdSdk.ADType.BIGV2
                        ad?.size?.trim().equals("bigv3",ignoreCase = true) -> AdSdk.ADType.BIGV3
                        ad?.size?.trim().equals("grid_ad",ignoreCase = true) -> AdSdk.ADType.GRID_AD
                        ad?.size?.trim().equals("dynamic",ignoreCase = true) -> AdSdk.ADType.DYNAMIC
                        else -> AdSdk.ADType.DEFAULT_AD
                    }
                else
                    return adSize
            }
            return adSize
        }
        catch (e:Exception){
            return adSize
        }
    }

    fun Context.fetchBackgroundTime(name:String,defaultTime:Int):Int{
        try {
            var ads = AppPref.getString(this,AppPref.ads)
            if (!ads.equals("")){
                try {
                    val type: Type = object : TypeToken<List<AdMod?>?>() {}.type
                    var adsObj:List<AdMod> = Gson().fromJson(ads,type)
                    var value = adsObj.filter { ad -> ad.ad_name.equals(name)}.get(0).background_threshold!!
                    if (value == 0){
                        return defaultTime
                    }
                    else
                        return value
                }
                catch (e:Exception){
                    return defaultTime
                }
            }
            return defaultTime
        }
        catch (e:Exception){
            return defaultTime
        }
    }

    fun Context.fetchBannerAdSize(name:String,adSize:AdSize):AdSize{
        try {
            if (this != null){
                val ad = fetchAdById(name)
                if (ad != null)
                    return when{
                        ad?.size?.trim().equals("banner",ignoreCase = true) -> AdSize.BANNER
                        ad?.size?.trim().equals("large_banner",ignoreCase = true) -> AdSize.MEDIUM_RECTANGLE
                        ad?.size?.trim().equals("medium_rectangle",ignoreCase = true) -> AdSize.LARGE_BANNER
                        else -> adSize
                    }
                else
                    return adSize
            }
            return adSize
        }
        catch (e:Exception){
            return adSize
        }

    }
}