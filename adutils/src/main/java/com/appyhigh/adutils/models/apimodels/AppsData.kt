package com.appyhigh.adutils.models.apimodels

import com.google.gson.annotations.SerializedName

data class AppsData (
    @SerializedName("latestVersion")
    val latestVersion:Double = 0.0,
    @SerializedName("criticalVersion")
    val criticalVersion:Double = 0.0,
    @SerializedName("platform")
    val platform:String = "",
    @SerializedName("packageId")
    val packageId:String = "",
    @SerializedName("appName")
    val appName:String = "",
    @SerializedName("_id")
    val _id:String = "",
    @SerializedName("isActive")
    val isActive:Boolean = false,
    @SerializedName("showAppAds")
    val showAppAds :Boolean? = null,
    @SerializedName("adMob")
    val adMob:List<AdMod> = emptyList()

) {
    override fun toString(): String {
        return "AppsData(latestVersion=$latestVersion, criticalVersion=$criticalVersion, platform='$platform', packageId='$packageId', appName='$appName', _id='$_id', isActive=$isActive, adMob=$adMob)"
    }
}