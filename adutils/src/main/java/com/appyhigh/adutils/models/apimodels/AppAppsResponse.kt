package com.appyhigh.adutils.models.apimodels

import com.google.gson.annotations.SerializedName

data class AppAppsResponse (
    @SerializedName("message")
    val message:String = "",
    @SerializedName("status")
    val status:String = "",
    @SerializedName("apps")
    val apps:List<AppsData> = emptyList(),
)