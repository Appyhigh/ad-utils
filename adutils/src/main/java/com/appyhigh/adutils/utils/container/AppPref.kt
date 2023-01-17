package com.appyhigh.adutils.utils.container

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager


object AppPref {
    private var sharedPreferences: SharedPreferences? = null

    const val appdata = "appdata"
    const val ads = "ads"
    const val lastFetched = "lastFetched"
    const val showAppAds = "showAppAds"

    fun getInstance(context: Context): SharedPreferences? {
        if (sharedPreferences == null) sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        return sharedPreferences
    }

    fun put(context: Context, key:String, value: Any){
        val editor = getInstance(context)?.edit()
        when (value) {
            is Int -> editor?.putInt(key,value)
            is Boolean -> editor?.putBoolean(key,value)
            is String -> editor?.putString(key,value)
            is Long -> editor?.putLong(key,value)
            is Float -> editor?.putFloat(key,value)
            else -> editor?.putString(key, value.toString())
        }
        editor?.apply()
        editor?.commit()
    }


    fun getBoolean(context: Context, key:String): Boolean {
        return getInstance(context)?.getBoolean(key,true)!!
    }

    fun getFloat(context: Context, key:String): Float {
        return getInstance(context)?.getFloat(key, 0.0F)!!
    }
    fun getLong(context: Context, key:String): Long {
        return getInstance(context)?.getLong(key,0L)!!
    }
    fun getString(context: Context, key:String): String {
        return getInstance(context)?.getString(key,"")!!
    }
    fun getInt(context: Context, key:String): Int {
        return getInstance(context)?.getInt(key,0)!!
    }
}