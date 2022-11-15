package com.appyhigh.adutils.utils

import android.util.Log
import com.appyhigh.adutils.AdSdk
import com.appyhigh.adutils.models.apimodels.AdMod
import com.appyhigh.adutils.models.apimodels.AppsData
import com.appyhigh.adutils.models.apimodels.SingleAppResponse
import com.example.speakinenglish.container.AppPrefs
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type


object AdMobUtil {

    fun fetchAllAds():List<AdMod>{
        var ads = AppPrefs.ads.get()
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

    fun fetchLatestVersion():String{
        var ads = AppPrefs.appdata.get()
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

    fun fetchCriticalVersion():String{
        var ads = AppPrefs.appdata.get()
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

    fun fetchColor(key: String):String{
        var ads = AppPrefs.ads.get()
        if (!ads.equals("")){
            try {
                val type: Type = object : TypeToken<List<AdMod?>?>() {}.type
                var adsObj:List<AdMod> = Gson().fromJson(ads,type)
                return adsObj.filter { it -> it.ad_name.equals(key) }.get(0).color_hex
            }
            catch (e:Exception){
                return "#000000"
            }
        }
        return "#000000"
    }

    fun fetchRefreshTime(key: String):Long{
        var ads = AppPrefs.ads.get()
        if (!ads.equals("")){
            try {
                val type: Type = object : TypeToken<List<AdMod?>?>() {}.type
                var adsObj:List<AdMod> = Gson().fromJson(ads,type)
                return adsObj.filter {it.ad_name.equals(key) }.get(0).refresh_rate_ms.toLong()
            }
            catch (e:Exception){
                return 5000.toLong()
            }
        }
        return 5000.toLong()
    }

    fun fetchAdById(key:String):AdMod?{
        var ads = AppPrefs.ads.get()
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

    fun fetchPrimaryById(key:String):List<String>{
        var ads = AppPrefs.ads.get()
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

    fun fetchSecondaryById(key:String):List<String>{
        var ads = AppPrefs.ads.get()
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

    fun fetchAdStatusFromAdId(id:String):Boolean{
        var ads = AppPrefs.ads.get()
        if (!ads.equals("")){
            try {
                val type: Type = object : TypeToken<List<AdMod?>?>() {}.type
                var adsObj:List<AdMod> = Gson().fromJson(ads,type)
                return adsObj.filter { ad -> ad.ad_name.equals(id)}.get(0).isActive
            }
            catch (e:Exception){
                return true
            }
        }
        return true
    }

    fun fetchAdLoadTimeout(id:String):Int{
        var ads = AppPrefs.ads.get()
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

    fun fetchAdSize(name:String):String{
        val ad = fetchAdById(name)
        return when{
            ad?.size.equals("small") -> AdSdk.ADType.SMALL
            ad?.size.equals("medium") -> AdSdk.ADType.MEDIUM
            ad?.size.equals("bigv1") -> AdSdk.ADType.BIGV1
            ad?.size.equals("bigv2") -> AdSdk.ADType.BIGV2
            ad?.size.equals("bigv3") -> AdSdk.ADType.BIGV3
            else -> AdSdk.ADType.DEFAULT_AD
        }

    }
}