package com.appyhigh.adutils.models.apimodels

import com.google.gson.annotations.SerializedName

data class AdMod(
    @SerializedName("primary_ids")
    val primary_ids: List<String> = emptyList(),
    @SerializedName("secondary_ids")
    val secondary_ids: List<String> = emptyList(),
    @SerializedName("_id")
    val _id: String = "",
    @SerializedName("ad_name")
    val ad_name: String = "",
    @SerializedName("ad_type")
    val ad_type: String = "",
    @SerializedName("isActive")
    val isActive: Boolean = false,
    @SerializedName("refresh_rate_ms")
    val refresh_rate_ms: Int = 0,
    @SerializedName("color_hex")
    val color_hex: String = "",
    @SerializedName("size")
    val size: String = "",
    @SerializedName("primary_adload_timeout_ms")
    val primary_adload_timeout_ms: Int = 0
)