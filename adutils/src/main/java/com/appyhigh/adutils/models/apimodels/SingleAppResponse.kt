package com.appyhigh.adutils.models.apimodels

import com.google.gson.annotations.SerializedName

data class SingleAppResponse (
    @SerializedName("message")
    val message:String = "",
    @SerializedName("status")
    val status:String = "",
    @SerializedName("app")
    val app:AppsData? = null
)