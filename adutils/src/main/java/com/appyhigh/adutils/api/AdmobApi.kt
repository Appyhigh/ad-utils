package com.appyhigh.adutils.api

import com.appyhigh.adutils.models.apimodels.AppAppsResponse
import com.appyhigh.adutils.models.apimodels.AppRequest
import com.appyhigh.adutils.models.apimodels.SingleAppResponse
import io.reactivex.Single
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AdmobApi {

    @GET("api/v2/app")
    fun getAllAppsData(): Single<AppAppsResponse>

    @POST("api/v2/app/info")
    fun getAppInfo(@Body body: AppRequest): Single<SingleAppResponse>
}