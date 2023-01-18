package com.appyhigh.adutils.api

import android.app.Activity
import android.content.Context
import android.provider.Settings
import com.appyhigh.adutils.BuildConfig
import com.appyhigh.adutils.DynamicsAds
import com.appyhigh.adutils.utils.HttpConstants
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

object AdmobInstance {
    val API = "https://admob-automation.apyhi.com/"
    val TEST_API = "https://admob-automation-qa.apyhi.com/"

    fun ApiBuilder(activity: Context): AdmobApi{
        val token = DynamicsAds.fetchToken(activity)
        val headerAuthorizationInterceptor: Interceptor = object : Interceptor {
            override fun intercept(chain: Interceptor.Chain): Response {
                var request = chain.request()
                val builder = request.newBuilder()
                val header =
                    request.headers.newBuilder()
                        .add(HttpConstants.AUTHORIZATION_HEADER, token)
                        .build()
                request = builder.headers(header).build()
                return chain.proceed(request)
            }
        }
        val interceptor = HttpLoggingInterceptor()
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY)
        val clientBuilder: OkHttpClient.Builder = OkHttpClient.Builder()
        clientBuilder.addInterceptor(headerAuthorizationInterceptor)
        clientBuilder.addInterceptor(interceptor)

        val retrofit: Retrofit = Retrofit.Builder()
            .baseUrl(API)
            .client(clientBuilder.build())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return retrofit.create(AdmobApi::class.java)
    }
}