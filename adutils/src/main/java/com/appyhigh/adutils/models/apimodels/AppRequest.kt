package com.appyhigh.adutils.models.apimodels

import com.google.gson.annotations.SerializedName

class AppRequest {
    @SerializedName("packageId")
    var packageId: String = ""

    @SerializedName("platform")
    var platform: String = "ANDROID"

    constructor(){

    }
    constructor(packageId: String, platform: String) {
        this.packageId = packageId
        this.platform = platform
    }
}